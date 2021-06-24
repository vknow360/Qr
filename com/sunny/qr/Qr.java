package com.sunny.qr;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;
import java.util.Map;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.*;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;

@DesignerComponent(version = 5,
        versionName = "5.1",
        description = "An extension to create (with logo) and read barcode offline<br>Developed by <a href=https://sunnythedeveloper.epizy.com>Sunny Gupta</a>",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "https://res.cloudinary.com/andromedaviewflyvipul/image/upload/c_scale,h_20,w_20/v1571472765/ktvu4bapylsvnykoyhdm.png",
        helpUrl = "http://sunnythedeveloper.epizy.com/2020/03/21/qr-read-and-create-bar-code-offline")
@SimpleObject(external = true)
@UsesPermissions(permissionNames ="android.permission.WRITE_EXTERNAL_STORAGE,android.permission.READ_EXTERNAL_STORAGE")
@UsesLibraries(libraries = "zxing-3.4.jar")
public class Qr extends AndroidNonvisibleComponent {
    public Activity activity;
    public Context context;
    public boolean useAdditionalDecoders = false;
    public SymbolShapeHint shapeHint;
    public Qr(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
        activity = container.$context();
    }
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,defaultValue = "False")
    @SimpleProperty(description = "Specifies whether decoders should use additional hints")
    public void UseAdditionalDecoders(boolean use){
        useAdditionalDecoders = use;
    }

    @SimpleProperty(description = "Specifies bar code shape.<br> Note: Normally you would not need to use it.")
    public void BarCodeShape(String shape){
        switch (shape){
            case "NONE":
                shapeHint = SymbolShapeHint.FORCE_NONE;
            case "RECTANGLE":
                shapeHint = SymbolShapeHint.FORCE_RECTANGLE;
            case "SQUARE":
                shapeHint = SymbolShapeHint.FORCE_SQUARE;
        }
    }
    public void Reader(File myFile){
        try {
            MultiFormatReader reader = new MultiFormatReader();
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
            Bitmap bMap = BitmapFactory.decodeStream(bis);
            int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];
            bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());
            LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(), intArray);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = null;
            if (useAdditionalDecoders){
                Map<DecodeHintType, Object> hintsMap = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
                hintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
                hintsMap.put(DecodeHintType.POSSIBLE_FORMATS,
                        EnumSet.allOf(BarcodeFormat.class));
                hintsMap.put(DecodeHintType.PURE_BARCODE, Boolean.FALSE);
                result = reader.decode(bitmap, hintsMap);
            }else {
                result = reader.decode(bitmap);
            }
            barDecoded(result.getText(),result.getBarcodeFormat().toString());
        }catch (Exception e){
            barDecoded(e.getMessage() != null?e.getMessage():e.toString(),"Unknown");
        }
    }
    @SimpleFunction(description = "Returns a list of available barcode formats")
    public static List<String> BarcodeFormats(){
        BarcodeFormat[] barcodeFormat = BarcodeFormat.values();
        List<String> formats = new ArrayList<>();
        for (BarcodeFormat b:barcodeFormat){
            formats.add(b.toString());
        }
        return formats;
    }
    @SimpleFunction(description = "Decodes barcode from file")
    public void DecodeBarCode(final String filePath){
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                File myFile = new File(filePath);
                Reader(myFile.getAbsoluteFile());
            }
        });
    }
    public void barDecoded(final String result,final String barFormat){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BarCodeDecoded(result,barFormat);
            }
        });
    }
    @SimpleEvent(description="Event raised after 'DecodeBarCode' method with result and barcode's format")
    public void BarCodeDecoded(String result,String barFormat){
        EventDispatcher.dispatchEvent(this, "BarCodeDecoded",result,barFormat);
    }
    @SimpleEvent(description="Event raised after 'GenerateBarCode' method with response and filepath")
    public void BarCodeGenerated(String response,String filePath){
        EventDispatcher.dispatchEvent(this, "BarCodeGenerated",response,filePath);
    }
    @SimpleFunction(description="Generates bar code to given path in desired file format.]")
    public void GenerateBarCode(final String text,final String filePath,final String logoPath,final int height,final int width,final String fileFormat,final String barFormat,final String charset,final int margin,final int bgColor,final int barColor){
        AsynchUtil.runAsynchronously(new Runnable() {
            @Override
            public void run() {
                File myFile = new File(filePath);
                try {
                    FileOutputStream fos = new FileOutputStream(myFile);
                    Bitmap bitmap = Writer(text,BarcodeFormat.valueOf(barFormat),height,width,margin,charset,barColor,bgColor,logoPath);
                    boolean success = bitmap.compress(Bitmap.CompressFormat.valueOf(fileFormat.toUpperCase()),100,fos);
                    fos.flush();
                    fos.close();
                    if (success){
                        barGenerated("Barcode generated successfully",myFile.getPath());
                    }else {
                        barGenerated("Unable to generate barcode", "");
                    }
                } catch (Exception e) {
                    barGenerated(getMessage(e),"");
                }
            }
        });
    }
    public String getMessage(Exception e){
        return e.getMessage()!=null?e.getMessage():e.toString();
    }
    public void barGenerated(final String s,final String s1){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BarCodeGenerated(s,s1);
            }
        });
    }
    public Bitmap Writer(String codeData, BarcodeFormat barcodeFormat,int codeHeight, int codeWidth,int margin,String charset,int fColor,int bgColor,String logoPath) {
        try {
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            hints.put(EncodeHintType.CHARACTER_SET, charset);
            hints.put(EncodeHintType.MARGIN, margin);
            if (shapeHint != SymbolShapeHint.FORCE_NONE){
                hints.put(EncodeHintType.DATA_MATRIX_SHAPE,shapeHint);
            }
            MultiFormatWriter codeWriter = new MultiFormatWriter();
            BitMatrix byteMatrix = codeWriter.encode (
                    codeData,
                    barcodeFormat,
                    codeWidth,
                    codeHeight,
                    hints
            );
            int width   = byteMatrix.getWidth();
            int height  = byteMatrix.getHeight();
            int pixelSize = codeWidth/width;
            if (pixelSize > codeHeight/height){
                pixelSize = codeHeight/height;
            }
            int[] pixels = new int[codeWidth * codeHeight];
            for (int y = 0; y < height; y++) {
                int offset = y * codeWidth*pixelSize;
                for (int pixelsizeHeight = 0; pixelsizeHeight < pixelSize; pixelsizeHeight++, offset+=codeWidth) {
                    for (int x = 0; x < width; x++) {
                        for (int pixelsizeWidth = 0; pixelsizeWidth < pixelSize; pixelsizeWidth++) {
                            pixels[offset + x * pixelSize + pixelsizeWidth] = byteMatrix.get(x, y) ? fColor : bgColor;
                        }
                    }
                }
            }
            Bitmap imageBitmap = Bitmap.createBitmap(codeWidth, codeHeight, Bitmap.Config.ARGB_8888);
            imageBitmap.setPixels(pixels,0,codeWidth,0,0,codeWidth,codeHeight);
            if (!logoPath.isEmpty()){
                return mergeBitmaps(logoPath.startsWith("//")?BitmapFactory.decodeStream(form.openAsset(logoPath.substring(2))):BitmapFactory.decodeFile(logoPath),imageBitmap);
            }else {
                return imageBitmap;
            }
        } catch (Exception e) {
            return Bitmap.createBitmap(codeWidth, codeHeight, Bitmap.Config.ARGB_8888);
        }
    }
  /*
    credits to https://github.com/AyoubDev-DevYB/DevYb-QR-Logo
  */
    private Bitmap mergeBitmaps(Bitmap logo, Bitmap qrcode) {
        Bitmap combined = Bitmap.createBitmap(qrcode.getWidth(), qrcode.getHeight(), qrcode.getConfig());
        android.graphics.Canvas canvas = new android.graphics.Canvas(combined);
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        canvas.drawBitmap(qrcode, new Matrix(), null);
        Bitmap resizeLogo = Bitmap.createScaledBitmap(logo, canvasWidth / 5, canvasHeight / 5, true);
        int centreX = (canvasWidth - resizeLogo.getWidth()) / 2;
        int centreY = (canvasHeight - resizeLogo.getHeight()) / 2;
        canvas.drawBitmap(resizeLogo, centreX, centreY, null);
        return combined;
    }
}

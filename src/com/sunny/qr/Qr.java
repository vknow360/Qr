package com.sunny.qr;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.FileUtil;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import com.google.zxing.multi.GenericMultipleBarcodeReader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

public class Qr extends AndroidNonvisibleComponent {
    public Activity activity;
    public Context context;
    public boolean useAdditionalDecoders = false;
    public SymbolShapeHint shapeHint = SymbolShapeHint.FORCE_NONE;
    private static final Map<DecodeHintType, Object> HINTS = new EnumMap<>(DecodeHintType.class);
    private static final Map<DecodeHintType, Object> HINTS_PURE;

    static {
        HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.allOf(BarcodeFormat.class));
        HINTS_PURE = new EnumMap<>(HINTS);
        HINTS_PURE.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
    }
    public Qr(ComponentContainer container) {
        super(container.$form());
        context = container.$context();
        activity = container.$context();

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
    private void Reader(File var1) {
        try {
            Bitmap var2 = BitmapFactory.decodeStream(FileUtil.openFile(form,var1));
            int[] var3 = new int[var2.getWidth() * var2.getHeight()];
            var2.getPixels(var3, 0, var2.getWidth(), 0, 0, var2.getWidth(), var2.getHeight());
            RGBLuminanceSource var4 = new RGBLuminanceSource(var2.getWidth(), var2.getHeight(), var3);
            BinaryBitmap var5 = new BinaryBitmap(new HybridBinarizer(var4));
            ArrayList<Result> var6 = new ArrayList<>(1);
            MultiFormatReader var7 = new MultiFormatReader();
            ReaderException var8 = null;

            try {
                GenericMultipleBarcodeReader var9 = new GenericMultipleBarcodeReader(var7);
                Result[] var10 = var9.decodeMultiple(var5, HINTS);
                if (var10 != null) {
                    Collections.addAll(var6, var10);
                }
            } catch (ReaderException var16) {
                var16.printStackTrace();
                var8 = var16;
            }

            if (var6.isEmpty()){
                try {
                    GenericMultipleBarcodeReader var9 = new GenericMultipleBarcodeReader(var7);
                    Result[] var10 = var9.decodeMultiple(var5, HINTS_PURE);
                    if (var10 != null) {
                        Collections.addAll(var6, var10);
                    }
                } catch (ReaderException var16) {
                    var16.printStackTrace();
                    var8 = var16;
                }
            }

            Result var18;
            if (var6.isEmpty()) {
                try {
                    var18 = var7.decode(var5, HINTS_PURE);
                    if (var18 != null) {
                        var6.add(var18);
                    }
                } catch (ReaderException var15) {
                    var15.printStackTrace();
                    var8 = var15;
                }
            }

            if (var6.isEmpty()) {
                try {
                    var18 = var7.decode(var5, HINTS);
                    if (var18 != null) {
                        var6.add(var18);
                    }
                } catch (ReaderException var14) {
                    var14.printStackTrace();
                    var8 = var14;
                }
            }

            if (var6.isEmpty()) {
                try {
                    BinaryBitmap var19 = new BinaryBitmap(new HybridBinarizer(var4));
                    Result var20 = var7.decode(var19, HINTS);
                    if (var20 != null) {
                        var6.add(var20);
                    }
                } catch (ReaderException var13) {
                    var13.printStackTrace();
                    var8 = var13;
                }
            }

            if (var6.isEmpty()) {
                this.barDecoded(var8 == null ? NotFoundException.getNotFoundInstance().getMessage() : var8.getMessage(), "Unknown");
            } else {
                ArrayList<String> var21 = new ArrayList<>();
                ArrayList<String> var22 = new ArrayList<>();
                if (var6.size() > 1) {
                    for (Result o : var6) {
                        var21.add(o.getText());
                        var22.add(o.getBarcodeFormat().toString());
                    }
                    this.barDecoded(YailList.makeList(var21), YailList.makeList(var22));
                } else {
                    this.barDecoded(var6.get(0).getText(), var6.get(0).getBarcodeFormat().toString());
                }
            }
        } catch (Exception var17) {
            var17.printStackTrace();
            this.barDecoded(var17.getMessage() != null ? var17.getMessage() : var17.toString(), "Unknown");
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
    public void barDecoded(final Object result,final Object barFormat){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BarCodeDecoded(result == null ? "" : result,barFormat);
            }
        });
    }
    @SimpleEvent(description="Event raised after 'DecodeBarCode' method with result and barcode's format")
    public void BarCodeDecoded(Object result,Object barFormat){
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
    @SuppressLint("NewApi")
    public Bitmap Writer(String codeData, BarcodeFormat barcodeFormat, int codeHeight, int codeWidth, int margin, String charset, int fColor, int bgColor, String logoPath) {
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

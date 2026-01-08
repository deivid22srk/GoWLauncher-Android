package com.winlator.cmod.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IconExtractor {

    public static Bitmap extractIcon(File exeFile) {
        try {
            RandomAccessFile raf = new RandomAccessFile(exeFile, "r");
            
            byte[] dosHeader = new byte[64];
            raf.read(dosHeader);
            
            if (dosHeader[0] != 'M' || dosHeader[1] != 'Z') {
                raf.close();
                return null;
            }
            
            int peOffset = ByteBuffer.wrap(dosHeader, 60, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            raf.seek(peOffset);
            byte[] peSignature = new byte[4];
            raf.read(peSignature);
            
            if (peSignature[0] != 'P' || peSignature[1] != 'E') {
                raf.close();
                return null;
            }
            
            raf.seek(peOffset + 4 + 16);
            byte[] optionalHeaderSizeBytes = new byte[2];
            raf.read(optionalHeaderSizeBytes);
            int optionalHeaderSize = ByteBuffer.wrap(optionalHeaderSizeBytes).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            
            long resourceTableOffset = peOffset + 4 + 20 + 112;
            raf.seek(resourceTableOffset);
            
            byte[] resourceEntry = new byte[8];
            raf.read(resourceEntry);
            
            int resourceRVA = ByteBuffer.wrap(resourceEntry, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            if (resourceRVA == 0) {
                raf.close();
                return null;
            }
            
            raf.close();
            return null;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap createDefaultIcon(String gameName) {
        int size = 256;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setAntiAlias(true);
        
        int[] colors = {0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF009688, 0xFF4CAF50, 0xFF8BC34A};
        int colorIndex = Math.abs(gameName.hashCode()) % colors.length;
        paint.setColor(colors[colorIndex]);
        
        canvas.drawRoundRect(0, 0, size, size, 32, 32, paint);
        
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(size / 3f);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        
        String initial = gameName.isEmpty() ? "?" : gameName.substring(0, 1).toUpperCase();
        
        float textY = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(initial, size / 2f, textY, paint);
        
        return bitmap;
    }
}

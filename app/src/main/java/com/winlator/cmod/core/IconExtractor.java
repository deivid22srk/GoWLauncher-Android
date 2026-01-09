package com.winlator.cmod.core;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class IconExtractor {
    private static final String TAG = "IconExtractor";
    
    private static final int RT_ICON = 3;
    private static final int RT_GROUP_ICON = 14;
    
    private static class IconDirEntry {
        int width;
        int height;
        int colorCount;
        int planes;
        int bitCount;
        int bytesInRes;
        int imageOffset;
        int iconId;
    }
    
    private static class ResourceEntry {
        int id;
        long offset;
        int size;
    }
    
    private static class PEInfo {
        long sectionTableOffset;
        int numberOfSections;
        long resourceDirBase;
        int resourceRVA;
    }

    public static Bitmap extractIcon(File exeFile) {
        try (RandomAccessFile raf = new RandomAccessFile(exeFile, "r")) {
            Log.d(TAG, "Starting icon extraction from: " + exeFile.getName());
            
            byte[] dosHeader = new byte[64];
            raf.read(dosHeader);
            
            if (dosHeader[0] != 'M' || dosHeader[1] != 'Z') {
                Log.e(TAG, "Not a valid PE file (missing MZ signature)");
                return null;
            }
            
            int peOffset = ByteBuffer.wrap(dosHeader, 60, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            raf.seek(peOffset);
            byte[] peSignature = new byte[4];
            raf.read(peSignature);
            
            if (peSignature[0] != 'P' || peSignature[1] != 'E') {
                Log.e(TAG, "Not a valid PE file (missing PE signature)");
                return null;
            }
            
            raf.seek(peOffset + 6);
            byte[] numberOfSectionsBytes = new byte[2];
            raf.read(numberOfSectionsBytes);
            int numberOfSections = ByteBuffer.wrap(numberOfSectionsBytes).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            
            raf.seek(peOffset + 20);
            byte[] optionalHeaderSizeBytes = new byte[2];
            raf.read(optionalHeaderSizeBytes);
            int optionalHeaderSize = ByteBuffer.wrap(optionalHeaderSizeBytes).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            
            raf.seek(peOffset + 24);
            byte[] magicBytes = new byte[2];
            raf.read(magicBytes);
            int magic = ByteBuffer.wrap(magicBytes).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            
            boolean isPE32Plus = (magic == 0x20b);
            long resourceTableOffset = isPE32Plus ? (peOffset + 24 + 112) : (peOffset + 24 + 96);
            
            raf.seek(resourceTableOffset);
            byte[] resourceEntry = new byte[8];
            raf.read(resourceEntry);
            
            int resourceRVA = ByteBuffer.wrap(resourceEntry, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int resourceSize = ByteBuffer.wrap(resourceEntry, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            if (resourceRVA == 0 || resourceSize == 0) {
                Log.e(TAG, "No resource table found");
                return null;
            }
            
            Log.d(TAG, "Resource RVA: 0x" + Integer.toHexString(resourceRVA) + ", Size: " + resourceSize);
            
            long sectionTableOffset = peOffset + 24 + optionalHeaderSize;
            long resourceFileOffset = rvaToFileOffset(raf, resourceRVA, sectionTableOffset, numberOfSections);
            
            if (resourceFileOffset == -1) {
                Log.e(TAG, "Could not convert RVA to file offset");
                return null;
            }
            
            Log.d(TAG, "Resource file offset: 0x" + Long.toHexString(resourceFileOffset));
            
            PEInfo peInfo = new PEInfo();
            peInfo.sectionTableOffset = sectionTableOffset;
            peInfo.numberOfSections = numberOfSections;
            peInfo.resourceDirBase = resourceFileOffset;
            peInfo.resourceRVA = resourceRVA;
            
            List<ResourceEntry> iconGroupResources = findResourcesByType(raf, peInfo, RT_GROUP_ICON);
            
            if (iconGroupResources.isEmpty()) {
                Log.e(TAG, "No icon group resources found");
                return null;
            }
            
            Log.d(TAG, "Found " + iconGroupResources.size() + " icon groups");
            
            ResourceEntry firstIconGroup = iconGroupResources.get(0);
            raf.seek(firstIconGroup.offset);
            
            byte[] iconGroupData = new byte[firstIconGroup.size];
            raf.read(iconGroupData);
            
            Log.d(TAG, "Icon group size: " + firstIconGroup.size + " bytes");
            
            List<IconDirEntry> iconEntries = parseIconGroup(iconGroupData);
            
            if (iconEntries.isEmpty()) {
                Log.e(TAG, "No icons in icon group");
                return null;
            }
            
            Log.d(TAG, "Found " + iconEntries.size() + " icons in group");
            
            IconDirEntry bestIcon = selectBestIcon(iconEntries);
            Log.d(TAG, "Selected icon: " + bestIcon.width + "x" + bestIcon.height + " " + bestIcon.bitCount + "bpp, ID: " + bestIcon.iconId);
            
            List<ResourceEntry> iconResources = findResourcesByType(raf, peInfo, RT_ICON);
            
            Log.d(TAG, "Found " + iconResources.size() + " icon resources");
            
            ResourceEntry iconResource = null;
            for (ResourceEntry res : iconResources) {
                if (res.id == bestIcon.iconId) {
                    iconResource = res;
                    break;
                }
            }
            
            if (iconResource == null) {
                Log.e(TAG, "Could not find icon resource with id " + bestIcon.iconId);
                return null;
            }
            
            Log.d(TAG, "Icon data offset: 0x" + Long.toHexString(iconResource.offset) + ", size: " + iconResource.size);
            
            raf.seek(iconResource.offset);
            byte[] iconData = new byte[iconResource.size];
            raf.read(iconData);
            
            byte[] icoFile = createICOFile(bestIcon, iconData);
            
            Bitmap result = decodeICO(icoFile);
            if (result != null) {
                Log.d(TAG, "Successfully extracted icon: " + result.getWidth() + "x" + result.getHeight());
            }
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting icon: " + e.getMessage(), e);
            return null;
        }
    }
    
    private static long rvaToFileOffset(RandomAccessFile raf, int rva, long sectionTableOffset, int numberOfSections) throws Exception {
        for (int i = 0; i < numberOfSections; i++) {
            long sectionOffset = sectionTableOffset + (i * 40);
            raf.seek(sectionOffset + 12);
            
            byte[] virtualAddressBytes = new byte[4];
            raf.read(virtualAddressBytes);
            int virtualAddress = ByteBuffer.wrap(virtualAddressBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            byte[] virtualSizeBytes = new byte[4];
            raf.read(virtualSizeBytes);
            int virtualSize = ByteBuffer.wrap(virtualSizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            byte[] pointerToRawDataBytes = new byte[4];
            raf.read(pointerToRawDataBytes);
            int pointerToRawData = ByteBuffer.wrap(pointerToRawDataBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            if (rva >= virtualAddress && rva < virtualAddress + virtualSize) {
                return pointerToRawData + (rva - virtualAddress);
            }
        }
        return -1;
    }
    
    private static List<ResourceEntry> findResourcesByType(RandomAccessFile raf, PEInfo peInfo, int resourceType) throws Exception {
        List<ResourceEntry> results = new ArrayList<>();
        
        raf.seek(peInfo.resourceDirBase);
        byte[] dirHeader = new byte[16];
        raf.read(dirHeader);
        
        int numberOfNamedEntries = ByteBuffer.wrap(dirHeader, 12, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        int numberOfIdEntries = ByteBuffer.wrap(dirHeader, 14, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        
        for (int i = 0; i < numberOfNamedEntries + numberOfIdEntries; i++) {
            byte[] entry = new byte[8];
            raf.read(entry);
            
            int typeId = ByteBuffer.wrap(entry, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int offsetToData = ByteBuffer.wrap(entry, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            if ((typeId & 0x80000000) == 0 && typeId == resourceType) {
                boolean isDirectory = (offsetToData & 0x80000000) != 0;
                int actualOffset = offsetToData & 0x7FFFFFFF;
                
                if (isDirectory) {
                    long level2Offset = peInfo.resourceDirBase + actualOffset;
                    processLevel2(raf, peInfo, level2Offset, results);
                }
            }
        }
        
        return results;
    }
    
    private static void processLevel2(RandomAccessFile raf, PEInfo peInfo, long dirOffset, List<ResourceEntry> results) throws Exception {
        raf.seek(dirOffset);
        byte[] dirHeader = new byte[16];
        raf.read(dirHeader);
        
        int numberOfNamedEntries = ByteBuffer.wrap(dirHeader, 12, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        int numberOfIdEntries = ByteBuffer.wrap(dirHeader, 14, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        
        for (int i = 0; i < numberOfNamedEntries + numberOfIdEntries; i++) {
            byte[] entry = new byte[8];
            raf.read(entry);
            
            int resourceId = ByteBuffer.wrap(entry, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int offsetToData = ByteBuffer.wrap(entry, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            boolean isDirectory = (offsetToData & 0x80000000) != 0;
            int actualOffset = offsetToData & 0x7FFFFFFF;
            
            if (isDirectory) {
                long level3Offset = peInfo.resourceDirBase + actualOffset;
                processLevel3(raf, peInfo, level3Offset, resourceId, results);
            }
        }
    }
    
    private static void processLevel3(RandomAccessFile raf, PEInfo peInfo, long dirOffset, int resourceId, List<ResourceEntry> results) throws Exception {
        raf.seek(dirOffset);
        byte[] dirHeader = new byte[16];
        raf.read(dirHeader);
        
        int numberOfNamedEntries = ByteBuffer.wrap(dirHeader, 12, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        int numberOfIdEntries = ByteBuffer.wrap(dirHeader, 14, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        
        for (int i = 0; i < numberOfNamedEntries + numberOfIdEntries; i++) {
            byte[] entry = new byte[8];
            raf.read(entry);
            
            int languageId = ByteBuffer.wrap(entry, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int offsetToData = ByteBuffer.wrap(entry, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            int actualOffset = offsetToData & 0x7FFFFFFF;
            
            long dataEntryOffset = peInfo.resourceDirBase + actualOffset;
            raf.seek(dataEntryOffset);
            
            byte[] dataEntry = new byte[16];
            raf.read(dataEntry);
            
            int dataRVA = ByteBuffer.wrap(dataEntry, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int size = ByteBuffer.wrap(dataEntry, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            long fileOffset = rvaToFileOffset(raf, dataRVA, peInfo.sectionTableOffset, peInfo.numberOfSections);
            
            if (fileOffset != -1) {
                ResourceEntry resEntry = new ResourceEntry();
                resEntry.id = resourceId;
                resEntry.offset = fileOffset;
                resEntry.size = size;
                results.add(resEntry);
            }
        }
    }
    
    private static List<IconDirEntry> parseIconGroup(byte[] data) {
        List<IconDirEntry> entries = new ArrayList<>();
        
        if (data.length < 6) {
            Log.e(TAG, "Icon group data too small: " + data.length + " bytes");
            return entries;
        }
        
        int reserved = ByteBuffer.wrap(data, 0, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        int type = ByteBuffer.wrap(data, 2, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        int count = ByteBuffer.wrap(data, 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        
        Log.d(TAG, "Icon group header - Reserved: " + reserved + ", Type: " + type + ", Count: " + count);
        
        if (type != 1) {
            Log.e(TAG, "Invalid icon group type: " + type);
            return entries;
        }
        
        for (int i = 0; i < count && (6 + i * 14 + 14) <= data.length; i++) {
            int offset = 6 + i * 14;
            
            IconDirEntry entry = new IconDirEntry();
            entry.width = data[offset] & 0xFF;
            if (entry.width == 0) entry.width = 256;
            
            entry.height = data[offset + 1] & 0xFF;
            if (entry.height == 0) entry.height = 256;
            
            entry.colorCount = data[offset + 2] & 0xFF;
            entry.planes = ByteBuffer.wrap(data, offset + 4, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            entry.bitCount = ByteBuffer.wrap(data, offset + 6, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            entry.bytesInRes = ByteBuffer.wrap(data, offset + 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            entry.iconId = ByteBuffer.wrap(data, offset + 12, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            
            Log.d(TAG, "Icon entry " + i + ": " + entry.width + "x" + entry.height + " " + entry.bitCount + "bpp, ID: " + entry.iconId);
            
            entries.add(entry);
        }
        
        return entries;
    }
    
    private static IconDirEntry selectBestIcon(List<IconDirEntry> entries) {
        IconDirEntry best = entries.get(0);
        
        for (IconDirEntry entry : entries) {
            if (entry.width > best.width || 
                (entry.width == best.width && entry.bitCount > best.bitCount)) {
                best = entry;
            }
        }
        
        return best;
    }
    
    private static byte[] createICOFile(IconDirEntry entry, byte[] iconData) {
        byte[] icoFile = new byte[6 + 16 + iconData.length];
        
        icoFile[0] = 0;
        icoFile[1] = 0;
        icoFile[2] = 1;
        icoFile[3] = 0;
        icoFile[4] = 1;
        icoFile[5] = 0;
        
        int width = entry.width == 256 ? 0 : entry.width;
        int height = entry.height == 256 ? 0 : entry.height;
        
        icoFile[6] = (byte) width;
        icoFile[7] = (byte) height;
        icoFile[8] = (byte) entry.colorCount;
        icoFile[9] = 0;
        
        ByteBuffer.wrap(icoFile, 10, 2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) entry.planes);
        ByteBuffer.wrap(icoFile, 12, 2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) entry.bitCount);
        ByteBuffer.wrap(icoFile, 14, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(iconData.length);
        ByteBuffer.wrap(icoFile, 18, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(22);
        
        System.arraycopy(iconData, 0, icoFile, 22, iconData.length);
        
        return icoFile;
    }
    
    private static Bitmap decodeICO(byte[] icoData) {
        try {
            if (icoData.length < 22) {
                Log.e(TAG, "ICO file too small");
                return null;
            }
            
            int imageOffset = ByteBuffer.wrap(icoData, 18, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int imageSize = ByteBuffer.wrap(icoData, 14, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            if (imageOffset + imageSize > icoData.length) {
                Log.e(TAG, "Invalid ICO file structure");
                return null;
            }
            
            byte[] imageData = new byte[imageSize];
            System.arraycopy(icoData, imageOffset, imageData, 0, imageSize);
            
            if (imageSize > 8 && imageData[0] == (byte)0x89 && imageData[1] == 'P' && 
                imageData[2] == 'N' && imageData[3] == 'G') {
                Log.d(TAG, "Decoding PNG icon");
                return BitmapFactory.decodeByteArray(imageData, 0, imageSize);
            }
            
            if (imageSize < 40) {
                Log.e(TAG, "Image data too small for BMP header");
                return null;
            }
            
            int width = ByteBuffer.wrap(imageData, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int height = ByteBuffer.wrap(imageData, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int bitCount = ByteBuffer.wrap(imageData, 14, 2).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
            
            height = height / 2;
            
            Log.d(TAG, "Decoding BMP icon: " + width + "x" + height + " " + bitCount + "bpp");
            
            if (bitCount == 32) {
                return decodeBMP32(imageData, width, height);
            } else if (bitCount == 24) {
                return decodeBMP24(imageData, width, height);
            } else if (bitCount == 8) {
                return decodeBMP8(imageData, width, height);
            } else if (bitCount == 4) {
                return decodeBMP4(imageData, width, height);
            } else {
                Log.e(TAG, "Unsupported bit count: " + bitCount);
                return null;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error decoding ICO: " + e.getMessage(), e);
            return null;
        }
    }
    
    private static Bitmap decodeBMP32(byte[] data, int width, int height) {
        try {
            int[] pixels = new int[width * height];
            int headerSize = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int offset = headerSize;
            
            int rowSize = width * 4;
            int padding = (4 - (rowSize % 4)) % 4;
            
            for (int y = 0; y < height; y++) {
                int srcY = height - 1 - y;
                int srcOffset = offset + srcY * (rowSize + padding);
                
                for (int x = 0; x < width; x++) {
                    if (srcOffset + 3 >= data.length) break;
                    
                    int b = data[srcOffset++] & 0xFF;
                    int g = data[srcOffset++] & 0xFF;
                    int r = data[srcOffset++] & 0xFF;
                    int a = data[srcOffset++] & 0xFF;
                    
                    pixels[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
            
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding 32-bit BMP: " + e.getMessage());
            return null;
        }
    }
    
    private static Bitmap decodeBMP24(byte[] data, int width, int height) {
        try {
            int[] pixels = new int[width * height];
            int headerSize = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            int offset = headerSize;
            
            int rowSize = width * 3;
            int padding = (4 - (rowSize % 4)) % 4;
            
            for (int y = 0; y < height; y++) {
                int srcY = height - 1 - y;
                int srcOffset = offset + srcY * (rowSize + padding);
                
                for (int x = 0; x < width; x++) {
                    if (srcOffset + 2 >= data.length) break;
                    
                    int b = data[srcOffset++] & 0xFF;
                    int g = data[srcOffset++] & 0xFF;
                    int r = data[srcOffset++] & 0xFF;
                    
                    pixels[y * width + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
            
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding 24-bit BMP: " + e.getMessage());
            return null;
        }
    }
    
    private static Bitmap decodeBMP8(byte[] data, int width, int height) {
        try {
            int[] pixels = new int[width * height];
            int headerSize = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            int[] palette = new int[256];
            int paletteOffset = headerSize;
            for (int i = 0; i < 256; i++) {
                if (paletteOffset + 3 >= data.length) break;
                int b = data[paletteOffset++] & 0xFF;
                int g = data[paletteOffset++] & 0xFF;
                int r = data[paletteOffset++] & 0xFF;
                paletteOffset++;
                palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            
            int offset = headerSize + 256 * 4;
            int rowSize = width;
            int padding = (4 - (rowSize % 4)) % 4;
            
            for (int y = 0; y < height; y++) {
                int srcY = height - 1 - y;
                int srcOffset = offset + srcY * (rowSize + padding);
                
                for (int x = 0; x < width; x++) {
                    if (srcOffset >= data.length) break;
                    int index = data[srcOffset++] & 0xFF;
                    pixels[y * width + x] = palette[index];
                }
            }
            
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding 8-bit BMP: " + e.getMessage());
            return null;
        }
    }
    
    private static Bitmap decodeBMP4(byte[] data, int width, int height) {
        try {
            int[] pixels = new int[width * height];
            int headerSize = ByteBuffer.wrap(data, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            
            int[] palette = new int[16];
            int paletteOffset = headerSize;
            for (int i = 0; i < 16; i++) {
                if (paletteOffset + 3 >= data.length) break;
                int b = data[paletteOffset++] & 0xFF;
                int g = data[paletteOffset++] & 0xFF;
                int r = data[paletteOffset++] & 0xFF;
                paletteOffset++;
                palette[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
            
            int offset = headerSize + 16 * 4;
            int rowSize = (width + 1) / 2;
            int padding = (4 - (rowSize % 4)) % 4;
            
            for (int y = 0; y < height; y++) {
                int srcY = height - 1 - y;
                int srcOffset = offset + srcY * (rowSize + padding);
                
                for (int x = 0; x < width; x += 2) {
                    if (srcOffset >= data.length) break;
                    int byte_ = data[srcOffset++] & 0xFF;
                    int index1 = (byte_ >> 4) & 0x0F;
                    int index2 = byte_ & 0x0F;
                    
                    pixels[y * width + x] = palette[index1];
                    if (x + 1 < width) {
                        pixels[y * width + x + 1] = palette[index2];
                    }
                }
            }
            
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        } catch (Exception e) {
            Log.e(TAG, "Error decoding 4-bit BMP: " + e.getMessage());
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

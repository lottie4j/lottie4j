package com.lottie4j.fxfileviewer.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.Deflater;

/**
 * Utility class for saving images in PNG format without external dependencies.
 * <p>
 * This class provides pure Java implementation of PNG file writing, avoiding the need for
 * Swing/AWT dependencies (ImageIO, BufferedImage). It manually constructs PNG files according
 * to the PNG specification, including proper chunk formatting and CRC checksums.
 * </p>
 */
public class ImageSaver {

    private ImageSaver() {
        // Utility class - hide constructor
    }

    /**
     * Writes a PNG image to the specified output stream.
     * <p>
     * The pixel data should be in ARGB format (8 bits per channel, packed into integers).
     * The output PNG will be in RGBA format with 8-bit depth.
     * </p>
     *
     * @param fos    the output stream to write the PNG data to
     * @param pixels the pixel data in ARGB format (length must equal width * height)
     * @param width  the width of the image in pixels
     * @param height the height of the image in pixels
     * @throws IOException if an I/O error occurs during writing
     */
    public static void writePNG(FileOutputStream fos, int[] pixels, int width, int height) throws IOException {
        // PNG signature
        fos.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        // IHDR chunk
        writeChunk(fos, "IHDR", createIHDR(width, height));

        // IDAT chunk (image data)
        writeChunk(fos, "IDAT", compressImageData(pixels, width, height));

        // IEND chunk
        writeChunk(fos, "IEND", new byte[0]);
    }

    /**
     * Creates the IHDR (Image Header) chunk data for a PNG file.
     * <p>
     * The IHDR chunk specifies the image dimensions, bit depth, color type, and other properties.
     * This implementation creates an RGBA image with 8-bit depth and no interlacing.
     * </p>
     *
     * @param width  the width of the image in pixels
     * @param height the height of the image in pixels
     * @return the IHDR chunk data as a byte array (13 bytes)
     */
    private static byte[] createIHDR(int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate(13);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(width);
        buffer.putInt(height);
        buffer.put((byte) 8);  // bit depth
        buffer.put((byte) 6);  // color type (RGBA)
        buffer.put((byte) 0);  // compression method
        buffer.put((byte) 0);  // filter method
        buffer.put((byte) 0);  // interlace method
        return buffer.array();
    }

    /**
     * Compresses image data for the IDAT chunk.
     * <p>
     * Converts ARGB pixel data to RGBA format and adds PNG filter bytes (set to 0 for no filtering).
     * The resulting data is compressed using DEFLATE compression as required by the PNG specification.
     * </p>
     *
     * @param pixels the pixel data in ARGB format
     * @param width  the width of the image in pixels
     * @param height the height of the image in pixels
     * @return the compressed image data
     * @throws IOException if compression fails
     */
    private static byte[] compressImageData(int[] pixels, int width, int height) throws IOException {
        // Convert ARGB pixels to RGBA bytes with filter byte per scanline
        int rowBytes = width * 4 + 1;  // 4 bytes per pixel + 1 filter byte
        byte[] imageData = new byte[rowBytes * height];

        for (int y = 0; y < height; y++) {
            int rowStart = y * rowBytes;
            imageData[rowStart] = 0;  // filter type: none

            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                int idx = rowStart + 1 + x * 4;

                // Convert ARGB to RGBA
                imageData[idx] = (byte) ((pixel >> 16) & 0xFF);  // R
                imageData[idx + 1] = (byte) ((pixel >> 8) & 0xFF);  // G
                imageData[idx + 2] = (byte) (pixel & 0xFF);  // B
                imageData[idx + 3] = (byte) ((pixel >> 24) & 0xFF);  // A
            }
        }

        // Compress with deflate
        return deflate(imageData);
    }

    /**
     * Compresses data using DEFLATE algorithm.
     *
     * @param data the uncompressed data
     * @return the compressed data
     */
    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        deflater.finish();

        byte[] buffer = new byte[data.length + 100];
        int compressedSize = deflater.deflate(buffer);
        deflater.end();

        byte[] result = new byte[compressedSize];
        System.arraycopy(buffer, 0, result, 0, compressedSize);
        return result;
    }

    /**
     * Writes a PNG chunk to the output stream.
     * <p>
     * A PNG chunk consists of: length (4 bytes), type (4 bytes), data (variable), and CRC (4 bytes).
     * </p>
     *
     * @param fos  the output stream
     * @param type the chunk type (4 ASCII characters, e.g., "IHDR", "IDAT", "IEND")
     * @param data the chunk data
     * @throws IOException if an I/O error occurs
     */
    private static void writeChunk(FileOutputStream fos, String type, byte[] data) throws IOException {
        // Write length
        writeInt(fos, data.length);

        // Write type
        fos.write(type.getBytes());

        // Write data
        fos.write(data);

        // Write CRC
        int crc = calculateCRC(type.getBytes(), data);
        writeInt(fos, crc);
    }

    /**
     * Writes a 32-bit integer in big-endian format.
     *
     * @param fos   the output stream
     * @param value the integer value to write
     * @throws IOException if an I/O error occurs
     */
    private static void writeInt(FileOutputStream fos, int value) throws IOException {
        fos.write((value >> 24) & 0xFF);
        fos.write((value >> 16) & 0xFF);
        fos.write((value >> 8) & 0xFF);
        fos.write(value & 0xFF);
    }

    /**
     * Calculates the CRC-32 checksum for a PNG chunk.
     * <p>
     * The CRC is calculated over the chunk type and data fields, but not the length field.
     * Uses the standard PNG CRC algorithm with polynomial 0xEDB88320.
     * </p>
     *
     * @param type the chunk type bytes
     * @param data the chunk data bytes
     * @return the CRC-32 checksum
     */
    private static int calculateCRC(byte[] type, byte[] data) {
        int crc = 0xFFFFFFFF;

        // Process type
        for (byte b : type) {
            crc = updateCRC(crc, b);
        }

        // Process data
        for (byte b : data) {
            crc = updateCRC(crc, b);
        }

        return crc ^ 0xFFFFFFFF;
    }

    /**
     * Updates the CRC value with a single byte.
     * <p>
     * Uses the standard PNG CRC-32 algorithm.
     * </p>
     *
     * @param crc the current CRC value
     * @param b   the byte to process
     * @return the updated CRC value
     */
    private static int updateCRC(int crc, byte b) {
        crc ^= (b & 0xFF);
        for (int i = 0; i < 8; i++) {
            if ((crc & 1) != 0) {
                crc = (crc >>> 1) ^ 0xEDB88320;
            } else {
                crc = crc >>> 1;
            }
        }
        return crc;
    }
}

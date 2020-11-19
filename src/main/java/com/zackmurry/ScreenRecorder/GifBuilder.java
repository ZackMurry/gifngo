package com.zackmurry.ScreenRecorder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.Arrays;

/**
 * logic taken from this: http://www.java2s.com/Code/Java/2D-Graphics-GUI/AnimatedGifEncoder.htm
 */
public class GifBuilder {

    private int width;
    private int height;
    private int repeat = -1;
    private int frameDelay;
    private String fileName;
    private int sample = 10; // value used for quantizer
    private Color transparent = null; // color for transparency
    private int dispose = -1; // disposal code (-1 = use default)

    private OutputStream out;
    private BufferedImage currentFrame;
    private boolean[] usedEntry = new boolean[256];
    private int colorDepth; // number of bit planes
    private int palSize = 7;
    private byte[] colorTable;
    private int transparentIndex; // index of transparent in color table
    private boolean onFirstFrame = true;
    private boolean closeStream;

    private boolean error = false;

    public GifBuilder(String fileName) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(fileName)));
    }

    public GifBuilder(OutputStream os) throws IOException {
        if (os == null) {
            throw new NullPointerException("Error creating GifBuilder: OutputStream should not be null.");
        }
        out = os;
        writeString("GIF89a"); // header
    }

    public boolean build() {
        if (error) {
            return false;
        }
        try {
            out.write(0x3b); // gif trailer
            out.flush();
            if (closeStream) {
                out.close();
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public GifBuilder withFrames(BufferedImage[] frames) {
        for(BufferedImage frame : frames) {
            if (!addFrame(frame)) {
                error = true;
            }
        }
        return this;
    }

    private boolean addFrame(BufferedImage frame) {
        if (frame == null) {
            return false;
        }

        // setting dimensions based on first frame if not set manually
        if (width == 0 || height == 0) {
            width = frame.getWidth();
            height = frame.getHeight();
        }

        byte[] pixels = getImagePixels(frame);
        byte[] indexedPixels = analyzePixels(pixels);

        try {
            if (onFirstFrame) {
                writeLogicalScreenDescriptor();
                writePalette(); // global color table
                if (repeat >= 0) {
                    // use NS app extension to indicate reps
                    writeNetscapeExt();
                }
            }
            writeGraphicControlExt();
            writeImageDescriptor();
            if (!onFirstFrame) {
                writePalette(); // local color table
            } else {
                onFirstFrame = false;
            }
            writePixels(indexedPixels); // encode and write pixel data
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private int findClosest(Color color) {
        if (colorTable == null) {
            return -1;
        }

        int minPos = 0;
        int dmin = 256 * 256 * 256;
        for (int i = 0; i < colorTable.length; i++) {
            // calculating square magnitude between desired color and actual
            int dr = color.getRed() - (colorTable[i++] & 0xff);
            int dg = color.getGreen() - (colorTable[i++] & 0xff);
            int db = color.getBlue() - (colorTable[i] & 0xff); // bruhh
            int d = dr * dr + dg * dg + db * db;
            int index = i / 3;
            if (usedEntry[index] && d < dmin) {
                dmin = d;
                minPos = index;
            }
        }
        return minPos;
    }

    private byte[] analyzePixels(byte[] pixels) {
        int numPixels = pixels.length / 3;
        byte[] indexedPixels = new byte[numPixels];
        NeuQuant neuQuant = new NeuQuant(pixels, pixels.length, sample);
        colorTable = neuQuant.process();
        // convert map from BGR to RGB
        for (int i = 0; i < colorTable.length; i += 3) {
            byte temp = colorTable[i];
            colorTable[i] = colorTable[i+2];
            colorTable[i+2] = temp;
            usedEntry[i/3] = false;
        }
        // map pixels to new palette
        for (int i = 0, k = 0; i < numPixels; i++) {
            int index = neuQuant.map(pixels[k++] & 0xff, pixels[k++] & 0xff, pixels[k++] & 0xff);
            usedEntry[index] = true;
            indexedPixels[i] = (byte) index;
        }
        colorDepth = 8;
        palSize = 7;
        if (transparent != null) {
            transparentIndex = findClosest(transparent);
        }
        return indexedPixels;
    }

    private byte[] getImagePixels(BufferedImage image) {
        if (width != image.getWidth() || height != image.getHeight() || image.getType() != BufferedImage.TYPE_3BYTE_BGR) {
            // create a new image with the correct size and format
            BufferedImage imgWithCorrectDetails = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g = imgWithCorrectDetails.createGraphics();
            g.drawImage(image, 0, 0, null);
            image = imgWithCorrectDetails;
        }
        return ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
    }

    private void writeLogicalScreenDescriptor() throws IOException {
        writeShort(width);
        writeShort(height);

        // see https://en.wikipedia.org/wiki/GIF#Example_GIF_file
        out.write((0x80 | // global flag color table = 1
                0x70 | // color resolution = 7
                0x00 | // gct sort flag = 0
                palSize // gct
        ));
        out.write(0); // background color index
        out.write(0); // pixel aspect ratio (1:1)
    }

    private void writePalette() throws IOException {
        out.write(colorTable, 0, colorTable.length);
        int n = (3 * 256) - colorTable.length;
        // filling rest of color table if room left
        for (int i = 0; i < n; i++) {
            out.write(0);
        }
    }

    // see http://www.vurdalakov.net/misc/gif/netscape-looping-application-extension
    private void writeNetscapeExt() throws IOException {
        out.write(0x21); // start extension
        out.write(0xff); // extension label
        out.write(11); // block size
        writeString("NETSCAPE 2.0");
        out.write(3); // sub-block size
        out.write(1); // loop sub-block id
        writeShort(repeat); // num of loops (0 = repeat forever)
        out.write(0); // terminate block
    }

    private void writeGraphicControlExt() throws IOException {
        out.write(0x21); // start extension
        out.write(0xf9); // GCE label
        out.write(4); // block size
        int transp, disp;
        if (transparent == null) {
            transp = 0;
            disp = 0; // dispose = no action
        } else {
            transp = 1;
            disp = 2;
        }
        if (dispose >= 0) {
            disp = dispose & 7;
        }
        disp <<= 2;

        out.write(0 | // 1:3 reversed
                 disp | // 4:6 disposal
                0 | // 7 user input -- 0 = none
                transp); // transparency flag

        writeShort(frameDelay); // delay * 1/100 secs
        out.write(transparentIndex);
        out.write(0); // terminate block
    }

    private void writeImageDescriptor() throws IOException {
        out.write(0x2c); // image separator
        writeShort(0); // pos: 0,0
        writeShort(0);
        writeShort(width);
        writeShort(height);

        if (onFirstFrame) {
            // no LCT - GCT is used for first (or only) frame
            out.write(0);
        } else {
            // normal LCT
            out.write(0x80 | // local color table 1=yes
                    0 | // no interlace
                    0 | // no sorting
                    0 | // reserved
                    palSize); // size of color table
        }

    }

    private void writePixels(byte[] indexedPixels) throws IOException {
        LZWEncoder encoder = new LZWEncoder(width, height, indexedPixels, colorDepth);
        encoder.encode(out);
    }

    private void writeShort(int value) throws IOException {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
    }

    private void writeString(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            out.write((byte) s.charAt(i));
        }
    }

    public GifBuilder withWidth(int width) {
        this.width = width;
        return this;
    }

    public GifBuilder withHeight(int height) {
        this.height = height;
        return this;
    }

    public GifBuilder withDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public GifBuilder withDimensions(Dimension dimensions) {
        width = (int) dimensions.getWidth();
        height = (int) dimensions.getHeight();
        return this;
    }

    public GifBuilder withFileName(String fileName) {
        try {
            out = new BufferedOutputStream(new FileOutputStream(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            error = true;
        }
        return this;
    }

    public GifBuilder withFrameRate(double fps) {
        if (fps > 0d) {
            frameDelay = (int) Math.round(100 / fps);
        } else {
            System.out.println("FPS should not be less than or equal to 0. Build failed.");
            error = true;
            return this;
        }
        return this;
    }

    public GifBuilder withFrameDelay(int frameDelay) {
        this.frameDelay = Math.round(frameDelay);
        return this;
    }



}

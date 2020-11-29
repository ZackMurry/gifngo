package com.zackmurry.ScreenRecorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;

/**
 * general logic taken from this: http://www.java2s.com/Code/Java/2D-Graphics-GUI/AnimatedGifEncoder.htm
 * see https://en.wikipedia.org/wiki/GIF#Animated_GIF for more information on what is being written
 * along with https://www.w3.org/Graphics/GIF/spec-gif89a.txt (GIF89a specification)
 *
 * todo: separate class into GifMaker and GifMakerBuilder -- the former would be for producing the GIFs and the latter would be for applying settings to the GifMaker
 */
public class GifBuilder {

    private static final Logger logger = LoggerFactory.getLogger(GifBuilder.class);

    // width and height of gif. by default, these are just the width and height of the first image in the gif.
    private int width;
    private int height;
    
    // how many times the gif should repeat. -1 is 0 times and 0 is infinitely many times
    // doesn't just copy the gif over and over -- uses NetScape extension to do this
    // see writeNetscapeExt() for more info
    private int repeat = -1;
    
    // delay between frames in hundredths of a second
    private int frameDelay;
    
    private String fileName;
    private int sample = 10; // value used for quantizer
    private Color transparent = null; // color for transparency
    
    /* disposal methods give instructions on what to do with an old frame once a new one is displayed
       disposal method codes (see 23 iv on https://www.w3.org/Graphics/GIF/spec-gif89a.txt for more info):
            0: not specified
            1: don't dispose
            2: restore to background color
            3: restore to previous
       disposalMethod is initialized at -1, which indicates to writeGraphicsControlExt() that it should use a default value
    */
    private int disposalMethod = -1;

    
    private OutputStream out;
    private BufferedImage currentFrame;
    private boolean[] usedEntry = new boolean[256];
    private int colorDepth; // number of bit planes
    
    // size of color table palette is 256, but decoder uses raises two to the power of (palSize + 1) to find palette size
    private int palSize = 7;
    
    private byte[] colorTable;
    private int transparentIndex; // index of transparent in color table
    private boolean onFirstFrame = true;
    private boolean closeStream = true;
    private boolean useGlobalColorTable = false;

    private boolean error = false;

    public GifBuilder(String fileName) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(fileName)));
    }

    public GifBuilder(OutputStream os) throws IOException {
        if (os == null) {
            throw new NullPointerException("Error creating GifBuilder: OutputStream should not be null.");
        }
        out = os;
        
        // write header: animated GIF standard
        writeString("GIF89a");
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
    
    // todo move first frame initialization to withFrames method
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
                if (useGlobalColorTable) {
                    writePalette(); // create global color table
                }
                if (repeat >= 0) {
                    // use NS app extension to indicate repetitions
                    writeNetscapeExt();
                }
            }
            writeGraphicControlExt();
            writeImageDescriptor();
            if (!onFirstFrame || !useGlobalColorTable) {
                writePalette(); // local color table
            }
            if (onFirstFrame) {
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
            int db = color.getBlue() - (colorTable[i] & 0xff);
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
        // preferably turn NeuQuant.process into a static method so that i can just do NeuQuant.process(pixels, pixels.length, sample)
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
        // write width of gif in pixels
        writeShort(width);
        
        // write height of gif in pixels
        writeShort(height);

        // definitely see http://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp#logical_screen_descriptor_block for more info on this packed byte
        out.write((useGlobalColorTable ? 0x80 : 0x00) | // global color table flag (0x80 for use GCT and 0x00 for don't)
                0x70 | // color resolution = 7
                0x00 | // no gct sort flag (indicates that the color table is in random order, not in order of decreasing importance)
                palSize // gct
        );
        out.write(0); // background color index is 0
        out.write(0); // pixel aspect ratio is default (1:1)
    }

    // could maybe fix by doing away with the global color table all together (along with the gct flag, of course)
    // it seems like the 
    private void writePalette() throws IOException {
        out.write(colorTable, 0, colorTable.length);
        int n = (3 * 256) - colorTable.length;
        // filling rest of color table if room left
        for (int i = 0; i < n; i++) {
            out.write(0);
        }
    }

    /** 
      * used for repeating gif without copying content
      * see http://www.vurdalakov.net/misc/gif/netscape-looping-application-extension for information about what is being written
      */
    private void writeNetscapeExt() throws IOException {
        // write extension header
        out.write(0x21);
        
        // write application extension label
        out.write(0xff);
        
        // write block size
        out.write(11);
        
        // write application identifier
        writeString("NETSCAPE 2.0");
        
        // write sub-block data size
        out.write(3);
        
        // sub-block id: loop count (1 is the ID for loop count)
        out.write(1);
        
        // write loop count
        writeShort(repeat);
        
        // write block terminator (0x00)
        out.write(0);
    }

    /**
      * see http://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp#graphics_control_extension_block
      * and https://www.w3.org/Graphics/GIF/spec-gif89a.txt at 25
      */
    private void writeGraphicControlExt() throws IOException {
        // write extension header
        out.write(0x21);
        
        // graphics control label
        out.write(0xf9);
        
        // block size
        out.write(4);
        
        int transparentFlag;
        int disposalBits;
        
        if (transparent == null) {
            transparentFlag = 0;
            disposalBits = 0; // dispose = no action
        } else {
            transparentFlag = 1;
            disposalBits = 2;
        }
        
        // if disposal method is not set to default
        if (disposalMethod >= 0) {
            // only take first 3 bits of disposalMethod
            disposalBits = disposalMethod & 7;
        }
        
        // left shift disposalBits by two to make the bit packing easier.
        // the reason this works is that there are two bits of information stored to the right of the disposalBits included in this byte
        // so we can left shift it by two to leave room for them during the bitwise OR
        disposalBits <<= 2;

        out.write(0 | // bits 1-3 are reserved
                 disposalBits | // bits 4-6 are for the disposal method
                0 | // bit 7 is for a user input flag which we aren't using
                transparentFlag); // bit 8 is for a flag for a transparent color

        // write delay between frames
        writeShort(frameDelay);
        
        out.write(transparentIndex);
        out.write(0); // terminate block
    }

    // see chapter 20 of GIF89a specification
    private void writeImageDescriptor() throws IOException {
        // write image separator (it's always 0x2c)
        out.write(0x2c);
        
        // top-left of image is at 0,0
        writeShort(0); // left offset
        writeShort(0); // top offset
        
        writeShort(width);
        writeShort(height);

        if (onFirstFrame && useGlobalColorTable) {
            // no LCT - GCT is used for first (or only) frame
            out.write(0);
        } else {
            // write a global normal local color table (LCT)
            out.write(0x80 | // bit 1: local color table flag (0x80 is 1 followed by 7 0s in binary)
                    0 | // bit 2: no interlace
                    0 | // bit 3: no sorting of color table
                    0 | // bits 4-5: reserved by specification
                    palSize); // bits 6-8: size of color table
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
            logger.error("FPS should not be less than or equal to 0. Build failed.");
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

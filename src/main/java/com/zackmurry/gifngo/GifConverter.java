package com.zackmurry.gifngo;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Class that converts a list of BufferedImages into a GIF89a format (https://www.w3.org/Graphics/GIF/spec-gif89a.txt)
 * some logic taken from here: http://www.java2s.com/Code/Java/2D-Graphics-GUI/AnimatedGifEncoder.htm
 */
public final class GifConverter implements VideoProducer {

    public static final double DEFAULT_FRAMES_PER_SECOND = 24;

    private final Logger logger = LoggerFactory.getLogger(GifConverter.class);

    @Getter @Setter
    private List<Frame> frames;

    @Getter @Setter
    private OutputStream outputStream;

    @Getter @Setter
    private int quantizationSample = 10; // value used for quantizer

    @Getter @Setter
    private int width;

    @Getter @Setter
    private int height;

    @Getter @Setter
    private Color transparentColor = null; // color for transparency

    @Setter
    private boolean shouldCloseStream = true;

    @Setter
    private boolean useGlobalColorTable;

    // how many times the gif should repeat. 0 is infinitely many times (a repeat value of 3 will play the GIF 3 times)
    // doesn't just copy the gif over and over -- uses NetScape extension to do this
    // see writeNetscapeExt() for more info
    @Getter @Setter
    private int repeat = 0;

    /* disposal methods give instructions on what to do with an old frame once a new one is displayed
       disposal method codes (see 23 iv on https://www.w3.org/Graphics/GIF/spec-gif89a.txt for more info):
            0: not specified
            1: don't dispose
            2: restore to background color
            3: restore to previous
       disposalMethod is initialized at -1, which indicates to writeGraphicControlExt() that it should use a default value
    */
    @Getter @Setter
    private int disposalMethod = -1;

    // delay between frames in hundredths of a second
    @Getter @Setter
    private int frameDelay = (int) Math.round(100 / DEFAULT_FRAMES_PER_SECOND);


    private boolean encounteredError;
    private byte[] colorTable;
    private int colorDepth; // number of bit planes
    private final boolean[] usedEntry = new boolean[256];
    private int previousFrameTime;

    // size of color table palette is 256, but decoder uses raises two to the power of (palSize + 1) to find palette size
    private int palSize = 7;
    private int transparentIndex; // index of transparent in color table

    public GifConverter() {

    }

    public GifConverter(List<Frame> frames) {
        this.frames = frames;
    }

    public GifConverter(List<Frame> frames, OutputStream outputStream) {
        this.frames = frames;
        this.outputStream = outputStream;
    }

    /**
     * method that starts the processing of frames and saves the gif to the OutputStream
     * @return boolean representing if the converting succeeded
     */
    @Override
    public boolean process() {
        if (encounteredError || !isReady()) {
            return false;
        }

        // write header: animated GIF standard
        try {
            writeString("GIF89a");
        } catch (IOException e) {
            logger.error("Error writing GIF header. This is likely because of a bad output stream. Trying to continue...");
            e.printStackTrace();
            encounteredError = true;
        }

        if (frames == null || frames.size() == 0) {
            return false;
        }

        doFirstFrameProcessing();

        for (int i = 1; i < frames.size(); i++) {
            processFrame(frames.get(i));
        }

        try {
            outputStream.write(0x3b); // gif trailer
            outputStream.flush();
            if (shouldCloseStream) {
                outputStream.close();
            }
        } catch (IOException e) {
            logger.warn("Problem closing output stream. This is non-fatal. Please close the program to view your GIF.");
            e.printStackTrace();
            encounteredError = true;
        }
        if (encounteredError) {
            logger.warn("Encountered an error while converting to GIF. There is likely more logging above. The GIF may still be valid.");
        }
        return !encounteredError;
    }

    private void processFrame(Frame frame) {
        if (frame == null) {
            encounteredError = true;
            return;
        }

        byte[] pixels = getImagePixels(frame.getImage());
        byte[] indexedPixels = analyzePixels(pixels);
        try {
            writeGraphicControlExt((int) Math.round((frame.getTimeSinceStart() - previousFrameTime) / 10d));
            previousFrameTime = frame.getTimeSinceStart();
            writeImageDescriptor();
            if (!useGlobalColorTable) {
                writePalette();
            }
            writePixels(indexedPixels);
        } catch (IOException e) {
            logger.error("Error writing graphic control extension for a frame. Trying to continue...");
            e.printStackTrace();
            encounteredError = true;
        }

    }

    private void doFirstFrameProcessing() {
        try {

            Frame firstFrame = frames.get(0);

            if (width == 0) {
                width = firstFrame.getImage().getWidth();
            }
            if (height == 0) {
                height = firstFrame.getImage().getHeight();
            }

            if (useGlobalColorTable) {
                writePalette();
            }

            try {
                writeLogicalScreenDescriptor();
            } catch (IOException e) {
                logger.error("Error writing logical screen descriptor.");
                e.printStackTrace();
                encounteredError = true;
            }

            if (repeat == 0 || repeat > 1) {
                writeNetscapeExt();
            }

            byte[] pixels = getImagePixels(firstFrame.getImage());
            byte[] indexedPixels = analyzePixels(pixels);

            writeGraphicControlExt(firstFrame.getTimeSinceStart());
            previousFrameTime = firstFrame.getTimeSinceStart();
            writeImageDescriptor();
            if (!useGlobalColorTable) {
                writePalette();
            }
            writePixels(indexedPixels);
        } catch (IOException e) {
            logger.error("Error processing first frame.");
            encounteredError = true;
        }
    }

    private byte[] analyzePixels(byte[] pixels) {
        int numPixels = pixels.length / 3;
        byte[] indexedPixels = new byte[numPixels];
        // preferably turn NeuQuant.process into a static method so that i can just do NeuQuant.process(pixels, pixels.length, sample)
        NeuQuant neuQuant = new NeuQuant(pixels, pixels.length, quantizationSample);
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
        if (transparentColor != null) {
            transparentIndex = findClosest(transparentColor);
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

    private void writePalette() throws IOException {
        outputStream.write(colorTable, 0, colorTable.length);
        int n = (3 * 256) - colorTable.length;
        // filling rest of color table if room left
        for (int i = 0; i < n; i++) {
            outputStream.write(0);
        }
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

    private void writePixels(byte[] indexedPixels) throws IOException {
        LZWEncoder encoder = new LZWEncoder(width, height, indexedPixels, colorDepth);
        encoder.encode(outputStream);
    }

    // see chapter 20 of GIF89a specification
    private void writeImageDescriptor() throws IOException {
        // write image separator (it's always 0x2c)
        outputStream.write(0x2c);

        // top-left of image is at 0,0
        writeShort(0); // left offset
        writeShort(0); // top offset

        writeShort(width);
        writeShort(height);

        // write a flags for normal local color table (LCT)
        outputStream.write(0b10000000 | // bit 1: local color table flag
                0 | // bit 2: no interlace
                0 | // bit 3: no sorting of color table
                0 | // bits 4-5: reserved by specification
                palSize); // bits 6-8: size of color table

    }

    /**
     * used for repeating gif without copying content
     * see http://www.vurdalakov.net/misc/gif/netscape-looping-application-extension for information about what is being written
     */
    private void writeNetscapeExt() throws IOException {
        // write extension header
        outputStream.write(0x21);

        // write application extension label
        outputStream.write(0xff);

        // write block size
        outputStream.write(0x0b);

        // write application identifier
        writeString("NETSCAPE2.0");

        // write sub-block data size
        outputStream.write(3);

        // sub-block id: loop count (1 is the ID for loop count)
        outputStream.write(1);

        // write loop count -- takes up two bytes
        writeShort(repeat & 0xffff);

        // write block terminator
        outputStream.write(0x00);
    }

    /**
     * see http://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp#graphics_control_extension_block
     * and https://www.w3.org/Graphics/GIF/spec-gif89a.txt at 25
     */
    private void writeGraphicControlExt() throws IOException {
        // write extension header
        outputStream.write(0x21);

        // graphics control label
        outputStream.write(0xf9);

        // block size
        outputStream.write(4);

        int transparentFlag;
        int disposalBits;

        if (transparentColor == null) {
            transparentFlag = 0;
            disposalBits = 0; // dispose = no action
        } else {
            transparentFlag = 1;
            disposalBits = 2;
        }

        // if disposal method is not set to default
        if (disposalMethod >= 0) {
            // only take last 3 bits of disposalMethod
            disposalBits = disposalMethod & 0b111;
        }

        // left shift disposalBits by two to make the bit packing easier.
        // the reason this works is that there are two bits of information stored to the right of the disposalBits included in this byte
        // so we can left shift it by two to leave room for them during the bitwise OR
        disposalBits <<= 2;

        outputStream.write(0 | // bits 1-3 are reserved
                disposalBits | // bits 4-6 are for the disposal method
                0 | // bit 7 is for a user input flag which we aren't using
                transparentFlag); // bit 8 is for a flag for a transparent color

        // write delay between frames
        // todo this could be set to account for variations in frame rate
        // for example, if a n frames were skipped before this one, it could be played for (n + 1) times as long to compensate.
        // this would probably involve wrapping each frame in an object that contains the time (could be absolute or relative to the recording start)
        // and then iterating through them to find gaps in frames and compensating through them like that.
        writeShort(frameDelay);

        outputStream.write(transparentIndex);
        outputStream.write(0); // terminate block
    }

    /**
     * see http://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp#graphics_control_extension_block
     * and https://www.w3.org/Graphics/GIF/spec-gif89a.txt at 25
     */
    private void writeGraphicControlExt(int time) throws IOException {
        // write extension header
        outputStream.write(0x21);

        // graphics control label
        outputStream.write(0xf9);

        // block size
        outputStream.write(4);

        int transparentFlag;
        int disposalBits;

        if (transparentColor == null) {
            transparentFlag = 0;
            disposalBits = 0; // dispose = no action
        } else {
            transparentFlag = 1;
            disposalBits = 2;
        }

        // if disposal method is not set to default
        if (disposalMethod >= 0) {
            // only take last 3 bits of disposalMethod
            disposalBits = disposalMethod & 0b111;
        }

        // left shift disposalBits by two to make the bit packing easier.
        // the reason this works is that there are two bits of information stored to the right of the disposalBits included in this byte
        // so we can left shift it by two to leave room for them during the bitwise OR
        disposalBits <<= 2;

        outputStream.write(0 | // bits 1-3 are reserved
                disposalBits | // bits 4-6 are for the disposal method
                0 | // bit 7 is for a user input flag which we aren't using
                transparentFlag); // bit 8 is for a flag for a transparent color

        // write delay between frames
        // todo this could be set to account for variations in frame rate
        // for example, if a n frames were skipped before this one, it could be played for (n + 1) times as long to compensate.
        // this would probably involve wrapping each frame in an object that contains the time (could be absolute or relative to the recording start)
        // and then iterating through them to find gaps in frames and compensating through them like that.
        writeShort(time);

        outputStream.write(transparentIndex);
        outputStream.write(0); // terminate block
    }

    private void writeLogicalScreenDescriptor() throws IOException {
        // write width of gif in pixels
        writeShort(width);

        // write height of gif in pixels
        writeShort(height);

        // definitely see http://www.matthewflickinger.com/lab/whatsinagif/bits_and_bytes.asp#logical_screen_descriptor_block for more info on this packed byte
        outputStream.write((useGlobalColorTable ? 0x80 : 0x00) | // global color table flag (0x80 for use GCT and 0x00 for don't)
                0x70 | // color resolution = 7
                0x00 | // no gct sort flag (indicates that the color table is in random order, not in order of decreasing importance)
                palSize // gct
        );
        outputStream.write(0); // background color index is 0
        outputStream.write(0); // pixel aspect ratio is default (1:1)
    }

    private void writeShort(int value) throws IOException {
        outputStream.write(value & 0xff);
        outputStream.write((value >> 8) & 0xff);
    }

    private void writeString(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            outputStream.write((byte) s.charAt(i));
        }
    }

    @Override
    public void setFrameRate(double fps) {
        if (fps <= 0) {
            logger.warn("The frames per second of a GifConverter should always be greater than 0.");
            encounteredError = true;
            return;
        }
        this.frameDelay = (int) Math.round(100 / fps);
    }

    @Override
    public boolean getUseGlobalColorTable() {
        return useGlobalColorTable;
    }

    @Override
    public boolean getShouldCloseStream() {
        return shouldCloseStream;
    }

    @Override
    public boolean isReady() {
        return !encounteredError && frames.size() > 0 && outputStream != null;
    }

    /**
     * clones settings of GifConverter, not the frames and stuff.
     * notably doesn't clone width and height, as almost always, those are determined by the class itself
     * @return a copy of this with the settings given to it previously
     */
    @Override
    public GifConverter cloneSettings() {
        GifConverter clone = new GifConverter();
        clone.setRepeat(repeat);
        clone.setQuantizationSample(quantizationSample);
        clone.setTransparentColor(transparentColor);
        clone.setShouldCloseStream(shouldCloseStream);
        clone.setFrameDelay(frameDelay);
        clone.setDisposalMethod(disposalMethod);
        clone.setUseGlobalColorTable(useGlobalColorTable);
        return clone;
    }
}

package com.zackmurry.gifngo.converter;

import com.zackmurry.gifngo.models.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * see https://en.wikipedia.org/wiki/GIF#Animated_GIF for more information on what is being written
 */
public class GifConverterBuilder {

    private static final Logger logger = LoggerFactory.getLogger(GifConverterBuilder.class);

    private final GifConverter gifConverter = new GifConverter();

    public GifConverterBuilder(String fileName) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(fileName)));
    }

    public GifConverterBuilder(OutputStream os) {
        if (os == null) {
            throw new NullPointerException("Error creating GifBuilder: OutputStream should not be null.");
        }
        this.gifConverter.setOutputStream(os);
    }

    public GifConverter build() {
        if (!gifConverter.isReady()) {
            logger.error("Error finishing GifConverter build: GifConverter needs a list of frames, the fps it needs to record at (or a frame delay), and an OutputStream. One or more of these are not present.");
            return null;
        }
        return gifConverter;
    }

    public GifConverter getCopy() {
        if (!gifConverter.isReady()) {
            logger.error("Error finishing GifConverter build: GifConverter needs a list of frames, the fps it needs to record at (or a frame delay), and an OutputStream. One or more of these are not present.");
            return null;
        }
        return gifConverter.cloneSettings();
    }

    public GifConverterBuilder withFrames(Frame[] frames) {
        this.gifConverter.setFrames(Arrays.stream(frames).collect(Collectors.toList()));
        return this;
    }

    public GifConverterBuilder withFrames(List<Frame> frames) {
        this.gifConverter.setFrames(frames);
        return this;
    }


    public GifConverterBuilder withWidth(int width) {
        this.gifConverter.setWidth(width);
        return this;
    }

    public GifConverterBuilder withHeight(int height) {
        this.gifConverter.setHeight(height);
        return this;
    }

    public GifConverterBuilder withDimensions(int width, int height) {
        this.gifConverter.setWidth(width);
        this.gifConverter.setHeight(height);
        return this;
    }

    public GifConverterBuilder withDimensions(Dimension dimensions) {
        this.gifConverter.setWidth((int) dimensions.getWidth());
        this.gifConverter.setHeight((int) dimensions.getHeight());
        return this;
    }

    public GifConverterBuilder withFrameRate(double fps) {
        if (fps <= 0d) {
        logger.warn("Frames per second should not be less than or equal to zero. Reverting to default for frames per second ({}).", GifConverter.DEFAULT_FRAMES_PER_SECOND);
            this.gifConverter.setFrameDelay((int) Math.round(100 / GifConverter.DEFAULT_FRAMES_PER_SECOND));
        }
        this.gifConverter.setFrameDelay((int) Math.round(100 / fps));
        return this;
    }

    public GifConverterBuilder withFrameDelay(int frameDelay) {
        this.gifConverter.setFrameDelay(frameDelay);
        return this;
    }

    public GifConverterBuilder withGlobalColorTable(boolean useGlobalColorTable) {
        this.gifConverter.setUseGlobalColorTable(useGlobalColorTable);
        return this;
    }

    public GifConverterBuilder shouldCloseStream(boolean shouldCloseStream) {
        this.gifConverter.setShouldCloseStream(shouldCloseStream);
        return this;
    }

    public GifConverterBuilder withTransparentColor(Color color) {
        this.gifConverter.setTransparentColor(color);
        return this;
    }

    public GifConverterBuilder withQuantizationSample(int quantizationSample) {
        this.gifConverter.setQuantizationSample(quantizationSample);
        return this;
    }

    public GifConverterBuilder repeatInfinitely() {
        this.gifConverter.setRepeat(0);
        return this;
    }

    public GifConverterBuilder noRepeat() {
        this.gifConverter.setRepeat(-1);
        return this;
    }

    public GifConverterBuilder withRepeat(int repeatCount) {
        this.gifConverter.setRepeat(repeatCount);
        return this;
    }

    public GifConverterBuilder withDisposalMethod(int disposalMethod) {
        this.gifConverter.setDisposalMethod(disposalMethod);
        return this;
    }

}

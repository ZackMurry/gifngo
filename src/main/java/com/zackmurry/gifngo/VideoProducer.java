package com.zackmurry.gifngo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.List;

public interface VideoProducer {

    boolean process();

    void setFrameRate(double fps);

    void setWidth(int width);
    int getWidth();

    void setHeight(int height);
    int getHeight();

    void setFrameDelay(int delay);
    int getFrameDelay();

    void setUseGlobalColorTable(boolean useGlobalColorTable);
    boolean getUseGlobalColorTable();

    void setShouldCloseStream(boolean closeStream);
    boolean getShouldCloseStream();

    void setTransparentColor(Color color);
    Color getTransparentColor();

    void setQuantizationSample(int sample);
    int getQuantizationSample();

    void setRepeat(int repeat);
    int getRepeat();

    void setDisposalMethod(int disposalMethod);
    int getDisposalMethod();

    void setFrames(List<BufferedImage> frames);
    List<BufferedImage> getFrames();

    void setOutputStream(OutputStream out);
    OutputStream getOutputStream();

    boolean isReady();

    VideoProducer cloneSettings();

}

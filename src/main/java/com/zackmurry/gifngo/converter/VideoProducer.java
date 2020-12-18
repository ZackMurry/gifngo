package com.zackmurry.gifngo.converter;

import com.zackmurry.gifngo.models.Frame;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.List;

public interface VideoProducer {

    boolean process();

    void setWidth(int width);
    int getWidth();

    void setHeight(int height);
    int getHeight();

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

    void setFrames(List<com.zackmurry.gifngo.models.Frame> frames);
    List<Frame> getFrames();

    void setOutputStream(OutputStream out);
    void setOutputFile(String fileName) throws FileNotFoundException;
    OutputStream getOutputStream();

    boolean isReady();

    VideoProducer cloneSettings();

}

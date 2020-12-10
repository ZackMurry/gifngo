package com.zackmurry.gifngo;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * class that takes screenshots and delivers them to a GIF builder
 *
 * still haven't decided if this should save the images as jpegs and then call another class to orchestrate a gif builder or not.
 * the main consideration is heap space, which in tests has ran out (presumably due to the images in the captures array list being stored in memory
 * instead of on the hard drive. thus, a good fix would be to store them in the captures folder, but there is more thought that needs to be put into that.
 * a good file structure would probably separate GIFs into their own folders in the captures directory, as to make it clear to a parser which photos belong where,
 * with the order determined by the names of the files, which would just be System.currentTimeMillis() at each respective time, so they could just be numerically
 * sorted.
 *
 * something else that needs to happen is the capability for recording multiple gifs without restarting the program. this is pretty much a necessity and is fairly
 * incompatible with the current set up, but could be implemented pretty easily if the photos were stored in the hard drive instead of in memory.
 *
 * another useful feature would be the ability to delay building GIFs until the user is ready for it, as it'd be pretty inconvenient to be running
 * a quantization algorithm while the user is in a match or something
 *
 */
public class ScreenRecorderManager {

    private static final Logger logger = LoggerFactory.getLogger(ScreenRecorderManager.class);

    private boolean recording = false;
    
    private static final String DOWNLOADS_FOLDER_PATH = System.getProperty("user.home") + File.separator + "Downloads";

    @Getter
    private int framesPerSecond = 24;

    private int timeBetweenCapturesMs = 1000 / framesPerSecond;

    // the time that each individual thread should wait between captures
    private int timeBetweenThreadCaptures;

    private final String captureFolderName = "captures";
    File capturesFolder = new File(captureFolderName);

    @Getter @Setter
    private double strictFps = -1;

    @Getter @Setter
    private boolean saveToDownloadsFolder = true;

    @Getter @Setter
    private String outputFileName;

    @Getter @Setter
    private int repeat = 0;

    @Getter @Setter
    private boolean singleRecording = false;

    private final ArrayList<BufferedImage> captures = new ArrayList<>();
    private final ArrayList<ScreenRecorder> screenRecorders = new ArrayList<>();
    private long recordStartTime;
    private int threadCount;

    public ScreenRecorderManager() {
        this(1);
    }

    public ScreenRecorderManager(int threadCount) {
        this.threadCount = threadCount;
        logger.debug("Set to record on {} threads.", threadCount);
        this.timeBetweenThreadCaptures = timeBetweenCapturesMs * threadCount;
        if (!capturesFolder.exists()) {
            if (!capturesFolder.mkdir()) {
                logger.error("Error initializing: captures folder could not be created.");
            }
        } else {
            // if the folder for the images exists, clear the images
            clearCapturesFolder();
        }
    }

    private void saveImageToCaptures(BufferedImage image, String fileName) throws IOException {
        ImageIO.write(image, "jpeg", new File(fileName));
    }

    public void startRecording() {
        if (recording) {
            logger.error("Cannot start recording while already recording.");
            return;
        }
        logger.info("Recording...");
        recording = true;

        screenRecorders.clear();

        for (int i = 0; i < threadCount; i++) {
            int recordingOffset = i * timeBetweenCapturesMs;
            screenRecorders.add(new ScreenRecorder(recordingOffset, timeBetweenThreadCaptures));
        }
        // starting after construction so that they all start at roughly the same time
        screenRecorders.forEach(ScreenRecorder::startRecording);
        recordStartTime = System.currentTimeMillis();
    }

    public void stopRecording() {
        if (!recording) {
            logger.error("ERROR: Cannot stop recording if not currently recording.");
            return;
        }
        logger.info("Stopped recording.");
        recording = false;

        List<List<BufferedImage>> separatedCaptures = new ArrayList<>();
        for (ScreenRecorder recorder : screenRecorders) {
            separatedCaptures.add(recorder.stopRecording());
        }

        for (int i = 0; i < separatedCaptures.get(screenRecorders.size() - 1).size(); i++) {
            for (List<BufferedImage> caps : separatedCaptures) {
                if (i < caps.size()) {
                    captures.add(caps.get(i));
                }
            }
        }

        // todo adjust export frame rate to real frame rate
        double secondsRecorded = (System.currentTimeMillis() - recordStartTime) / 1000d;
        double realFramesPerSecond = captures.size() / secondsRecorded;
        logger.debug("Recorded for {} seconds at {} frames per second. Recorded at {} real frames per second.", secondsRecorded, framesPerSecond, realFramesPerSecond);
        double absStrictFps = Math.abs(strictFps);
        if (strictFps != 0 && (realFramesPerSecond - absStrictFps > framesPerSecond || realFramesPerSecond + absStrictFps < framesPerSecond)) {
            if (strictFps > 0) {
                logger.error("Recording failed: expected {} +/- {} frames per second, but got {} frames per second.", framesPerSecond, strictFps, realFramesPerSecond);
                return;
            } else {
                logger.warn("Frames per second is more than {} away from the desired frame rate ({}). Frame rate: {}. Continuing...", -strictFps, framesPerSecond, realFramesPerSecond);
            }
        }

        logger.info("Building GIF...");
        
        String outputPath;
        String gifFileName = (outputFileName != null ? outputFileName : System.currentTimeMillis()) + ".gif";
        if (saveToDownloadsFolder) {
            // todo probably have a prettier default file name
            outputPath = DOWNLOADS_FOLDER_PATH + File.separator + gifFileName;
        } else {
            outputPath = gifFileName;
        }
        
        GifConverterBuilder gifConverterBuilder;
        try {
            gifConverterBuilder = new GifConverterBuilder(outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        logger.info("Processing {} captures...", captures.size());


        
        GifConverter gifConverter = gifConverterBuilder
                .withFrameRate(framesPerSecond)
                .withFrames(captures.toArray(BufferedImage[]::new))
                .build();
        gifConverter.process();
        logger.info("GIF successfully created. Saved to {}.", outputPath);

        captures.clear();
        if (singleRecording) {
            System.exit(0);
        }
    }

    public void toggleRecording() {
        if (recording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void clearCapturesFolder() {
        File[] captures = capturesFolder.listFiles();
        if (captures == null) {
            logger.error("ERROR: Captures should not be null");
            return;
        }
        for (File f : captures) {
            f.delete();
        }
    }

    public boolean isRecording() {
        return recording;
    }
    
    public void setFramesPerSecond(int fps) {
        if (recording) {
            logger.warn("Updating frames per second while recording will produce unexpected behavior. The GIF encoder will only receive the frames per second at the last frame, meaning that all of the other frames will be played at the final framerate.");
        }
        framesPerSecond = fps;
        timeBetweenCapturesMs = 1000 / fps;
        timeBetweenThreadCaptures = timeBetweenCapturesMs * threadCount;
    }
    
}

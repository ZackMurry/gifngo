package com.zackmurry.ScreenRecorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
 * another useful feature would be the ability to delay building GIFs until the user is ready for it, as it'd be pretty inconvienent to be running
 * a quantization algorithm while the user is in a match or something
 *
 * todo: have dev mode with more thorough logs and prod mode with more user-friendly logs
 */
public class ScreenRecorderManager {

    private static final Logger logger = LoggerFactory.getLogger(ScreenRecorderManager.class);

    private boolean recording = false;
    
    private static final String DOWNLOADS_FOLDER_PATH = System.getProperty("user.home") + File.separator + "Downloads";

    private int framesPerSecond = 15;
    private final String captureFolderName = "captures";
    File capturesFolder = new File(captureFolderName);
    private double acceptableFrameRateDifference = 0.5;
    private boolean failOnUnacceptableFrameRate = true;

    private ArrayList<BufferedImage> captures = new ArrayList<>();
    
    private boolean saveToDownloadsFolder = true;
    private String outputFileName;
    private boolean recordingFailed;

    private ScreenRecorder screenRecorder;
    private long recordStartTime;

    public ScreenRecorderManager() {
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
        screenRecorder = new ScreenRecorder(framesPerSecond);
        screenRecorder.startRecording();
        recordStartTime = System.currentTimeMillis();
    }

    public void stopRecording() {
        if (!recording) {
            logger.error("ERROR: Cannot stop recording if not currently recording.");
            return;
        }
        logger.info("Stopped recording.");
        recording = false;
        captures = screenRecorder.stopRecording();

        // todo maybe adjust export frame rate if the frame rate is more than a couple off
        double secondsRecorded = (System.currentTimeMillis() - recordStartTime) / 1000d;
        double realFramesPerSecond = captures.size() / secondsRecorded;
        logger.debug("Recorded for {} seconds at {} frames per second. Recorded at {} real frames per second.", secondsRecorded, framesPerSecond, realFramesPerSecond);
        if (realFramesPerSecond - acceptableFrameRateDifference > framesPerSecond || realFramesPerSecond + acceptableFrameRateDifference < framesPerSecond) {
            if (failOnUnacceptableFrameRate) {
                logger.error("Frame rate unacceptable. Expected frame rate: {}; real frame rate: {}.", framesPerSecond, realFramesPerSecond);
                recordingFailed = true;
            } else {
                logger.warn("Frame rate unacceptable. Expected frame rate: {}; real frame rate: {}.", framesPerSecond, realFramesPerSecond);
            }
        }

        if (recordingFailed) {
            logger.error("Recording failed. There is likely more logs above.");
            return;
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
        
        GifBuilder gifBuilder;
        try {
            gifBuilder = new GifBuilder(outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        logger.info("Processing {} captures...", captures.size());


        
        gifBuilder
                .withFrameRate(framesPerSecond)
                .withFrames(captures.toArray(BufferedImage[]::new))
                .build();
        logger.info("GIF successfully created. Saved to {}.", outputPath);
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
    }
    
    public int getFramesPerSecond() {
        return framesPerSecond;
    }
    
    /**
      * when set to true, this will make the output directory for the gif be the downloads folder
      */
    public void setSaveToDownloadsFolder(boolean shouldSaveToDownloadsFolder) {
        saveToDownloadsFolder = shouldSaveToDownloadsFolder;
    }
    
    public boolean getSaveToDownloadsFolder() {
        return saveToDownloadsFolder;
    }
    
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public String getOutputFileName() {
        return outputFileName;
    }
    
}

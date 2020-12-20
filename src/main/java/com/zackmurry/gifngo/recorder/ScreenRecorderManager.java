package com.zackmurry.gifngo.recorder;

import com.zackmurry.gifngo.Constants;
import com.zackmurry.gifngo.models.Frame;
import com.zackmurry.gifngo.converter.GifConverter;
import com.zackmurry.gifngo.models.ImageDimension;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * class that orders ScreenRecorders to take screenshots and then delivers them to a GifConverter
 */
public class ScreenRecorderManager {

    private static final Logger logger = LoggerFactory.getLogger(ScreenRecorderManager.class);

    private boolean recording = false;
    
    private static final String DOWNLOADS_FOLDER_PATH = System.getProperty("user.home") + File.separator + "Downloads";

    @Getter
    private int framesPerSecond = 24;

    private int timeBetweenCapturesMs = (int) Math.round(1000d / framesPerSecond);

    // the time that each individual thread should wait between captures
    private int timeBetweenThreadCaptures;

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

    @Getter @Setter
    private ImageDimension outputDimensions = ImageDimension.fromString(Constants.DEFAULT_RESOLUTION);

    @Getter @Setter
    private boolean waitForBuild;

    private final ArrayList<ScreenRecorder> screenRecorders = new ArrayList<>();
    private long recordStartTime;
    private final int threadCount;

    private static final File capturesFolder = new File("captures");

    public ScreenRecorderManager() {
        this(1);
    }

    public ScreenRecorderManager(int threadCount) {
        if (threadCount <= 0) {
            logger.error("Thread count must be greater than 0.");
            throw new IllegalArgumentException("Thread count must be greater than 0.");
        }
        this.threadCount = threadCount;
        logger.debug("Set to record on {} threads.", threadCount);
        this.timeBetweenThreadCaptures = timeBetweenCapturesMs * threadCount;
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

        final List<List<Frame>> separatedCaptures = new ArrayList<>();
        for (ScreenRecorder recorder : screenRecorders) {
            separatedCaptures.add(recorder.stopRecording());
        }

        final ArrayList<Frame> captures = new ArrayList<>();
        for (int i = 0; i < separatedCaptures.get(screenRecorders.size() - 1).size(); i++) {
            for (List<Frame> caps : separatedCaptures) {
                if (i < caps.size()) {
                    captures.add(caps.get(i));
                }
            }
        }

        final double secondsRecorded = (System.currentTimeMillis() - recordStartTime) / 1000d;
        final double realFramesPerSecond = captures.size() / secondsRecorded;
        logger.debug("Recorded for {} seconds at {} frames per second. Recorded at {} real frames per second.", secondsRecorded, framesPerSecond, realFramesPerSecond);

        final double absStrictFps = Math.abs(strictFps);
        if (strictFps != 0 && (realFramesPerSecond - absStrictFps > framesPerSecond || realFramesPerSecond + absStrictFps < framesPerSecond)) {
            if (strictFps > 0) {
                logger.error("Recording failed: expected {} +/- {} frames per second, but got {} frames per second.", framesPerSecond, absStrictFps, realFramesPerSecond);
                return;
            } else {
                logger.warn("Frames per second is more than {} away from the desired frame rate ({}). Frame rate: {}. Continuing...", absStrictFps, framesPerSecond, realFramesPerSecond);
            }
        }

        if (waitForBuild) {
            // add images to folder in captures folder
            addGifImagesToCapturesFolder(String.valueOf(System.currentTimeMillis()), captures);
            return;
        }

        captures.forEach(capture -> capture.setImage(ImageResizer.resize(capture.getImage(), outputDimensions)));

        logger.info("Building GIF...");
        
        String outputPath = generateOutputFilePath();
        
        GifConverter gifConverter;
        try {
            gifConverter = new GifConverter(captures, outputPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        logger.info("Processing {} captures...", captures.size());
        gifConverter.process();
        logger.info("GIF successfully created. Saved to {}.", outputPath);

        if (singleRecording) {
            System.exit(0);
        }
    }

    private String generateOutputFilePath() {
        final String gifFileName = (outputFileName != null ? outputFileName : System.currentTimeMillis()) + ".gif";
        if (saveToDownloadsFolder) {
            return DOWNLOADS_FOLDER_PATH + File.separator + gifFileName;
        } else {
            return gifFileName;
        }
    }

    private void addGifImagesToCapturesFolder(String folderName, List<Frame> images) {
        if (!capturesFolder.exists()) {
            if (!capturesFolder.mkdir()) {
                logger.error("Failed to create captures folder. Aborting...");
                System.exit(1);
            }
        }

        final File gifFolder = new File(capturesFolder.getPath() + "\\" + folderName);
        if (!gifFolder.mkdir()) {
            logger.error("Failed to create folder for gif images. Gif save failed.");
            return;
        }
        logger.debug("Saving {} image{} to {}...", images.size(), images.size() != 1 ? "s" : "", gifFolder.getAbsolutePath());

        try {
            for (Frame image : images) {
                boolean wrote = ImageIO.write(image.getImage(), "jpeg", new File(gifFolder.getPath() + "\\" + image.getTimeSinceStart() + ".jpeg"));
                if (!wrote) {
                    logger.warn("Wrote is false");
                }
            }
        } catch (IOException e) {
            logger.error("Error saving images to disk. Deleting the folder for that gif and aborting...");
            if (gifFolder.delete()) {
                logger.debug("Successfully deleted gif folder.");
            } else {
                logger.warn("Error deleting folder {}. This may cause unexpected behavior after starting the program again. It is recommended to delete this folder.", gifFolder);
            }
            System.exit(1);
        }
        logger.info("Gif files saved.");
    }

    public void toggleRecording() {
        if (recording) {
            stopRecording();
        } else {
            startRecording();
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

    /**
     * deletes a directory with only files insides (i.e., no sub-directories)
     * @param f file to delete
     * @return boolean representing success
     */
    private boolean deleteFileDirectory(File f) {
        File[] files = f.listFiles();
        if (files == null) {
            return f.delete();
        }
        for (File file : files) {
            if (!file.delete()) {
                return false;
            }
        }
        return f.delete();
    }

    public void buildGifs() {
        logger.info("Building gifs...");
        if (!waitForBuild) {
            logger.debug("Received a call to buildGifs() with waitForBuild off.");
            return;
        }

        if (!capturesFolder.exists()) {
            logger.info("No gifs to build.");
            return;
        }


        final File[] gifFolders = capturesFolder.listFiles();

        if (gifFolders == null) {
            logger.warn("No folders for gifs found. Not building any gifs.");
            return;
        }

        // todo this might create an OutOfMemoryError. this could be changed to get one image at a time and pass them to the GifConverter, which would avoid using a ton of memory
        for (File gifFolder : gifFolders) {
            final File[] imageFiles = gifFolder.listFiles();

            if (imageFiles == null) {
                logger.warn("A gif folder is empty. Skipping...");
                if (!deleteFileDirectory(gifFolder)) {
                    logger.warn("Failed to delete folder for a gif. Location: {}", gifFolder.getAbsolutePath());
                }
                continue;
            }

            final ArrayList<Frame> frames = new ArrayList<>();
            logger.debug("Processing {} images...", imageFiles.length);
            for (File imageFile : imageFiles) {
                BufferedImage image;
                int imageName;
                try {
                    image = ImageIO.read(imageFile);
                    // substring file name to remove .jpeg at the end
                    String fileName = imageFile.getName().substring(0, imageFile.getName().length() - 5);
                    imageName = Integer.parseInt(fileName);
                } catch (IOException | NumberFormatException e) {
                    logger.error("Exception occurred when reading an image of a gif. Skipping image...");
                    continue;
                }
                frames.add(new Frame(image, imageName));
            }

            frames.forEach(capture -> capture.setImage(ImageResizer.resize(capture.getImage(), outputDimensions)));
            frames.sort(Comparator.comparingInt(Frame::getTimeSinceStart));

            final String filePath = generateOutputFilePath();
            try {
                boolean builtSuccessfully = new GifConverter(frames, filePath).process();
                if (!builtSuccessfully) {
                    logger.warn("Problem occurred while building gif. It could possibly still work; location: {}", filePath);
                } else {
                    logger.info("Successfully created a gif located at {}", filePath);
                }
            } catch (IOException e) {
                logger.error("Error constructing output stream to build gif to:", e);
            }

            // delete folder for gif
            if (!deleteFileDirectory(gifFolder)) {
                logger.warn("Could not delete folder {}, which contains the images for a gif. You should delete this manually in order to avoid errors in the future, but this could fix itself after another build.", gifFolder.getAbsolutePath());
            }
            logger.debug("Successfully built gif to {}", filePath);
        }
        logger.info("All gifs finished building.");
    }
    
}

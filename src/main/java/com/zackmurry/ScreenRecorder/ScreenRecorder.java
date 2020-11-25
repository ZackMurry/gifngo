package com.zackmurry.ScreenRecorder;

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
public class ScreenRecorder extends Thread {

    private boolean recording = false;

    private static final int SCREEN_WIDTH = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    private static final int SCREEN_HEIGHT = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    private static final Rectangle captureRect = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);

    private int framesPerSecond = 15;
    private int timeBetweenCapturesMs = 1000 / framesPerSecond;
    private final String captureFolderName = "captures";
    File capturesFolder = new File(captureFolderName);

    private Robot robot;
    private ArrayList<BufferedImage> captures = new ArrayList<>();

    public ScreenRecorder() {
        if (!capturesFolder.exists()) {
            if (!capturesFolder.mkdir()) {
                System.out.println("Error initializing: captures folder could not be created. This is likely because the folder already exists, but this is likely a bug of gifngo.");
            }
        } else {
            // if the folder for the images exists, clear the images
            clearCapturesFolder();
        }
    }

    // todo save images as files and then load them to convert to gif maybe
    public void run() {
        while (recording) {
            try {
                long startTime = System.currentTimeMillis();
                BufferedImage capture = robot.createScreenCapture(captureRect);
                captures.add(capture);
                long delay = timeBetweenCapturesMs - (System.currentTimeMillis() - startTime);
                if (delay > 0) {
                    // maybe do this https://stackoverflow.com/questions/54394042/java-how-to-avoid-using-thread-sleep-in-a-loop
                    Thread.sleep(delay);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveImageToCaptures(BufferedImage image, String fileName) throws IOException {
        ImageIO.write(image, "jpeg", new File(fileName));
    }

    public void startRecording() {
        if (recording) {
            System.out.println("ERROR: Cannot start recording while already recording.");
            return;
        }
        System.out.println("Recording...");
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                e.printStackTrace();
            }
        }
        recording = true;
        this.start();
    }

    public void stopRecording() {
        if (!recording) {
            System.out.println("ERROR: Cannot stop recording if not currently recording.");
            return;
        }
        System.out.println("Stopped recording.");
        recording = false;
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Building GIF...");

        GifBuilder gifBuilder;
        try {
            gifBuilder = new GifBuilder("testgif.gif");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Processing " + captures.size() + " captures...");
        gifBuilder
                .withFrameRate(framesPerSecond)
                .withFrames(captures.toArray(BufferedImage[]::new))
                .build();
        System.out.println("GIF made!");
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
            System.out.println("ERROR: Captures should not be null");
            return;
        }
        for (File f : captures) {
            f.delete();
        }
    }

    public void setTimeBetweenCapturesMs(int ms) {
        timeBetweenCapturesMs = ms;
    }

    public int getTimeBetweenCapturesMs() {
        return timeBetweenCapturesMs;
    }

    public boolean isRecording() {
        return recording;
    }
    
    public void setFramesPerSecond(int fps) {
        if (recording) {
            System.out.println("WARNING: Updating frames per second while recording will produce unexpected behavior.\n" +
                              "\tThe GIF encoder will only receive the frames per second at the last frame, meaning that all of the other frames will be played at the final framerate.");
        }
        framesPerSecond = fps;
        timeBetweenCapturesMs = 1000 / fps;
    }
    
    public int getFramesPerSecond() {
        return framesPerSecond;
    }
    
    /**
      * The time between captures is how long the program should wait between captures. This value is not a guarantee,
      * because some framerates could be infeasible for some computers.
      * 
      * @return time between captures in milliseconds
      */
    public int getTimeBetweenCaptures() {
        return timeBetweenCapturesMs;
    }
    
    /**
      * @param ms interval (in milliseconds) between captures
      */
    public void setTimeBetweenCaptures(int ms) {
        if (recording) {
            System.out.println("WARNING: Updating frames per second while recording will produce unexpected behavior.\n" +
                              "\tThe GIF encoder will only receive the frames per second at the last frame, meaning that all of the other frames will be played at the final framerate.");
        }
        timeBetweenCapturesMs = ms;
        framesPerSecond = 1000 / ms;
    }
    
}

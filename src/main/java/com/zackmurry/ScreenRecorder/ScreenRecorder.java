package com.zackmurry.ScreenRecorder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ScreenRecorder extends Thread {

    private static boolean recording = false;

    private static final int SCREEN_WIDTH = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    private static final int SCREEN_HEIGHT = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    private final Rectangle captureRect = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);

    private int framesPerSecond = 15;
    private int timeBetweenCapturesMs = 1000 / framesPerSecond;
    private final String captureFolderName = "captures";
    File capturesFolder = new File(captureFolderName);

    private Robot robot;
    private ArrayList<BufferedImage> captures = new ArrayList<>();

    public ScreenRecorder() {
        if (!capturesFolder.exists()) {
            System.out.println(capturesFolder.mkdir());
        } else {
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

    private void startRecording() {
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

    private void stopRecording() {
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

}

package com.zackmurry.gifngo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.ArrayList;

public class ScreenRecorder extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(ScreenRecorder.class);

    private final ArrayList<Frame> captures = new ArrayList<>();
    private boolean recording;

    private static final int SCREEN_WIDTH = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    private static final int SCREEN_HEIGHT = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    private static final Rectangle CAPTURE_RECT = new Rectangle(SCREEN_WIDTH, SCREEN_HEIGHT);
    private final int timeBetweenCapturesMs;
    private int msOffset;

    private Robot robot;

    public ScreenRecorder(int msOffset, int timeBetweenCapturesMs) {
        this.timeBetweenCapturesMs = timeBetweenCapturesMs;
        this.msOffset = msOffset;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    /**
     * assumes single-threaded if this constructor is called (records at the fps)
     * @param framesPerSecond to record at
     */
    public ScreenRecorder(int framesPerSecond) {
        this.timeBetweenCapturesMs = 1000 / framesPerSecond;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    // todo save images as files and then load them to convert to gif maybe
    public void run() {
        long recordStartTime = System.currentTimeMillis();
        if (msOffset > 0) {
            try {
                Thread.sleep(msOffset);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.debug("Thread {} starting recording.", super.getName());
        while (recording) {
            try {
                long startTime = System.currentTimeMillis();
                Frame capture = new Frame(robot.createScreenCapture(CAPTURE_RECT), (int) (System.currentTimeMillis() - recordStartTime));
                captures.add(capture);
                long delay = timeBetweenCapturesMs - (System.currentTimeMillis() - startTime);
                if (delay > 0) {
                    // todo maybe do this https://stackoverflow.com/questions/54394042/java-how-to-avoid-using-thread-sleep-in-a-loop
                    Thread.sleep(delay);
                } else if (delay < -5) {
                    logger.warn("Failure to keep up with frame rate: skipped {} milliseconds.", -delay);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void startRecording() {
        recording = true;
        this.start();
    }

    public ArrayList<Frame> stopRecording() {
        recording = false;
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return captures;
    }

}

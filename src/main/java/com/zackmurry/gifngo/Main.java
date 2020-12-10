package com.zackmurry.gifngo;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.tulskiy.keymaster.common.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        CommandLineArguments cla = new CommandLineArguments();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(cla)
                .build();
        jCommander.setProgramName("gifngo");
        jCommander.parse(args);
        if (cla.isHelp()) {
            jCommander.usage();
            return;
        }
        if (!cla.isDebug()) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.INFO);
        }
        ScreenRecorderManager recorder = new ScreenRecorderManager(cla.getThreadCount());
        recorder.setFramesPerSecond(cla.getFramesPerSecond());
        recorder.setRepeat(cla.getRepeat());
        recorder.setStrictFps(cla.getStrictFps());
        recorder.setSingleRecording(cla.isSingleRecording());


        System.out.println("fps: " + cla.getFramesPerSecond() + "; tc: " + cla.getThreadCount() + "; r: " + cla.getRepeat());

        Provider provider = Provider.getCurrentProvider(false);
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK, false), hotkey -> recorder.toggleRecording());
        logger.info("Listening for commands...");
    }

}

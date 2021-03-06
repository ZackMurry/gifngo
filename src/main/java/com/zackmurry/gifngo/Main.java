package com.zackmurry.gifngo;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.tulskiy.keymaster.common.Provider;
import com.zackmurry.gifngo.models.ImageDimension;
import com.zackmurry.gifngo.recorder.ScreenRecorderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // todo saving args and using like --args [save name] to load them
    public static void main(String[] args) {
        CommandLineArguments cla = new CommandLineArguments();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(cla)
                .build();
        jCommander.setProgramName("gifngo");
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            logger.error("Invalid parameter -- {}", e.getMessage());
            return;
        }
        if (!cla.isDebug()) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.INFO);
        }
        if (cla.isHelp()) {
            jCommander.usage();
            return;
        }
        if (cla.isInit()) {
            PathInitializer.initialize();
            return;
        }
        if (cla.isVersion()) {
            System.out.println("\ngifngo " + cla.getVersionNumber() + "\n");
            System.out.println("Created by Zack Murry");
            return;
        }
        ScreenRecorderManager recorder = new ScreenRecorderManager(cla.getThreadCount());
        recorder.setFramesPerSecond(cla.getFramesPerSecond());
        recorder.setRepeat(cla.getRepeat());
        recorder.setStrictFps(cla.getStrictFps());
        recorder.setSingleRecording(cla.isSingleRecording());
        
        try {
            ImageDimension outputDimensions = ImageDimension.fromString(cla.getResolution());
            recorder.setOutputDimensions(outputDimensions);
        } catch (IllegalArgumentException e) {
            logger.warn(e.getMessage());
            logger.info("Defaulting output resolution to {}.", Constants.DEFAULT_RESOLUTION);
        }

        KeyStroke key = CommandLineArguments.parseKey(cla.getKey());

        Provider provider = Provider.getCurrentProvider(false);
        provider.register(key, hotkey -> recorder.toggleRecording());

        if (!cla.getWaitForBuild().isEmpty()) {
            recorder.setWaitForBuild(true);
            KeyStroke buildKey = CommandLineArguments.parseKey(cla.getWaitForBuild());
            provider.register(buildKey, hotKey -> recorder.buildGifs());
        }

        logger.info("Listening for commands...");
    }

}

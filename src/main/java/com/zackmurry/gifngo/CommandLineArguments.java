package com.zackmurry.gifngo;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

// todo allow for using previous arguments

@Getter
@Setter
public class CommandLineArguments {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineArguments.class);

    @Parameter(names = {"--fps", "-f"}, description = "Frames per second to record at.")
    private int framesPerSecond = Constants.DEFAULT_FPS;

    @Parameter(names = {"--repeat", "-r"}, description = "Number of times to play the GIF. 0 means repeat infinitely. Greater than 0 means play r times.")
    private int repeat = 0;

    @Parameter(names = {"--threads", "-t"}, description = "Number of threads to record on.")
    private int threadCount = 2;

    @Parameter(names = {"--strict-fps", "-s"}, description = "Acceptable frame rate difference. The recording will fail if the fps is more than" +
            " s away from the desired fps. A value of zero will not give any warnings about fps. A negative value will act as a positive value," +
            " but instead give a warning instead of failing the recording.")
    private double strictFps = -3;

    @Parameter(names = {"--single-recording", "-sr"}, description = "Whether the program should end after processing a recording.")
    private boolean singleRecording = false;

    @Parameter(names = {"--help", "-h", "help"}, description = "List options.", help = true)
    private boolean help = false;

    @Parameter(names = {"--debug", "-d"}, description = "Use debug logging.")
    private boolean debug = false;

    @Parameter(names = {"--resolution", "-res"}, description = "Set the output resolution. Should be in the format WIDTHxHEIGHT, where WIDTH and HEIGHT are positive integers.")
    private String resolution = Constants.DEFAULT_RESOLUTION;

    @Parameter(names = {"--init", "-i", "init"}, description = "Automatically allow using a \"gifngo\" command anywhere by updating your path.")
    private boolean init = false;

    @Parameter(names = {"--key", "-k"}, description = "Sets the key used to start and end recording. Should be entered in the format [F-KEY]_[SHIFT|CONTROL|ALT|NONE]. F-KEY must be a value " +
            "from F1 to F24. For example, to use F9, you can set this value to \"F9\".")
    private String key = "F7_SHIFT";

    // todo add tests for this
    public static KeyStroke parseKey(String k) {
        final String[] parts = k.split("_");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Your key must be in the format [F-KEY]_[SHIFT|CONTROL|ALT|NONE]. Please type \"gifngo help\" to learn more. Your entry doesn't contain one underscore.");
        }
        final String fNumStr = parts[0].substring(1);
        int fNum;
        try {
            fNum = Integer.parseInt(fNumStr);
        } catch (NumberFormatException e) {
            logger.error("Your key must be an F-Key and should have an integer value between the 'F' and the underscore.");
            throw e;
        }
        if (fNum <= 0 || fNum > 24) {
            throw new IllegalArgumentException("Your f value must be between 1 and 24. Entered f value: " + fNum + ".");
        }

        // converting to the proper KeyEvent
        final int keyCode = fNum + (KeyEvent.VK_F1 - 1);

        int mask = 0;
        // todo use the bitwise OR to combine these, but I don't see the value to the user, and it'd make the input even harder to grasp
        if (parts[1].equalsIgnoreCase("SHIFT")) {
            mask = InputEvent.SHIFT_DOWN_MASK;
        } else if (parts[1].equalsIgnoreCase("CONTROL") || parts[1].equalsIgnoreCase("CTRL")) {
            mask = InputEvent.CTRL_DOWN_MASK;
        } else if (parts[1].equalsIgnoreCase("ALT")) {
            mask = InputEvent.ALT_DOWN_MASK;
        } else if (!parts[1].equalsIgnoreCase("NONE")) { // the mask value for NONE is just zero, so we don't need to do anything
            throw new IllegalArgumentException("Invalid key input: after the underscore, there should be either SHIFT, CONTROL, ALT, or NONE in order to specify a modifier.");
        }

        return KeyStroke.getKeyStroke(keyCode, mask, false);

    }

}

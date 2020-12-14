package com.zackmurry.gifngo;

import com.beust.jcommander.Parameter;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
@Setter
public class CommandLineArguments {

    @Parameter(names = {"--fps", "-f"}, description = "Frames per second to record at.")
    private int framesPerSecond = 18;

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

    @Parameter(names = {"--help", "-h"}, description = "List options.", help = true)
    private boolean help = false;

    @Parameter(names = {"--debug", "-d"}, description = "Use debug logging.")
    private boolean debug = false;

    @Parameter(names = {"--resolution", "-res"}, description = "Set the output resolution. Should be in the format WIDTHxHEIGHT, where WIDTH and HEIGHT are positive integers.")
    private String resolution = "960x540";

}

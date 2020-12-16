package com.zackmurry.gifngo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PathInitializer.class);

    private static final String GIFNGO_BIN_PATH = System.getProperty("user.home") + "\\gifngo\\bin";

    public static void initialize() {
        try {
            if (!createBatchFile()) {
                return;
            }
            String osName = System.getProperty("os.name");
            // todo support other OSs
            if (osName.startsWith("Windows")) {
                Runtime.getRuntime().exec("cmd /c \"setx PATH \\\"%PATH%;" + GIFNGO_BIN_PATH + "\\\"\"");
            } else {
                logger.error("Operating system not supported for this command. You'll have to do update your PATH to include " + GIFNGO_BIN_PATH + " manually.");
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static boolean createBatchFile() throws IOException {
        File binDir = new File(GIFNGO_BIN_PATH);
        if (!binDir.exists()) {
            logger.debug("Creating bin directory.");
            binDir.mkdirs();
        } else {
            logger.debug("Bin directory already exists.");
        }
        final File gifngoBatchFile = new File(GIFNGO_BIN_PATH + "\\gifngo.bat");
        if (!gifngoBatchFile.createNewFile()) {
            logger.warn("The batch file for gifngo already exists. Replacing...");
        }
        String path = PathInitializer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (!decodedPath.endsWith(".jar")) {
            logger.error("To initialize, you should run directly from the jar file. Expected location to end with .jar but instead is: {}", decodedPath);
            return false;
        }
        logger.debug("Path of .jar: {}", decodedPath);
        FileWriter fw = new FileWriter(gifngoBatchFile);
        // todo might want to change /k to /c
        fw.write("@ECHO OFF\n" +
                "cmd /k \"java -jar " + decodedPath + " %*\"");
        return true;
    }

}

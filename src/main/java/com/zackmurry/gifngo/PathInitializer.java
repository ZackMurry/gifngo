package com.zackmurry.gifngo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class PathInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PathInitializer.class);

    private static final String GIFNGO_BIN_PATH = System.getProperty("user.home") + "\\gifngo\\bin";

    /**
     * basically just takes response from a cmd command and prints it to the screen
     */
    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
        }
    }

    public static void initialize() {
        String currentPath = getPathWithPrevInfoErased();
        try {
            if (!createBatchFile()) {
                return;
            }
            String osName = System.getProperty("os.name");
            // todo support other OSs
            if (osName.startsWith("Windows")) {
                // todo might want to check if path already has /gifngo/bin in it
                final ProcessBuilder builder = new ProcessBuilder();
                // todo might not need the semicolon here
                builder.command("cmd.exe", "/c", "setx", "/M", "PATH", "\"" + currentPath + GIFNGO_BIN_PATH + "\"");
                final Process process = builder.start();
                StreamGobbler gobbler = new StreamGobbler(process.getInputStream(), System.out::println);
                gobbler.run();
                logger.info("Initialization finished. Please restart your computer to see if it has succeeded. If it failed, make sure that you ran this program in the administrator command prompt.");
            } else {
                logger.error("Operating system not supported for this command. You'll have to update your PATH to include " + GIFNGO_BIN_PATH + " manually.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String getPathWithPrevInfoErased() {
        String currentPath = System.getenv("Path");
        // backslash heaven incoming
        final String escapedBinPath = GIFNGO_BIN_PATH.replaceAll("\\\\", "\\\\\\\\");
        currentPath = currentPath.replaceAll(";" + escapedBinPath, "");

        // fixing bad semicolons in path
        if (currentPath.contains(";;")) {
            logger.debug("Path contains double-semicolons. Fixing...");
            StringBuilder builder = new StringBuilder(currentPath);
            for (int i = 0; i < builder.length() - 1; i++) {
                if (builder.charAt(i) == ';' && builder.charAt(i + 1) == ';') {
                    builder.delete(i, i + 1);
                    i--;
                }
            }
            currentPath = builder.toString();
        }

        logger.debug("Cleared path: {}", currentPath);
        return currentPath;
    }

    private static boolean createBatchFile() throws IOException {
        File binDir = new File(GIFNGO_BIN_PATH);

        if (binDir.exists()) {
            logger.debug("Bin directory already exists. Deleting...");
            File[] binFiles = binDir.listFiles();
            if (binFiles != null) {
                for (File binFile : binFiles) {
                    if (!binFile.delete()) {
                        logger.warn("Error deleting file in bin: {}", binFile.getAbsolutePath());
                    }
                }
            }
            if (!binDir.delete()) {
                logger.error("Error deleting bin directory. Aborting...");
                return false;
            }
        }
        logger.debug("Creating bin directory.");
        if (!binDir.mkdirs()) {
            logger.error("Error creating bin directory.");
            return false;
        }

        final File gifngoBatchFile = new File(GIFNGO_BIN_PATH + "\\gifngo.bat");
        if (!gifngoBatchFile.createNewFile()) {
            logger.warn("The batch file for gifngo already exists. Replacing...");
        }
        final String path = PathInitializer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);
        if (!decodedPath.endsWith(".jar")) {
            logger.error("To initialize, you should run directly from the jar file. Expected location to end with .jar but instead is: {}", decodedPath);
            return false;
        }
        if (decodedPath.startsWith("/") || decodedPath.startsWith("\\")) {
            decodedPath = decodedPath.substring(1);
        }
        logger.debug("Path of .jar: {}", decodedPath);
        final FileWriter fw = new FileWriter(gifngoBatchFile);
        // todo might want to change /k to /c
        fw.write("@ECHO OFF\n" +
                "cmd /k \"java -jar " + decodedPath + " %*\"");
        fw.close();
        return true;
    }

}

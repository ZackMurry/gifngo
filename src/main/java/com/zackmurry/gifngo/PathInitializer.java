package com.zackmurry.gifngo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.function.Consumer;

public class PathInitializer {

    private static final Logger logger = LoggerFactory.getLogger(PathInitializer.class);

    private static final String GIFNGO_BIN_PATH_WINDOWS = System.getProperty("user.home") + "\\.gifngo\\bin";

    private static final String LINUX_USERNAME = System.getenv("SUDO_USER");
    private static final String GIFNGO_BIN_PATH_LINUX = "/home/" + (LINUX_USERNAME != null ? LINUX_USERNAME : "//") + "/.gifngo/bin";

    private static final String OLD_GIFNGO_BIN_PATH = System.getProperty("user.home") + "\\gifngo\\bin";

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
        try {
            String osName = System.getProperty("os.name");
            if (osName.startsWith("Windows")) {
                if (!createBatchFile()) {
                    return;
                }
                final ProcessBuilder builder = new ProcessBuilder();
                final String currentPath = getPathWithPrevInfoErased();
                builder.command("cmd.exe", "/c", "setx", "/M", "PATH", "\"" + currentPath + GIFNGO_BIN_PATH_WINDOWS + "\"");
                final Process process = builder.start();
                StreamGobbler gobbler = new StreamGobbler(process.getInputStream(), System.out::println);
                gobbler.run();
                logger.info("Initialization finished. Please restart your computer to see if it has succeeded. If it failed, make sure that you ran this program in the administrator command prompt.");
            } else if (osName.startsWith("Linux")) {
                if (LINUX_USERNAME == null) {
                    logger.error("You need to use `sudo` to run this command");
                    return;
                }
                if (!generateProfileDFile()) {
                    return;
                }
                logger.info("Created /etc/profile.d/gifngo.sh");
                if (createBashExecutable()) {
                    logger.info("Successfully added gifngo to PATH. Please log out of your computer for the changes to take effect.");
                }
            } else {
                logger.error("Operating system not supported for this command. You'll have to update your PATH to include " + GIFNGO_BIN_PATH_LINUX + " manually.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static boolean generateProfileDFile() throws IOException {
        final File shFile = new File("/etc/profile.d/gifngo.sh");
        if (shFile.exists()) {
            if (!shFile.delete()) {
                logger.error("Error accessing {} -- use `sudo` mode", shFile.getAbsolutePath());
            } else {
                logger.debug("Deleted file from previous configuration: {}", shFile.getAbsolutePath());
            }
        }
        try {
            if (!shFile.createNewFile()) {
                logger.error("Error creating {} -- make sure you are in sudo mode", shFile.getAbsolutePath());
                return false;
            }
        } catch (SecurityException | IOException e) {
            logger.error("Error accessing {} -- use `sudo` mode", shFile.getAbsolutePath());
            return false;
        }

        final FileWriter fw = new FileWriter(shFile.getAbsolutePath());
        fw.write("export PATH=${PATH}:" + GIFNGO_BIN_PATH_LINUX);
        fw.flush();
        fw.close();
        return true;
    }

    // todo doesn't work when a space is in the name
    private static boolean createBashExecutable() throws IOException {
        final File execFile = new File(GIFNGO_BIN_PATH_LINUX + "/gifngo");
        if (execFile.exists()) {
            if (!execFile.delete()) {
                logger.error("Error deleting {} -- make sure you are in `sudo` mode", execFile.getAbsolutePath());
                return false;
            } else {
                logger.debug("Deleted file from previous configuration: {}", execFile.getAbsolutePath());
            }
        }
        final File binFile = new File(GIFNGO_BIN_PATH_LINUX);
        if (binFile.exists()) {
            final File[] children = binFile.listFiles();
            if (children == null) {
                logger.error("Error deleting files in /bin");
                return false;
            }
            for (File child : children) {
                if (child.isFile()) {
                    if (!child.delete()) {
                        logger.warn("Unable to delete {} -- this may cause a conflict in your path", child.getAbsolutePath());
                    }
                } else {
                    logger.error("Encountered directory in {}", binFile.getAbsolutePath());
                    return false;
                }
            }
            if (!binFile.delete()) {
                logger.error("Unable to delete previous bin file -- location: {}", binFile.getAbsolutePath());
                return false;
            }
            logger.debug("Deleted bin file from previous initialization");
        }
        Set<PosixFilePermission> openToUserPermissions = PosixFilePermissions.fromString("rwxrwxrwx");
        FileAttribute<?> permissions = PosixFilePermissions.asFileAttribute(openToUserPermissions);
        try {
            Files.createDirectories(binFile.toPath(), permissions);
            Files.createFile(execFile.toPath(), permissions);
        } catch (SecurityException | IOException | InvalidPathException e) {
            logger.error("Error creating {} -- {}", execFile.getAbsolutePath(), e.getMessage());
            e.printStackTrace();
            return false;
        }

        final String encodedPathToJar = PathInitializer.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String pathToJar = URLDecoder.decode(encodedPathToJar, StandardCharsets.UTF_8);
        if (!pathToJar.endsWith(".jar")) {
            logger.error("To initialize, you should run directly from the jar file. Expected location to end with .jar but instead is: {}", pathToJar);
            return false;
        }
        logger.debug("Path to jar: {}", pathToJar);
        final FileWriter fw = new FileWriter(execFile.getAbsolutePath());
        final String bashString = "#!/usr/bin/env bash\njava -jar \"" + pathToJar + "\" \"$@\"";
        fw.write(bashString);
        fw.flush();
        fw.close();
        return true;
    }

    public static String getPathWithPrevInfoErased() {
        String currentPath = System.getenv("Path");
        // backslash heaven incoming
        final String escapedBinPath = GIFNGO_BIN_PATH_WINDOWS.replaceAll("\\\\", "\\\\\\\\");
        currentPath = currentPath.replaceAll(";" + escapedBinPath, "");
        final String oldEscapedBinPath = OLD_GIFNGO_BIN_PATH.replaceAll("\\\\", "\\\\\\\\");
        currentPath = currentPath.replaceAll(";" + oldEscapedBinPath, "");


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
        final File binDir = new File(GIFNGO_BIN_PATH_WINDOWS);

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
            logger.warn("Problem creating bin directory.");
        }

        final File gifngoBatchFile = new File(GIFNGO_BIN_PATH_WINDOWS + "\\gifngo.bat");
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

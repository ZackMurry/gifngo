![gifngo – A tool to create gifs in the command line](banner.svg)

# About
Gifngo is a lightweight screen recorder that outputs gifs. It is run through the command line and starts/stops recording with a hotkey.

# Installation
First, download the .jar file from the latest release. If you don't have Java installed, you'll need to download it and install it.  

## Initialization
For the best experience, it's recommended to add `gifngo` to your PATH variable.

### Windows
If you're on Windows, you can easily do this by using the `init` option whenever you run the .jar file in an administrator command prompt.
You can run the .jar file by navigating to the directory (in an administrator command prompt) which you installed gifngo to and running `java -jar [name of jar] init`.
This creates a .bat file which contains a command that runs the .jar file. It then adds the .bat file to your PATH.
Because this command modifies your PATH, you'll need to restart your computer to notice a change.

Once you've restarted your computer, you can open any command prompt and (hopefully) you will be able to type `gifngo help` to see a list of commands.

### Linux
To initialize on Linux, run `sudo java -jar [path to jar] init`. Then, log out of your computer and log back in (to refresh your path). Run `gifngo help` to see a list of commands.

This will automatically erase the configuration done by previous installations, so you should be able to do this without issue.

# Usage
To start recording a gif, run `gifngo` and press ALT and F7 on your keyboard at the same time when you are ready to start recording. Press ALT and F7 again to stop the recording and save it to your downloads folder.

## Configuration
You can pass arguments into gifngo by appending them to the end of the command which you use to start it. For example, you can type `gifngo help` to see a list of options.  

### Help
To view the list of commands, you can use the option `help`.

### Version
This command prints the version of gifngo that is being called.

### Frame rate
You can set the frame rate to record at by using the option `--fps [value]` or `-f [value]`. The default value is 18.

### Change activation key
In order to change the hotkey which starts recording, you can use the option `--key [value]` or `-k [value]`, where `[value]` is in the format `KEY_MODIFIER`. The `KEY` should be one of the function keys, e.g. F7. The `MODIFIER` value can be `SHIFT`, `CONTROL`, `ALT`, or `NONE`. The default value is `F7_ALT`.

### Wait before build
If your computer slows down significantly during the building of the gifs, you may want to delay the building of the gifs until a good time.
You can use the option `--wait-for-build [value]` or `-w [value]` to wait until you press a specific key before the gif-building process starts.
`[value]` should represent the hotkey that will start the building of the gif. This should be one of the function keys, like F8, and be in the format described in the change activation key option.
The default behavior is to build gifs immediately after recording stops.

### Resolution
To change the output resolution, use the option `--resolution [value]` or `-res [value]`, where `[value]` is in the format of `WIDTHxHEIGHT`, with `WIDTH` and `HEIGHT` being positive integers. For best results, this should be in the same aspect ratio as your monitor. The default output resolution is 480x270.

### Repeat
You can set the behavior of the gif after it is done playing by using the option `--repeat [value]` or `-r [value]`. A value of 0 will play the gif infinitely many times, which is the default. A value greater than zero will play the gif that many times when played.

### Thread count
If you notice that the program is running too slow, you can up the number of threads to compensate by using the option `--threads [value]` or `-t [value]`. The default number of threads is two.

### Strict fps
If you'd like to enforce a certain number of frames per second, you can set how far away from your target frame rate is unacceptable by using the option `--strict-fps [value]` or `-s [value]`. A value of zero gives no warnings about frame rate. A negative value will warn you if the frame rate is more than 'value' away from your target frame rate. A positive value will cancel the building of the gif if the frame rate is unacceptable (more than 'value' away from the target). The default value is -3.

### Single recording
If you'd like to end the program after recording one gif, you can use the option `--single-recording` or `-sr`.

### Debug
If you'd like to enable debug mode, you can use the option `--debug` or `-d`.

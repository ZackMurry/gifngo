# gifngo
 A lightweight Java screen recording application that allows exporting to gif.

# installation
First, download the .jar file from the latest release. If you don't have Java installed, you'll need to download it and install it.  
In the command prompt, navigate to the directory that contains the .jar file and run `java -jar [name of jar]`, where `name of jar` is simply the name of the .jar file.

# usage
To start recording a gif, press SHIFT and F7 on your keyboard at the same time. Press SHIFT and F7 again to stop the recording and save it to your downloads folder.

# configuration
You can pass arguments into gifngo by appending them to the end of the command you use to start it. For example, you can type `java -jar [name of jar] help` to see a list of options.  

## options

### frame rate
You can set the frame rate to record at by using the option `--fps [value]` or `-f [value]`. The default value is 24.

### repeat
You can set the behavior of the gif after it is done playing by using the option `--repeat [value]` or `-r [value]`. A value of 0 will play the gif infinitely many times, which is the default. A value greater than zero will play the gif that many times when played.

### thread count
If you notice that the program is running too slow, you can up the number of threads to compensate by using the option `--threads [value]` or `-t [value]`. The default number of threads is two.

### strict fps
If you'd like to enforce a certain number of frames per second, you can set how far away from your target frame rate is unacceptable by using the option `--strict-fps [value]` or `-s [value]`. A value of zero gives no warnings about frame rate. A negative value will warn you if the frame rate is more than 'value' away from your target frame rate. A positive value will cancel the building of the gif if the frame rate is unacceptable (more than 'value' away from the target). The default value is -3.

### single recording
If you'd like to end the program after recording one gif, you can use the option `--single-recording` or `-sr`.

### help
If you'd like a list of commands, you can use the option `--help` or `-h`.

### debug
If you'd like to enable debug mode, you can use the option `--debug` or `-d`.

### resolution
To change the output resolution, use the option `--resolution [value]` or `-res [value]`, where `[value]` is in the format of `WIDTHxHEIGHT`, with `WIDTH` and `HEIGHT` being positive integers. 

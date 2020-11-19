package com.zackmurry.ScreenRecorder;

import com.tulskiy.keymaster.common.Provider;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class Main {

    private static ScreenRecorder recorder;

    public static void main(String[] args) {
        recorder = new ScreenRecorder();
        Provider provider = Provider.getCurrentProvider(false);
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK, false), hotkey -> recorder.toggleRecording());
        System.out.println("Listening for commands...");
    }

}

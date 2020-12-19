package com.zackmurry.gifngo;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestCommandLineArguments {

    @Test
    public void testParseKey() {
        final KeyStroke expectedF7Shift = KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.SHIFT_DOWN_MASK, false);
        assertEquals(expectedF7Shift, CommandLineArguments.parseKey("F7_SHIFT"), "Key parser should parse F7_SHIFT correctly.");

        final KeyStroke expectedF7Alt = KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.ALT_DOWN_MASK, false);
        assertEquals(expectedF7Alt, CommandLineArguments.parseKey("F7_ALT"), "Key parser should parse ALT correctly.");

        final KeyStroke expectedF7Control = KeyStroke.getKeyStroke(KeyEvent.VK_F7, InputEvent.CTRL_DOWN_MASK, false);
        assertEquals(expectedF7Control, CommandLineArguments.parseKey("F7_CONTROL"), "Key parser should parse CONTROL correctly.");
        assertEquals(expectedF7Control, CommandLineArguments.parseKey("F7_CTRL"), "Key parser should parse CTRL correctly.");

        final KeyStroke expectedF19Control = KeyStroke.getKeyStroke(KeyEvent.VK_F19, InputEvent.CTRL_DOWN_MASK, false);
        assertEquals(expectedF19Control, CommandLineArguments.parseKey("F19_CONTROL"), "Key parser should parse double-digit function keys correctly.");

        final KeyStroke expectedF4None = KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0, false);
        assertEquals(expectedF4None, CommandLineArguments.parseKey("F4_NONE"));

        assertThrows(NumberFormatException.class, () -> CommandLineArguments.parseKey("FN_CONTROL"), "Key parser should throw a NumberFormatException if given an invalid F number.");
        assertThrows(IllegalArgumentException.class, () -> CommandLineArguments.parseKey("F7SHIFT"), "Key parser should require an underscore");
        assertThrows(IllegalArgumentException.class, () -> CommandLineArguments.parseKey("F7_SHIFT_"), "Key parser should require one and only one underscore.");
        assertThrows(IllegalArgumentException.class, () -> CommandLineArguments.parseKey("F7_RANDOM"), "Key parser should require a valid mask.");
        assertThrows(IllegalArgumentException.class, () -> CommandLineArguments.parseKey("F0_RANDOM"), "Key parser should require an F value greater than zero.");
        assertThrows(IllegalArgumentException.class, () -> CommandLineArguments.parseKey("F25_RANDOM"), "Key parser should require an F value less than 25.");

    }

}

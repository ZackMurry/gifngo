package com.zackmurry.gifngo.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ImageDimension {

    private int width;
    private int height;

    public static ImageDimension fromString(String resolution) throws IllegalArgumentException {
        String widthStr = "";
        String heightStr = "";
        for (int i = 1; i < resolution.length() - 1; i++) {
            char c = resolution.charAt(i);
            if (c == 'x') {
                widthStr = resolution.substring(0, i);
                heightStr = resolution.substring(i + 1);
            }
        }
        if (widthStr.isEmpty() || heightStr.isEmpty()) {
            throw new IllegalArgumentException("Output dimension should be formatted like WIDTHxHEIGHT");
        }

        int width, height;

        try {
            width = Integer.parseInt(widthStr);
            height = Integer.parseInt(heightStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Output dimension should contain two integers separated by an 'x'");
        }

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Output dimension should contain two positive integers separated by an 'x'");
        }

        return new ImageDimension(width, height);
    }

}

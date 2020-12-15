package com.zackmurry.gifngo.models;

import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;

@Getter
@Setter
public class Frame {

    private BufferedImage image;
    private int timeSinceStart;

    public Frame(BufferedImage image, int timeSinceStart) {
        this.image = image;
        this.timeSinceStart = timeSinceStart;
    }

}

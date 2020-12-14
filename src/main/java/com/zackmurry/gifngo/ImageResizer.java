package com.zackmurry.gifngo;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * resizes images to the size provided.
 * this is done to reduce output file size, but it also makes image[] to gif conversion quicker
 */
public class ImageResizer {

    public static BufferedImage resize(BufferedImage image, int width, int height) {
        Image resizedImg = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = bufferedImage.createGraphics();
        g2d.drawImage(resizedImg, 0, 0, null);
        g2d.dispose();

        return bufferedImage;
    }

    public static BufferedImage resize(BufferedImage image, ImageDimension dimension) {
        return resize(image, dimension.getWidth(), dimension.getHeight());
    }

}

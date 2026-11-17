package com.example.mobilecellnetwork.gui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

final class ProfilePhotoImage {
    private static final int SIZE = 256;
    private static final int EDGE_SAMPLES = 4;

    private ProfilePhotoImage() {
    }

    static BufferedImage create(byte[] bytes) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
        if (source == null) {
            throw new IOException("Unable to decode the profile photo.");
        }
        int side = Math.min(source.getWidth(), source.getHeight());
        BufferedImage image = source.getSubimage((source.getWidth() - side) / 2,
                (source.getHeight() - side) / 2, side, side);
        while (image.getWidth() != SIZE) {
            int next = image.getWidth() > SIZE ? Math.max(SIZE, image.getWidth() / 2) : SIZE;
            BufferedImage scaled = new BufferedImage(next, next, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scaled.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(image, 0, 0, next, next, null);
            } finally {
                graphics.dispose();
            }
            image = scaled;
        }

        // Copy even if the source was already 256 pixels: getSubimage shares its raster.
        BufferedImage result = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        double radius = SIZE / 2.0;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int inside = 0;
                for (int sy = 0; sy < EDGE_SAMPLES; sy++) {
                    for (int sx = 0; sx < EDGE_SAMPLES; sx++) {
                        double dx = x + (sx + 0.5) / EDGE_SAMPLES - radius;
                        double dy = y + (sy + 0.5) / EDGE_SAMPLES - radius;
                        if (dx * dx + dy * dy <= radius * radius) {
                            inside++;
                        }
                    }
                }
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) * inside / (EDGE_SAMPLES * EDGE_SAMPLES);
                result.setRGB(x, y, (alpha << 24) | (argb & 0x00FFFFFF));
            }
        }
        return result;
    }
}

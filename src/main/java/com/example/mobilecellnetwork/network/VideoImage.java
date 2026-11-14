package com.example.mobilecellnetwork.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

public final class VideoImage {
    private VideoImage() {
    }

    public static void validate(byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > VideoCallPacket.MAX_BYTES) {
            throw new IOException("Invalid video frame size");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new IOException("Invalid video frame");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported video frame");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (!reader.getFormatName().equalsIgnoreCase("jpeg")
                        || width <= 0 || height <= 0 || width > 240 || height > 240) {
                    throw new IOException("Invalid video format or dimensions");
                }
            } finally {
                reader.dispose();
            }
        }
    }
}

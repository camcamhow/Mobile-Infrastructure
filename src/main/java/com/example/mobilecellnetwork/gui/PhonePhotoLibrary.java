package com.example.mobilecellnetwork.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class PhonePhotoLibrary {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, PhotoTexture> TEXTURES =
            java.util.Collections.synchronizedMap(new LinkedHashMap<>(32, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, PhotoTexture> eldest) {
                    if (size() > 32) {
                        Minecraft.getInstance().getTextureManager().release(eldest.getValue().location());
                        return true;
                    }
                    return false;
                }
            });
    public static final int MAX_IMAGE_BYTES = 1_000_000;
    static final int MAX_IMAGE_PIXELS = 16_000_000;
    private static final Path GALLERY_ROOT = Minecraft.getInstance().gameDirectory.toPath()
            .resolve("mobilecellnetwork").resolve("gallery");
    private static volatile UUID activePhoneId;

    private PhonePhotoLibrary() {
    }

    public static void setActivePhoneId(UUID phoneId) {
        activePhoneId = phoneId;
    }

    public static Photo importImage(Path source) throws IOException {
        long size = Files.size(source);
        if (size <= 0 || size > MAX_IMAGE_BYTES) {
            throw new IOException("Images must be smaller than 1 MiB.");
        }
        if (!Files.isRegularFile(source)) {
            throw new IOException("Select a PNG or JPEG image file.");
        }
        byte[] bytes = Files.readAllBytes(source);
        validateImage(bytes);
        return save(bytes, source.getFileName().toString());
    }

    public static Photo saveCapturedImage(byte[] bytes) throws IOException {
        validateImage(bytes);
        return save(bytes, "Camera_" + System.currentTimeMillis() + ".jpg");
    }

    static byte[] encodeCameraFrame(int width, int height, java.nio.ByteBuffer pixels) throws IOException {
        if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
            throw new IOException("The current game resolution is too large to capture.");
        }
        int outputWidth = Math.min(width, 640);
        int outputHeight = Math.max(1, (int) ((long) height * outputWidth / width));
        BufferedImage capture = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < outputHeight; y++) {
            int sourceY = (int) ((long) y * height / outputHeight);
            for (int x = 0; x < outputWidth; x++) {
                int sourceX = (int) ((long) x * width / outputWidth);
                int offset = (sourceX + (height - sourceY - 1) * width) * 4;
                int red = pixels.get(offset) & 0xFF;
                int green = pixels.get(offset + 1) & 0xFF;
                int blue = pixels.get(offset + 2) & 0xFF;
                capture.setRGB(x, y, (red << 16) | (green << 8) | blue);
            }
        }
        byte[] jpg = encodeJpeg(capture);
        if (jpg.length > MAX_IMAGE_BYTES) {
            throw new IOException("Could not compress the camera frame below 1 MiB.");
        }
        return jpg;
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        try {
            for (float quality = 0.82F; quality >= 0.42F; quality -= 0.1F) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
                    writer.setOutput(output);
                    ImageWriteParam options = writer.getDefaultWriteParam();
                    options.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    options.setCompressionQuality(quality);
                    writer.write(null, new javax.imageio.IIOImage(image, null, null), options);
                }
                byte[] encoded = bytes.toByteArray();
                if (encoded.length <= MAX_IMAGE_BYTES) {
                    return encoded;
                }
                image = resize(image, 0.75D);
            }
        } finally {
            writer.dispose();
        }
        throw new IOException("Could not compress the screenshot below 1 MiB.");
    }

    private static BufferedImage resize(BufferedImage input, double ratio) {
        int width = Math.max(1, (int) (input.getWidth() * ratio));
        int height = Math.max(1, (int) (input.getHeight() * ratio));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(input, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    public static List<Photo> photos() throws IOException {
        Path gallery = galleryPath();
        Files.createDirectories(gallery);
        try (var paths = Files.list(gallery)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                        return name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg");
                    })
                    .sorted(Comparator.comparingLong(PhonePhotoLibrary::modified).reversed())
                    .map(PhonePhotoLibrary::readPhoto)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
    }

    public static Optional<Photo> newestPhoto() throws IOException {
        List<Photo> photos = photos();
        return photos.isEmpty() ? Optional.empty() : Optional.of(photos.get(0));
    }

    private static Photo readPhoto(Path path) {
        try {
            long size = Files.size(path);
            if (size <= 0 || size > MAX_IMAGE_BYTES) {
                return null;
            }
            byte[] bytes = Files.readAllBytes(path);
            validateImage(bytes);
            return new Photo(path.getFileName().toString(), bytes);
        } catch (IOException exception) {
            LOGGER.warn("Skipping unreadable photo in the local phone gallery: {}", path, exception);
            return null;
        }
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            LOGGER.warn("Unable to read the modification time of a gallery photo: {}", path, exception);
            return 0L;
        }
    }

    private static Photo save(byte[] bytes, String name) throws IOException {
        Path gallery = galleryPath();
        Files.createDirectories(gallery);
        String safeName = Path.of(name).getFileName().toString().replaceAll("[^A-Za-z0-9_.-]", "_");
        String prefix = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_";
        int maxNameLength = 128 - prefix.length();
        if (safeName.length() > maxNameLength) {
            int extension = safeName.lastIndexOf('.');
            String suffix = extension >= 0 && safeName.length() - extension <= 6
                    ? safeName.substring(extension) : "";
            safeName = safeName.substring(0, maxNameLength - suffix.length()) + suffix;
        }
        String fileName = prefix + safeName;
        Path destination = gallery.resolve(fileName);
        Files.write(destination, bytes);
        if (activePhoneId != null && bytes.length <= MAX_IMAGE_BYTES) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                    new com.example.mobilecellnetwork.network.PhonePhotoUploadPacket(activePhoneId, fileName, bytes));
        }
        return new Photo(fileName, bytes);
    }

    public static void replacePhonePhotos(UUID phoneId, List<String> names) {
        if (phoneId == null) {
            return;
        }
        Path gallery = galleryPath(phoneId);
        try {
            Files.createDirectories(gallery);
            try (var paths = Files.list(gallery)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    Files.delete(path);
                }
            }
        } catch (IOException exception) {
            LOGGER.warn("Unable to synchronize the phone photo gallery.", exception);
        }
    }

    public static void saveRemotePhoto(UUID phoneId, String name, byte[] bytes) {
        if (phoneId == null || name == null || !name.matches("[A-Za-z0-9_.-]{1,128}")
                || bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            return;
        }
        try {
            validateImage(bytes);
            Path gallery = galleryPath(phoneId);
            Files.createDirectories(gallery);
            Files.write(gallery.resolve(name), bytes);
        } catch (IOException exception) {
            LOGGER.warn("Unable to save a synchronized phone photo.", exception);
        }
    }

    public static void removeRemotePhoto(UUID phoneId, String name) {
        if (phoneId == null || name == null || !name.matches("[A-Za-z0-9_.-]{1,128}")) {
            LOGGER.warn("Received an invalid phone photo deletion.");
            return;
        }
        try {
            Files.deleteIfExists(galleryPath(phoneId).resolve(name));
            var client = Minecraft.getInstance();
            if (phoneId.equals(activePhoneId) && client.screen instanceof GalleryScreen gallery) {
                gallery.photoDeleted(name);
            }
        } catch (IOException exception) {
            LOGGER.warn("Unable to remove the local copy of a deleted phone photo.", exception);
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Photo deleted from phone, but its local copy could not be removed."), false);
            }
        }
    }

    private static Path galleryPath() {
        return activePhoneId == null ? GALLERY_ROOT : galleryPath(activePhoneId);
    }

    private static Path galleryPath(UUID phoneId) {
        return GALLERY_ROOT.resolve("devices").resolve(phoneId.toString());
    }

    public static void validateImage(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new IOException("Images must be smaller than 1 MiB.");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw new IOException("The selected file is not a supported PNG or JPEG image.");
            }
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("The selected file is not a supported PNG or JPEG image.");
            }
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IOException("Image dimensions exceed the supported limit.");
                }
            } finally {
                reader.dispose();
            }
        }
    }

    public static void drawThumbnail(net.minecraft.client.gui.GuiGraphics graphics, byte[] bytes,
                                     int x, int y, int width, int height) {
        drawThumbnail(graphics, Integer.toHexString(java.util.Arrays.hashCode(bytes)),
                bytes, x, y, width, height);
    }

    public static void drawThumbnail(net.minecraft.client.gui.GuiGraphics graphics, String cacheKey,
                                     byte[] bytes, int x, int y, int width, int height) {
        String key = cacheKey + ":" + Integer.toHexString(java.util.Arrays.hashCode(bytes));
        drawThumbnailWithKey(graphics, key, bytes, x, y, width, height);
    }

    public static void drawVersionedThumbnail(net.minecraft.client.gui.GuiGraphics graphics, String cacheKey,
                                              byte[] bytes, int x, int y, int width, int height) {
        drawThumbnailWithKey(graphics, cacheKey, bytes, x, y, width, height);
    }

    public static boolean drawProfilePhoto(net.minecraft.client.gui.GuiGraphics graphics, String cacheKey,
                                           byte[] bytes, int x, int y, int width, int height) {
        String key = "profile-circle:" + cacheKey + ":" + Integer.toHexString(java.util.Arrays.hashCode(bytes));
        PhotoTexture texture = TEXTURES.get(key);
        if (texture == null) {
            texture = uploadProfileTexture(bytes);
            if (texture == null) {
                return false;
            }
            TEXTURES.put(key, texture);
        }
        graphics.blit(texture.location(), x, y, 0.0F, 0.0F, width, height, width, height);
        return true;
    }

    private static void drawThumbnailWithKey(net.minecraft.client.gui.GuiGraphics graphics, String key,
                                             byte[] bytes, int x, int y, int width, int height) {
        PhotoTexture texture = TEXTURES.get(key);
        if (texture == null) {
            texture = uploadTexture(key, bytes);
            if (texture == null) {
                return;
            }
            TEXTURES.put(key, texture);
        }
        double scale = Math.min((double) width / texture.width(), (double) height / texture.height());
        int drawWidth = Math.max(1, (int) Math.round(texture.width() * scale));
        int drawHeight = Math.max(1, (int) Math.round(texture.height() * scale));
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        graphics.blit(texture.location(), drawX, drawY, 0.0F, 0.0F,
                drawWidth, drawHeight, drawWidth, drawHeight);
    }

    private static NativeImage decodeImage(byte[] bytes) throws IOException {
        boolean png = bytes.length > 4 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N';
        if (png) {
            return NativeImage.read(new ByteArrayInputStream(bytes));
        }
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
        if (decoded == null) {
            throw new IOException("Unsupported image format.");
        }
        NativeImage image = new NativeImage(decoded.getWidth(), decoded.getHeight(), false);
        for (int y = 0; y < decoded.getHeight(); y++) {
            for (int x = 0; x < decoded.getWidth(); x++) {
                int argb = decoded.getRGB(x, y);
                int abgr = 0xFF000000 | ((argb & 0xFF) << 16) | (argb & 0xFF00) | ((argb >> 16) & 0xFF);
                image.setPixelRGBA(x, y, abgr);
            }
        }
        return image;
    }

    private static PhotoTexture uploadTexture(String key, byte[] bytes) {
        RenderSystem.assertOnRenderThread();
        try {
            NativeImage image = decodeImage(bytes);
            int width = image.getWidth();
            int height = image.getHeight();
            DynamicTexture dynamicTexture = new DynamicTexture(image);
            dynamicTexture.setFilter(true, false);
            ResourceLocation location = Minecraft.getInstance().getTextureManager()
                    .register("mobilecellnetwork/photos/" + java.util.UUID.randomUUID(), dynamicTexture);
            return new PhotoTexture(location, width, height);
        } catch (IOException exception) {
            LOGGER.warn("Unable to upload a photo texture for the phone UI.", exception);
            return null;
        }
    }

    private static PhotoTexture uploadProfileTexture(byte[] bytes) {
        RenderSystem.assertOnRenderThread();
        try {
            validateImage(bytes);
            BufferedImage image = ProfilePhotoImage.create(bytes);
            int width = image.getWidth();
            int height = image.getHeight();
            NativeImage nativeImage = new NativeImage(width, height, false);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int argb = image.getRGB(x, y);
                    int abgr = (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >>> 16) & 0xFF);
                    nativeImage.setPixelRGBA(x, y, abgr);
                }
            }
            DynamicTexture texture = new DynamicTexture(nativeImage);
            texture.setFilter(true, false);
            ResourceLocation location = Minecraft.getInstance().getTextureManager()
                    .register("mobilecellnetwork/photos/" + UUID.randomUUID(), texture);
            return new PhotoTexture(location, width, height);
        } catch (IOException exception) {
            LOGGER.warn("Unable to create the phone profile photo.", exception);
            return null;
        }
    }

    private record PhotoTexture(ResourceLocation location, int width, int height) {
    }

    public record Photo(String name, byte[] bytes) {
        public Photo {
            bytes = bytes.clone();
        }

        @Override
        public byte[] bytes() {
            return bytes.clone();
        }

        byte[] bytesView() {
            return bytes;
        }
    }
}

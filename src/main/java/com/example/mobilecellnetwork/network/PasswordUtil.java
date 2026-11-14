package com.example.mobilecellnetwork.network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class PasswordUtil {
    private PasswordUtil() {
    }

    public static String generateSalt() {
        return UUID.randomUUID().toString();
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(((salt == null ? "" : salt) + (password == null ? "" : password))
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public static boolean verify(String password, String hash, String salt) {
        if (hash == null || hash.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(hash(password, salt).getBytes(StandardCharsets.UTF_8),
                hash.getBytes(StandardCharsets.UTF_8));
    }
}

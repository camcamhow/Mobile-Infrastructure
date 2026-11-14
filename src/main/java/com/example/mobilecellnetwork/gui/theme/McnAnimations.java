package com.example.mobilecellnetwork.gui.theme;

public final class McnAnimations {
    private McnAnimations() {
    }

    public static float linear(float t) {
        return clamp01(t);
    }

    public static float easeOut(float t) {
        float clamped = clamp01(t);
        float inverse = 1.0F - clamped;
        return clamp01(1.0F - inverse * inverse * inverse);
    }

    public static float easeIn(float t) {
        float clamped = clamp01(t);
        return clamp01(clamped * clamped * clamped);
    }

    public static float easeInOut(float t) {
        float clamped = clamp01(t);
        if (clamped < 0.5F) {
            return clamp01(4.0F * clamped * clamped * clamped);
        }
        float inverse = -2.0F * clamped + 2.0F;
        return clamp01(1.0F - inverse * inverse * inverse / 2.0F);
    }

    public static float spring(float t) {
        float clamped = clamp01(t);
        if (clamped == 0.0F || clamped == 1.0F) {
            return clamped;
        }
        return 1.0F - (float) (Math.pow(2.0D, -10.0D * clamped)
                * Math.cos(clamped * 10.0D - 0.75D) * (2.0D * Math.PI / 3.0D));
    }

    public static float lerp(float a, float b, float t) {
        float clamped = clamp01(t);
        return a + (b - a) * clamped;
    }

    public static int lerpColor(int fromColor, int toColor, float t) {
        float clamped = clamp01(t);
        int alpha = Math.round(lerp((fromColor >>> 24) & 0xFF, (toColor >>> 24) & 0xFF, clamped));
        int red = Math.round(lerp((fromColor >>> 16) & 0xFF, (toColor >>> 16) & 0xFF, clamped));
        int green = Math.round(lerp((fromColor >>> 8) & 0xFF, (toColor >>> 8) & 0xFF, clamped));
        int blue = Math.round(lerp(fromColor & 0xFF, toColor & 0xFF, clamped));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static float clamp01(float t) {
        return Math.max(0.0F, Math.min(1.0F, t));
    }
}

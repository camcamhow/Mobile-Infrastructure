package com.example.mobilecellnetwork.block;

public final class TowerDimensions {
    public static final float FOUNDATION_TOP = 5.0F / 16.0F;
    public static final float DECK_THICKNESS = 0.125F;
    public static final float RAIL_HEIGHT = 1.0F;

    private TowerDimensions() {}

    public static float mastHalfWidth(float y) {
        return 1.65F - (y - FOUNDATION_TOP) / (TestTowerBlock.HEIGHT - 0.6F - FOUNDATION_TOP);
    }
}

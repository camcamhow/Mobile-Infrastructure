package com.example.mobilecellnetwork.gui;

public record BoosterMenuLayout(int width, int height) {
    public static BoosterMenuLayout fit(int screenWidth, int screenHeight) {
        return new BoosterMenuLayout(Math.min(420, screenWidth - 12), Math.min(260, screenHeight - 12));
    }

    public int rowTop(int row) { return 100 + row * (rowHeight() + 2); }
    public int rowHeight() { return (footerY() - 106) / SignalBoosterMenu.PAGE_SIZE - 2; }
    public int footerY() { return height - 33; }
    public int navigationWidth() { return (width - 100) / 3; }
}

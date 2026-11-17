package com.example.mobilecellnetwork.gui;

public enum PhoneTab {
    KEYPAD("Keypad", "#"),
    RECENTS("Recents", "\u25F7"),
    CONTACTS("Contacts", "\u263A"),
    VOICEMAIL("Voicemail", "\u2709");

    final String label;
    final String glyph;

    PhoneTab(String label, String glyph) {
        this.label = label;
        this.glyph = glyph;
    }
}

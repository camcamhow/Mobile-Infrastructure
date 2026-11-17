package com.example.mobilecellnetwork.gui.admin;

import java.util.Optional;

public final class AdminUndoManager {
    private AdminUndoManager() {
    }

    private static UndoEntry undoEntry = null;

    private static class UndoEntry {
        final String description;
        final Runnable undoAction;
        final long createdTimeMillis;

        UndoEntry(String description, Runnable undoAction) {
            this.description = description;
            this.undoAction = undoAction;
            this.createdTimeMillis = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdTimeMillis >= AdminTheme.UNDO_DURATION_MS;
        }

        long getRemainingMillis() {
            return Math.max(0, AdminTheme.UNDO_DURATION_MS - (System.currentTimeMillis() - createdTimeMillis));
        }
    }

    public static void push(String description, Runnable undoAction) {
        undoEntry = new UndoEntry(description, undoAction);
    }

    public static boolean canUndo() {
        return undoEntry != null && !undoEntry.isExpired();
    }

    public static String getDescription() {
        return (undoEntry != null && !undoEntry.isExpired()) ? undoEntry.description : null;
    }

    public static void undo() {
        if (canUndo()) {
            undoEntry.undoAction.run();
            undoEntry = null;
        }
    }

    public static void clear() {
        undoEntry = null;
    }

    public static void tick() {
        if (undoEntry != null && undoEntry.isExpired()) {
            undoEntry = null;
        }
    }

    public static long getRemainingMillis() {
        if (undoEntry != null && !undoEntry.isExpired()) {
            return undoEntry.getRemainingMillis();
        }
        return 0;
    }
}
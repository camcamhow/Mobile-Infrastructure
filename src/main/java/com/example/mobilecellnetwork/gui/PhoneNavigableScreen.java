package com.example.mobilecellnetwork.gui;

public interface PhoneNavigableScreen {
    PhoneMenu phoneMenu();

    default void navigateBack() {
        PhoneNavigation.openHome(this);
    }

    default void navigateHome() {
        PhoneNavigation.openHome(this);
    }
}

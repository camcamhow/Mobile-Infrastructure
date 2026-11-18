package com.example.mobilecellnetwork.gui;

import com.example.mobilecellnetwork.registry.ModMenus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public final class ModScreens {
    private ModScreens() {
    }

    public static void register(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PHONE_MENU.get(), PhoneScreen::new);
        event.register(ModMenus.ANTENNA_MENU.get(), AntennaScreen::new);
        event.register(ModMenus.SIGNAL_BOOSTER_MENU.get(), SignalBoosterScreen::new);
        event.register(ModMenus.PHONE_CHARGER_MENU.get(), PhoneChargerScreen::new);
        event.register(ModMenus.SERVER_MENU.get(), ServerBlockScreen::new);
        event.register(ModMenus.EMERGENCY_DISPATCH_MENU.get(), EmergencyDispatchScreen::new);
    }
}

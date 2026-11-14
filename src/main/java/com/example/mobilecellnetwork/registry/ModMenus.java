package com.example.mobilecellnetwork.registry;

import com.example.mobilecellnetwork.MobileCellNetwork;
import com.example.mobilecellnetwork.gui.AntennaMenu;
import com.example.mobilecellnetwork.gui.EmergencyDispatchMenu;
import com.example.mobilecellnetwork.gui.PhoneChargerMenu;
import com.example.mobilecellnetwork.gui.PhoneMenu;
import com.example.mobilecellnetwork.gui.ServerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MobileCellNetwork.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<PhoneMenu>> PHONE_MENU =
            MENUS.register("phone", () -> new MenuType<>(PhoneMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>, MenuType<AntennaMenu>> ANTENNA_MENU =
            MENUS.register("antenna", () -> IMenuTypeExtension.create(AntennaMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<PhoneChargerMenu>> PHONE_CHARGER_MENU =
            MENUS.register("phone_charger", () -> IMenuTypeExtension.create(PhoneChargerMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<ServerMenu>> SERVER_MENU =
            MENUS.register("server", () -> IMenuTypeExtension.create(ServerMenu::fromNetwork));
    public static final DeferredHolder<MenuType<?>, MenuType<EmergencyDispatchMenu>> EMERGENCY_DISPATCH_MENU =
            MENUS.register("emergency_dispatch", () -> IMenuTypeExtension.create(EmergencyDispatchMenu::fromNetwork));

    private ModMenus() {
    }
}

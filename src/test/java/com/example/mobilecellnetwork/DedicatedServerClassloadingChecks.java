package com.example.mobilecellnetwork;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;

public final class DedicatedServerClassloadingChecks {
    public static void main(String[] args) throws Exception {
        URL[] classpath = Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
                .map(Path::of).map(Path::toUri).map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (java.net.MalformedURLException exception) {
                        throw new IllegalArgumentException(exception);
                    }
                }).toArray(URL[]::new);
        try (URLClassLoader loader = new URLClassLoader(classpath, ClassLoader.getPlatformClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("net.minecraft.client.")
                        || name.startsWith("com.mojang.blaze3d.")) {
                    throw new ClassNotFoundException("Client-only class on dedicated server: " + name);
                }
                return super.loadClass(name, resolve);
            }
        }) {
            for (String name : new String[] {
                    "com.example.mobilecellnetwork.MobileCellNetwork",
                    "com.example.mobilecellnetwork.network.NetworkHandler",
                    "com.example.mobilecellnetwork.network.OpenPhoneScreenPacket",
                    "com.example.mobilecellnetwork.gui.SignalBoosterMenu",
                    "com.example.mobilecellnetwork.blockentity.SignalBoosterBlockEntity",
                    "com.example.mobilecellnetwork.blockentity.PhoneChargerBlockEntity",
                    "com.example.mobilecellnetwork.blockentity.TestAntennaBlockEntity",
                    "com.example.mobilecellnetwork.block.TowerVariant",
                    "com.example.mobilecellnetwork.block.MonopoleGeometry",
                    "com.example.mobilecellnetwork.infrastructure.CablePorts"}) {
                Class<?> type = Class.forName(name, false, loader);
                type.getDeclaredConstructors();
                type.getDeclaredMethods();
            }
        }
        System.out.println("PASS: common entry point, payload registration, phone packet, and booster menu/entity "
                + "verify without client classes");
    }
}

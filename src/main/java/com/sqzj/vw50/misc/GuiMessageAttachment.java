package com.sqzj.vw50.misc;

import net.minecraft.client.multiplayer.chat.GuiMessage;

import java.util.WeakHashMap;

public class GuiMessageAttachment {

    private static final WeakHashMap<GuiMessage, Boolean> EXTRA_DATA = new WeakHashMap<>();

    public static void put(GuiMessage message, Boolean data) {
        EXTRA_DATA.put(message, data);
    }

    public static Boolean get(GuiMessage message) {
        return EXTRA_DATA.get(message);
    }

    public static void remove(GuiMessage message) {
        EXTRA_DATA.remove(message);
    }

    public static void clear() {
        EXTRA_DATA.clear();
    }

}
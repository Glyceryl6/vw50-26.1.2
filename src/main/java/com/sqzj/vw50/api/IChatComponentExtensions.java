package com.sqzj.vw50.api;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;

public interface IChatComponentExtensions {

    void VW50$setMessageHeight(GuiMessage message, int totalHeight);

    void VW50$setMessageWidth(GuiMessage message, int width);

    int VW50$getLineOffset(int lineIndex);

    boolean VW50$isMouseOverRepeatButton();

    void VW50$setMouseOverRepeatButton(boolean value);

    Component VW50$getMarkedMessage();

    void VW50$setMarkedMessage(Component component);

}
package com.sqzj.vw50.api;

import net.minecraft.network.chat.Component;

public interface IChatComponentExtensions {

    boolean VW50$isMouseOverRepeatButton();

    void VW50$setMouseOverRepeatButton(boolean value);

    Component VW50$getMarkedMessage();

    void VW50$setMarkedMessage(Component component);

}
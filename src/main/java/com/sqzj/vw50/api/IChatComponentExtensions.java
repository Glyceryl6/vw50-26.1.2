package com.sqzj.vw50.api;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public interface IChatComponentExtensions {

    boolean VW50$isMouseOverRepeatButton();

    void VW50$setMouseOverRepeatButton(boolean value);

    Component VW50$getMarkedMessage();

    void VW50$setMarkedMessage(Component component);

    boolean VW50$isMouseOverRedEnvelope();

    void VW50$setMouseOverRedEnvelope(boolean value);

    @Nullable
    UUID VW50$getMouseOverRedEnvelopeId();

    void VW50$setMouseOverRedEnvelopeId(UUID id);

}

package com.sqzj.vw50.mixin;

import com.sqzj.vw50.api.IChatComponentExtensions;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class MixinChatScreen extends Screen {

    protected MixinChatScreen(Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        ChatComponent chat = this.minecraft.gui.getChat();
        if (chat instanceof IChatComponentExtensions extensions) {
            if (extensions.VW50$isMouseOverRepeatButton()) {
                Component markedMessage = extensions.VW50$getMarkedMessage();
                chat.addPlayerMessage(markedMessage, null, null);
                cir.setReturnValue(true);
            }
        }
    }

}
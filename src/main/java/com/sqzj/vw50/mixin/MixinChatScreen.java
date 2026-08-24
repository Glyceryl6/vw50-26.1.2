package com.sqzj.vw50.mixin;

import com.sqzj.vw50.misc.hook.HookChatComponent;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class MixinChatScreen extends Screen {

    @Shadow
    private ChatComponent.DisplayMode displayMode;

    protected MixinChatScreen(Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) {
            return;
        }

        ChatComponent chat = this.minecraft.gui.getChat();
        int screenHeight = this.minecraft.getWindow().getGuiScaledHeight();
        if (HookChatComponent.handleMouseClick(
            chat, this.displayMode, screenHeight, (int) event.x(), (int) event.y())) {
            cir.setReturnValue(true);
        }
    }

}
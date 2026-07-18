package com.sqzj.vw50.mixin;

import com.sqzj.vw50.api.IChatComponentExtensions;
import com.sqzj.vw50.client.ClientRedEnvelopeManager;
import com.sqzj.vw50.common.envelope.RedEnvelopeStatus;
import com.sqzj.vw50.server.network.ClaimRedEnvelopePayload;
import com.sqzj.vw50.server.network.RedEnvelopeSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(ChatScreen.class)
public class MixinChatScreen extends Screen {

    protected MixinChatScreen(Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At(value = "HEAD"), cancellable = true)
    private void mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ClientRedEnvelopeManager.isMouseOverClaimPanelClose()) {
            ClientRedEnvelopeManager.closeClaimList();
            cir.setReturnValue(true);
            return;
        }

        ChatComponent chat = this.minecraft.gui.getChat();
        if (chat instanceof IChatComponentExtensions extensions) {
            if (extensions.VW50$isMouseOverRedEnvelope()) {
                UUID id = extensions.VW50$getMouseOverRedEnvelopeId();
                if (id != null) {
                    RedEnvelopeSnapshot snapshot = ClientRedEnvelopeManager.getSnapshot(id);
                    if (snapshot != null && (snapshot.status() != RedEnvelopeStatus.ACTIVE || snapshot.viewerClaimed())) {
                        ClientRedEnvelopeManager.toggleClaimList(id);
                    } else if (snapshot != null && snapshot.usePassword()) {
                        String password = snapshot.password();
                        if (!password.isBlank()) {
                            this.minecraft.keyboardHandler.setClipboard(password);
                            if (this.minecraft.player != null) {
                                this.minecraft.player.sendOverlayMessage(Component.translatable("red_envelope.chat.password_copied").withStyle(ChatFormatting.GOLD));
                            }
                        }
                    } else {
                        ClientPacketDistributor.sendToServer(new ClaimRedEnvelopePayload(id));
                    }

                    cir.setReturnValue(true);
                    return;
                }
            }

            if (extensions.VW50$isMouseOverRepeatButton()) {
                Component markedMessage = extensions.VW50$getMarkedMessage();
                String repeatText = markedMessage.getString().trim();
                if (this.minecraft.player != null && !repeatText.isBlank()) {
                    this.minecraft.player.connection.sendChat(repeatText);
                }

                cir.setReturnValue(true);
            }
        }
    }

}
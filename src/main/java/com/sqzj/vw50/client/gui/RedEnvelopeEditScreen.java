package com.sqzj.vw50.client.gui;

import com.mojang.datafixers.util.Pair;
import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.menu.SendRedEnvelopeMenu;
import com.sqzj.vw50.client.widget.UniversalCheckbox;
import com.sqzj.vw50.misc.GuiMessageAttachment;
import com.sqzj.vw50.misc.GuiMessageExtraData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class RedEnvelopeEditScreen extends AbstractContainerScreen<SendRedEnvelopeMenu> {

    private static final Identifier LOCATION = VW50.prefix("textures/gui/send_red_envelope.png");
    private static final Identifier CHECKBOX_CHECKED = VW50.prefix("small_checkbox_checked");
    private static final Identifier CHECKBOX_UNCHECKED = VW50.prefix("small_checkbox_unchecked");
    private static final Component DEFAULT_TITLE = Component.translatable("red_envelope.default_title");
    private static final Component TOO_MANY_PLAYERS = Component.translatable("red_envelope.too_many_players").withStyle(ChatFormatting.RED);
    private static final Component PLAYER_MORE_THAN_ITEMS = Component.translatable("red_envelope.player_more_than_items").withStyle(ChatFormatting.RED);
    private static final Component LUCKY_MONEY = Component.translatable("red_envelope.lucky_money").withStyle(ChatFormatting.BOLD);
    private static final Component DESTROY_ON_EXPIRED = Component.translatable("red_envelope.destroy_on_expired").withStyle(ChatFormatting.BOLD);
    private static final Component SEND = Component.translatable("red_envelope.send").withStyle(ChatFormatting.BOLD);
    private static final Component HINT_NAME = Component.translatable("red_envelope.hint.name");
    private static final Component HINT_PASSWORD = Component.translatable("red_envelope.hint.password");
    private static final WidgetSprites SEND_BUTTON_SPRITES = new WidgetSprites(
            VW50.prefix("send_button_default"),
            VW50.prefix("send_button_disabled"),
            VW50.prefix("send_button_hover"));
    private static final WidgetSprites PROPERTY_BUTTON_SPRITES = new WidgetSprites(
            VW50.prefix("property_button_default"),
            VW50.prefix("property_button_disabled"),
            VW50.prefix("property_button_hover"));
    private EditBox titleBox;
    private EditBox numberBox;
    private EditBox nameBox;
    private ImageButton sendButton;
    private boolean isLuckyMoney;
    private boolean destroyOnExpired;
    private boolean playerTooMany;
    private boolean playerMoreThanItems;
    private Property property = Property.NORMAL;

    public RedEnvelopeEditScreen(SendRedEnvelopeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 178, 189);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        Pair<Identifier, Identifier> sprites = Pair.of(CHECKBOX_CHECKED, CHECKBOX_UNCHECKED);
        this.titleBox = new EditBox(this.font, x + 34, y + 13, 110, 16, Component.empty());
        this.numberBox = new EditBox(this.font, x + 105, y + 33, 29, 16, Component.empty());
        this.nameBox = new EditBox(this.font, x + 105, y + 51, 61, 15, Component.empty());
        this.titleBox.setHint(Component.translatable("red_envelope.hint.title"));
        this.titleBox.setMaxLength(20);
        this.numberBox.setValue("0");
        this.numberBox.setHint(Component.translatable("red_envelope.hint.number"));
        this.numberBox.setFilter(value -> value.matches("\\d+"));
        this.numberBox.setResponder(this::updateSendButtonState);
        this.nameBox.setFilter(value -> !value.startsWith("/"));
        this.nameBox.setMaxLength(30);
        this.sendButton = new ImageButton(x + 109, y + 74, 50, 18, SEND_BUTTON_SPRITES, this::sendRedEnvelope);
        this.addRenderableWidget(new UniversalCheckbox(x + 57, y + 33, 16, 16, sprites, true, (_, value) -> this.isLuckyMoney = value));
        this.addRenderableWidget(new UniversalCheckbox(x + 57, y + 51, 16, sprites, (_, value) -> this.destroyOnExpired = value));
        this.addRenderableWidget(CycleButton.builder(Property::getDescription, this.property)
                .withValues(Property.values()).displayState(CycleButton.DisplayState.VALUE)
                .withSprite((button, _) -> PROPERTY_BUTTON_SPRITES.get(button.isActive(), button.isHoveredOrFocused()))
                .create(x + 138, y + 34, 27, 14, Component.empty(), (_, value) -> this.updateProperty(value)));
        List.of(this.titleBox, this.numberBox, this.nameBox, this.sendButton).forEach(this::addRenderableWidget);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        int x1 = 57 - this.font.width(LUCKY_MONEY);
        int x2 = 57 - this.font.width(DESTROY_ON_EXPIRED);
        int x3 = 138 - this.font.width(SEND);
        graphics.text(this.font, LUCKY_MONEY, x1 - 2, 36, -1);
        graphics.text(this.font, DESTROY_ON_EXPIRED, x2 - 2, 54, -1);
        graphics.text(this.font, SEND, x3 + 3, 79, -1);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, LOCATION, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT);
    }

    @Override
    protected void extractTooltip(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.playerTooMany && this.sendButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, TOO_MANY_PLAYERS, mouseX, mouseY);
        }

        if (this.playerMoreThanItems && this.sendButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, PLAYER_MORE_THAN_ITEMS, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.titleBox.keyPressed(event) || this.numberBox.keyPressed(event)) {
            return true;
        } else {
            return !event.isEscape() && (this.titleBox.isFocused() || this.numberBox.isFocused()) || super.keyPressed(event);
        }
    }

    @Override
    protected void containerTick() {
        this.updateUIForType();
    }

    private void updateUIForType() {
        switch (this.property) {
            case NORMAL -> {
                this.numberBox.active = true;
                this.nameBox.visible = false;
            }
            case EXCLUSIVE -> {
                this.numberBox.setValue("");
                this.numberBox.active = false;
                this.nameBox.visible = true;
                this.nameBox.setHint(HINT_NAME);
            }
            case PASSWORD -> {
                this.numberBox.active = true;
                this.nameBox.visible = true;
                this.nameBox.setHint(HINT_PASSWORD);
            }
        }
    }

    private void updateSendButtonState(String number) {
        if (number.isEmpty()) {
            this.numberBox.setValue("0");
            return;
        }

        if (number.matches("\\d+")) {
            String trimmed = number.replaceFirst("^0+(?!$)", StringUtils.EMPTY);
            if (!trimmed.equals(number)) {
                this.numberBox.setValue(trimmed);
                this.numberBox.setCursorPosition(trimmed.length());
            }
        }

        NonNullList<ItemStack> stacks = this.menu.giftSlot.copyToList();
        if (stacks.isEmpty()) {
            this.sendButton.active = false;
            return;
        } else {
            if (Integer.parseInt(number) > stacks.size()) {
                this.numberBox.setTextColor(-40864);
                this.sendButton.active = false;
                this.playerMoreThanItems = true;
                return;
            } else {
                this.numberBox.setTextColor(-1);
                this.sendButton.active = true;
                this.playerMoreThanItems = false;
            }
        }

        if (Integer.parseInt(number) > 256) {
            this.numberBox.setTextColor(-40864);
            this.playerTooMany = true;
        } else {
            this.numberBox.setTextColor(-1);
            this.playerTooMany = false;
        }
    }

    private void updateProperty(Property property) {
        List.of(this.numberBox, this.nameBox).forEach(editBox -> editBox.setValue(""));
        this.property = property;
        String number = this.numberBox.getValue();
        String name = this.nameBox.getValue();
        switch (property) {
            case NORMAL -> this.sendButton.active = !number.isEmpty();
            case EXCLUSIVE -> this.sendButton.active = !name.isEmpty();
            case PASSWORD -> this.sendButton.active = !number.isEmpty() && !name.isEmpty();
        }
    }

    private void sendRedEnvelope(Button button) {
        String title = this.titleBox.getValue();
        String number = this.numberBox.getValue();
        String name = this.nameBox.getValue();
        if (title.isEmpty()) title = DEFAULT_TITLE.getString();
        ChatComponent chat = this.minecraft.gui.getChat();
        GuiMessage message = new GuiMessage(this.minecraft.gui.getGuiTicks(), Component.literal(title),
                null, GuiMessageSource.SYSTEM_SERVER, GuiMessageTag.systemSinglePlayer());
        GuiMessageAttachment.put(message, new GuiMessageExtraData(Boolean.FALSE, Boolean.TRUE));
        if (chat.visibleMessageFilter.test(message)) {
            chat.logChatMessage(message);
            chat.addMessageToDisplayQueue(message);
            chat.addMessageToQueue(message);
        }
    }

    private enum Property implements StringRepresentable {

        NORMAL("normal"),
        EXCLUSIVE("exclusive"),
        PASSWORD("password");

        private final String name;

        Property(String name) {
            this.name = name;
        }

        public Component getDescription() {
            return Component.translatable(String.format("red_envelope.property.%s", this.name));
        }

        @Override
        public @NonNull String getSerializedName() {
            return this.name;
        }

    }

}
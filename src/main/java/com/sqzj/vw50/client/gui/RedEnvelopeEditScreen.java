package com.sqzj.vw50.client.gui;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import com.sqzj.vw50.VW50;
import com.sqzj.vw50.client.menu.SendRedEnvelopeMenu;
import com.sqzj.vw50.client.widget.UniversalCheckbox;
import com.sqzj.vw50.server.network.SendRedEnvelopePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class RedEnvelopeEditScreen extends AbstractContainerScreen<SendRedEnvelopeMenu> {

    private static final int SCREEN_WIDTH = 208;
    private static final int SCREEN_HEIGHT = 231;

    private static final Identifier LOCATION = VW50.prefix("textures/gui/send_red_envelope.png");
    private static final Identifier COLOR_POPUP = VW50.prefix("textures/gui/send_red_envelope_select_color.png");
    private static final Identifier ICON_POPUP = VW50.prefix("textures/gui/send_red_envelope_select_icon.png");
    private static final Identifier CHECKBOX_CHECKED = VW50.prefix("small_checkbox_checked");
    private static final Identifier CHECKBOX_UNCHECKED = VW50.prefix("small_checkbox_unchecked");

    private static final Component TOO_MANY_PLAYERS = Component.translatable("red_envelope.too_many_players").withStyle(ChatFormatting.RED);
    private static final Component PLAYER_MORE_THAN_ITEMS = Component.translatable("red_envelope.player_more_than_items").withStyle(ChatFormatting.RED);
    private static final Component LUCKY_MONEY = Component.translatable("red_envelope.lucky_money").withStyle(ChatFormatting.BOLD);
    private static final Component DESTROY_ON_EXPIRED = Component.translatable("red_envelope.destroy_on_expired").withStyle(ChatFormatting.BOLD);
    private static final Component ICON = Component.translatable("red_envelope.custom.icon").withStyle(ChatFormatting.BOLD);
    private static final Component COLOR = Component.translatable("red_envelope.custom.color").withStyle(ChatFormatting.BOLD);
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

    // Main texture coordinates. These are deliberately centralized so the UI can
    // be fine-tuned later without having to chase values through the class.
    private static final int PROPERTY_X = 154;
    private static final int PROPERTY_Y = 34;
    private static final int PROPERTY_W = 27;
    private static final int PROPERTY_H = 14;
    private static final int EXTRA_X = 120;
    private static final int EXTRA_Y = 51;
    private static final int EXTRA_W = 63;
    private static final int EXTRA_H = 16;
    private static final int LUCKY_X = 72;
    private static final int LUCKY_Y = 33;
    private static final int RETURN_X = 72;
    private static final int RETURN_Y = 51;
    private static final int SELECTOR_SIZE = 20;
    private static final int COLOR_SELECTOR_X = 46;
    private static final int COLOR_SELECTOR_Y = 73;
    private static final int ICON_SELECTOR_X = 94;
    private static final int ICON_SELECTOR_Y = 73;
    private static final int SEND_X = 123;
    private static final int SEND_Y = 74;
    private static final int SEND_W = 57;
    private static final int SEND_H = 19;
    private static final int PREVIEW_X = 10;
    private static final int PREVIEW_Y = 104;
    private static final int PREVIEW_W = 188;
    private static final int PREVIEW_H = 34;

    private static final int COLOR_POPUP_W = 54;
    private static final int COLOR_POPUP_H = 34;
    private static final int ICON_POPUP_W = 94;
    private static final int ICON_POPUP_H = 55;

    private EditBox titleBox;
    private EditBox numberBox;
    private EditBox nameBox;
    private ImageButton sendButton;
    private boolean isLuckyMoney = true;
    private boolean returnWhenExpired = true;
    private boolean playerTooMany;
    private boolean playerMoreThanItems;
    private boolean colorPopupOpen;
    private boolean iconPopupOpen;
    private Property property = Property.NORMAL;
    private int selectedColorIndex = 1;
    private int selectedIconIndex;
    private int iconPage;

    public RedEnvelopeEditScreen(SendRedEnvelopeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SCREEN_WIDTH, SCREEN_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;
        Pair<Identifier, Identifier> sprites = Pair.of(CHECKBOX_CHECKED, CHECKBOX_UNCHECKED);
        this.titleBox = new EditBox(this.font, x + 49, y + 13, 110, 16, Component.empty());
        this.numberBox = new EditBox(this.font, x + 120, y + 33, 29, 16, Component.empty());
        this.nameBox = new EditBox(this.font, x + 120, y + 51, 61, 15, Component.empty());
        this.titleBox.setHint(Component.translatable("red_envelope.hint.title"));
        this.titleBox.setMaxLength(64);
        this.numberBox.setHint(Component.translatable("red_envelope.hint.number"));
        this.numberBox.setFilter(value -> value.matches("\\d+"));
        this.numberBox.setResponder(this::updateSendButtonState);
        this.nameBox.setFilter(value -> !value.startsWith("/"));
        this.nameBox.setMaxLength(64);
        this.nameBox.setResponder(ignored -> this.updateSendButtonState(this.numberBox.getValue()));
        this.sendButton = new ImageButton(x + SEND_X, y + SEND_Y, SEND_W, SEND_H, SEND_BUTTON_SPRITES, this::sendRedEnvelope);
        this.addRenderableWidget(new UniversalCheckbox(x + LUCKY_X, y + LUCKY_Y, 16, 16,
                sprites, true, (_, value) -> this.isLuckyMoney = value));
        this.addRenderableWidget(new UniversalCheckbox(x + RETURN_X, y + RETURN_Y, 16,
                sprites, (_, value) -> this.returnWhenExpired = value));
        this.addRenderableWidget(CycleButton.builder(Property::getDescription, this.property)
                .withValues(Property.values()).displayState(CycleButton.DisplayState.VALUE)
                .withSprite((button, _) -> PROPERTY_BUTTON_SPRITES.get(button.isActive(), button.isHoveredOrFocused()))
                .create(x + PROPERTY_X, y + PROPERTY_Y, PROPERTY_W, PROPERTY_H, Component.empty(), (_, value) -> this.updateProperty(value)));
        List.of(this.titleBox, this.numberBox, this.nameBox, this.sendButton).forEach(this::addRenderableWidget);
        this.updateUIForType();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int luckyTextX = LUCKY_X - this.font.width(LUCKY_MONEY) - 3;
        int returnTextX = RETURN_X - this.font.width(DESTROY_ON_EXPIRED) - 3;
        graphics.text(this.font, LUCKY_MONEY, luckyTextX, LUCKY_Y + 4, -1);
        graphics.text(this.font, DESTROY_ON_EXPIRED, returnTextX, RETURN_Y + 4, -1);
        graphics.text(this.font, SEND, SEND_X + (SEND_W - this.font.width(SEND)) / 2, SEND_Y + 5, -1);
        graphics.text(this.font, ICON, -24, 71, -1);
        graphics.text(this.font, COLOR, -24, 88, -1);
        this.renderSelectors(graphics);
        this.renderPreview(graphics);
        this.renderNameSuggestions(graphics);
        this.renderPopups(graphics);
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, LOCATION,
                this.leftPos, this.topPos, 0.0F, 0.0F,
                SCREEN_WIDTH, SCREEN_HEIGHT, 256, 256);
    }

    @Override
    protected void extractTooltip(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.playerTooMany && this.sendButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, TOO_MANY_PLAYERS, mouseX, mouseY);
        } else if (this.playerMoreThanItems && this.sendButton.isHovered()) {
            graphics.setTooltipForNextFrame(this.font, PLAYER_MORE_THAN_ITEMS, mouseX, mouseY);
        }

        if (this.isInsideScreen(mouseX, mouseY, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H)) {
            graphics.setTooltipForNextFrame(this.font, this.previewTooltip(), Optional.empty(), mouseX, mouseY);
        } else if (this.isInsideScreen(mouseX, mouseY, COLOR_SELECTOR_X, COLOR_SELECTOR_Y, SELECTOR_SIZE, SELECTOR_SIZE)) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("red_envelope.selector.color", colorHex(selectedCardColor())), mouseX, mouseY);
        } else if (this.isInsideScreen(mouseX, mouseY, ICON_SELECTOR_X, ICON_SELECTOR_Y, SELECTOR_SIZE, SELECTOR_SIZE)) {
            graphics.setTooltipForNextFrame(this.font, Component.translatable("red_envelope.selector.icon", selectedIconStack().getHoverName()), mouseX, mouseY);
        } else if (this.iconPopupOpen) {
            int hovered = this.hoveredIconIndex(mouseX, mouseY);
            if (hovered >= 0) {
                ItemStack stack = this.iconStack(hovered);
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(stack.getHoverName());
                tooltip.add(Component.literal(RedEnvelopeStyleCatalog.ICON_ITEM_IDS.get(hovered)).withStyle(ChatFormatting.DARK_GRAY));
                graphics.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (this.titleBox.keyPressed(event) || this.numberBox.keyPressed(event) || this.nameBox.keyPressed(event)) return true;
        return !event.isEscape() && (this.titleBox.isFocused() || this.numberBox.isFocused() || this.nameBox.isFocused()) || super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (this.handleColorPopupClick(mouseX, mouseY)) return true;
        if (this.handleIconPopupClick(mouseX, mouseY)) return true;
        if (this.isInsideScreen(mouseX, mouseY, COLOR_SELECTOR_X, COLOR_SELECTOR_Y, SELECTOR_SIZE, SELECTOR_SIZE)) {
            this.colorPopupOpen = !this.colorPopupOpen;
            this.iconPopupOpen = false;
            return true;
        }

        if (this.isInsideScreen(mouseX, mouseY, ICON_SELECTOR_X, ICON_SELECTOR_Y, SELECTOR_SIZE, SELECTOR_SIZE)) {
            this.iconPopupOpen = !this.iconPopupOpen;
            this.colorPopupOpen = false;
            this.iconPage = this.selectedIconIndex / RedEnvelopeStyleCatalog.ICONS_PER_PAGE;
            return true;
        }

        if (this.property == Property.EXCLUSIVE) {
            List<String> names = this.matchingOnlineNames();
            int sx = this.leftPos + EXTRA_X;
            int sy = this.topPos + EXTRA_Y + EXTRA_H + 1;
            for (int i = 0; i < names.size(); i++) {
                if (this.isInside(mouseX, mouseY, sx, sy + i * 12, 88, 12)) {
                    this.nameBox.setValue(names.get(i));
                    return true;
                }
            }
        }

        this.colorPopupOpen = false;
        this.iconPopupOpen = false;
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void containerTick() {
        this.updateUIForType();
    }

    private void renderSelectors(GuiGraphicsExtractor graphics) {
        int color = this.selectedCardColor();
        graphics.fill(COLOR_SELECTOR_X + 2, COLOR_SELECTOR_Y + 2,
                COLOR_SELECTOR_X + SELECTOR_SIZE - 2, COLOR_SELECTOR_Y + SELECTOR_SIZE - 2, color);
        if ((color & 0x00FFFFFF) == 0x00FFFFFF) {
            this.drawOutline(graphics, COLOR_SELECTOR_X + 2, COLOR_SELECTOR_Y + 2,
                    COLOR_SELECTOR_X + SELECTOR_SIZE - 2,
                    COLOR_SELECTOR_Y + SELECTOR_SIZE - 2, 0xFF777777);
        }

        ItemStack icon = selectedIconStack();
        if (!icon.isEmpty()) {
            graphics.fakeItem(icon, ICON_SELECTOR_X + 2, ICON_SELECTOR_Y + 2);
        }
    }

    private void renderPreview(GuiGraphicsExtractor graphics) {
        int color = this.selectedCardColor();
        graphics.fill(PREVIEW_X, PREVIEW_Y, PREVIEW_X + PREVIEW_W, PREVIEW_Y + PREVIEW_H, color);
        graphics.fill(PREVIEW_X + 2, PREVIEW_Y + 2, PREVIEW_X + PREVIEW_W - 2, PREVIEW_Y + PREVIEW_H - 2, darken(color, 36));
        ItemStack icon = this.selectedIconStack();
        if (!icon.isEmpty()) graphics.fakeItem(icon, PREVIEW_X + 8, PREVIEW_Y + 9);
        String title = this.ellipsize(this.titleBox.getValue().isBlank()
                ? Component.translatable("red_envelope.default_title").getString()
                : this.titleBox.getValue(), 108);
        String detail = switch (this.property) {
            case PASSWORD -> this.ellipsize(Component.translatable("red_envelope.chat.copy_password",
                    this.nameBox.getValue().isBlank() ? "?" : this.nameBox.getValue()).getString(), 142);
            case EXCLUSIVE -> this.ellipsize(Component.translatable("red_envelope.chat.exclusive_value",
                    this.nameBox.getValue().isBlank() ? "?" : this.nameBox.getValue()).getString(), 142);
            case NORMAL -> Component.translatable("red_envelope.chat.click", 0, this.parsePositive(this.numberBox.getValue())).getString();
        };

        int primaryText = this.readableTextColor(color, 0xFFFFD27A);
        int secondaryText = this.readableTextColor(color, 0xFFFFFF88);
        graphics.text(this.font, title, PREVIEW_X + 30, PREVIEW_Y + 6, primaryText);
        graphics.text(this.font, detail, PREVIEW_X + 30, PREVIEW_Y + 19, secondaryText);
    }

    private void renderNameSuggestions(GuiGraphicsExtractor graphics) {
        if (this.property != Property.EXCLUSIVE || !this.nameBox.isFocused()) return;
        List<String> names = this.matchingOnlineNames();
        int sx = EXTRA_X;
        int sy = EXTRA_Y + EXTRA_H + 1;
        for (int i = 0; i < names.size(); i++) {
            int y = sy + i * 12;
            graphics.fill(sx, y, sx + 88, y + 12, 0xEE2C1512);
            graphics.text(this.font, names.get(i), sx + 3, y + 2, -1);
        }
    }

    private void renderPopups(GuiGraphicsExtractor graphics) {
        if (this.colorPopupOpen) {
            int x = colorPopupX();
            int y = popupY();
            graphics.blit(RenderPipelines.GUI_TEXTURED, COLOR_POPUP,
                    x, y, 0.0F, 0.0F, COLOR_POPUP_W,
                    COLOR_POPUP_H, COLOR_POPUP_W, COLOR_POPUP_H);
            int localIndex = this.selectedColorIndex;
            int col = localIndex % 4;
            int row = localIndex / 4;
            this.drawOutline(graphics, x + 4 + col * 12, y + 8 + row * 12,
                    x + 13 + col * 12, y + 17 + row * 12, 0xFFFFFFFF);
        }

        if (this.iconPopupOpen) {
            int x = this.iconPopupX();
            int y = this.popupY();
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICON_POPUP,
                    x, y, 0.0F, 0.0F, ICON_POPUP_W,
                    ICON_POPUP_H, ICON_POPUP_W, ICON_POPUP_H);
            int start = this.iconPage * RedEnvelopeStyleCatalog.ICONS_PER_PAGE;
            for (int slot = 0; slot < RedEnvelopeStyleCatalog.ICONS_PER_PAGE; slot++) {
                int index = start + slot;
                if (index >= RedEnvelopeStyleCatalog.ICON_ITEM_IDS.size()) break;
                int slotX = x + 14 + (slot % 3) * 23;
                int slotY = y + 8 + (slot / 3) * 23;
                ItemStack stack = iconStack(index);
                if (!stack.isEmpty()) graphics.fakeItem(stack, slotX + 2, slotY + 2);
                if (index == this.selectedIconIndex) {
                    this.drawOutline(graphics, slotX, slotY, slotX + 20, slotY + 20, 0xFFFFE08A);
                }
            }
        }
    }

    private boolean handleColorPopupClick(int mouseX, int mouseY) {
        if (!this.colorPopupOpen) return false;
        int x = this.leftPos + this.colorPopupX();
        int y = this.topPos + this.popupY();
        for (int i = 0; i < RedEnvelopeStyleCatalog.CARD_COLORS.size(); i++) {
            int col = i % 4;
            int row = i / 4;
            int sx = x + 4 + col * 12;
            int sy = y + 8 + row * 12;
            if (this.isInside(mouseX, mouseY, sx, sy, 10, 10)) {
                this.selectedColorIndex = i;
                this.colorPopupOpen = false;
                return true;
            }
        }

        return this.isInside(mouseX, mouseY, x, y, COLOR_POPUP_W, COLOR_POPUP_H);
    }

    private boolean handleIconPopupClick(int mouseX, int mouseY) {
        if (!this.iconPopupOpen) return false;
        int x = this.leftPos + this.iconPopupX();
        int y = this.topPos + this.popupY();

        if (this.isInside(mouseX, mouseY, x, y + 18, 14, 20)) {
            this.iconPage = Math.floorMod(this.iconPage - 1, RedEnvelopeStyleCatalog.iconPageCount());
            return true;
        }

        if (this.isInside(mouseX, mouseY, x + 80, y + 18, 14, 20)) {
            this.iconPage = (this.iconPage + 1) % RedEnvelopeStyleCatalog.iconPageCount();
            return true;
        }

        int start = this.iconPage * RedEnvelopeStyleCatalog.ICONS_PER_PAGE;
        for (int slot = 0; slot < RedEnvelopeStyleCatalog.ICONS_PER_PAGE; slot++) {
            int index = start + slot;
            if (index >= RedEnvelopeStyleCatalog.ICON_ITEM_IDS.size()) break;
            int sx = x + 14 + (slot % 3) * 23;
            int sy = y + 8 + (slot / 3) * 23;
            if (this.isInside(mouseX, mouseY, sx, sy, 20, 20)) {
                this.selectedIconIndex = index;
                this.iconPopupOpen = false;
                return true;
            }
        }

        return this.isInside(mouseX, mouseY, x, y, ICON_POPUP_W, ICON_POPUP_H);
    }

    private int hoveredIconIndex(int mouseX, int mouseY) {
        int x = this.leftPos + this.iconPopupX();
        int y = this.topPos + this.popupY();
        int start = this.iconPage * RedEnvelopeStyleCatalog.ICONS_PER_PAGE;
        for (int slot = 0; slot < RedEnvelopeStyleCatalog.ICONS_PER_PAGE; slot++) {
            int index = start + slot;
            if (index >= RedEnvelopeStyleCatalog.ICON_ITEM_IDS.size()) break;
            int sx = x + 14 + (slot % 3) * 23;
            int sy = y + 8 + (slot / 3) * 23;
            if (this.isInside(mouseX, mouseY, sx, sy, 20, 20)) return index;
        }

        return -1;
    }

    private List<Component> previewTooltip() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("red_envelope.preview.full"));
        lines.add(Component.literal(this.titleBox.getValue().isBlank()
                ? Component.translatable("red_envelope.default_title").getString()
                : this.titleBox.getValue()).withStyle(ChatFormatting.GOLD));
        if (this.property == Property.PASSWORD) {
            lines.add(Component.translatable("red_envelope.chat.copy_password", this.nameBox.getValue()).withStyle(ChatFormatting.YELLOW));
        }

        if (this.property == Property.EXCLUSIVE) {
            lines.add(Component.translatable("red_envelope.chat.exclusive_value", this.nameBox.getValue()).withStyle(ChatFormatting.YELLOW));
        }

        lines.add(Component.translatable("red_envelope.preview.style", selectedIconId(), colorHex(selectedCardColor())).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    private List<String> matchingOnlineNames() {
        if (this.minecraft.getConnection() == null) return List.of();
        String filter = this.nameBox.getValue().toLowerCase(Locale.ROOT);
        return this.minecraft.getConnection().getListedOnlinePlayers().stream().map(PlayerInfo::getProfile).map(GameProfile::name)
                .filter(name -> filter.isBlank() || name.toLowerCase(Locale.ROOT).contains(filter)).limit(5).toList();
    }

    private void updateUIForType() {
        switch (this.property) {
            case NORMAL -> {
                this.numberBox.active = true;
                this.nameBox.visible = false;
            }
            case EXCLUSIVE -> {
                if (!"1".equals(this.numberBox.getValue())) this.numberBox.setValue("1");
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

        this.updateSendButtonState(this.numberBox.getValue());
    }

    private void updateSendButtonState(String number) {
        if (this.sendButton == null || this.numberBox == null) return;
        this.playerTooMany = false;
        this.playerMoreThanItems = false;
        int playerCount = this.parsePositive(number);
        int itemCount = this.getGiftCount();
        boolean hasProperty = this.property == Property.NORMAL || !this.nameBox.getValue().isBlank();
        boolean numberOk = this.property == Property.EXCLUSIVE || playerCount > 0;
        boolean countOk = itemCount > 0 && playerCount <= itemCount;
        boolean maxOk = playerCount <= 256;
        this.playerMoreThanItems = itemCount > 0 && playerCount > itemCount;
        this.playerTooMany = playerCount > 256;
        this.numberBox.setTextColor((numberOk && countOk && maxOk) ? -1 : -40864);
        this.sendButton.active = itemCount > 0 && numberOk && countOk && maxOk && hasProperty;
    }

    private int parsePositive(String number) {
        if (number == null || number.isBlank()) return 0;
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int getGiftCount() {
        return this.menu.giftSlot.copyToList().stream()
                .filter(stack -> !stack.isEmpty())
                .mapToInt(ItemStack::getCount).sum();
    }

    private void updateProperty(Property property) {
        this.nameBox.setValue("");
        if (property == Property.EXCLUSIVE) this.numberBox.setValue("1");
        else if (this.numberBox.getValue().equals("1") && this.property == Property.EXCLUSIVE) this.numberBox.setValue("");
        this.property = property;
        this.updateUIForType();
    }

    private void sendRedEnvelope(Button button) {
        String title = this.titleBox.getValue().trim();
        String propertyValue = this.nameBox.getValue().trim();
        int playerCount = this.property == Property.EXCLUSIVE ? 1 : this.parsePositive(this.numberBox.getValue());
        SendRedEnvelopePayload.PropertyType type = switch (this.property) {
            case NORMAL -> SendRedEnvelopePayload.PropertyType.NORMAL;
            case PASSWORD -> SendRedEnvelopePayload.PropertyType.PASSWORD;
            case EXCLUSIVE -> SendRedEnvelopePayload.PropertyType.EXCLUSIVE;
        };

        ClientPacketDistributor.sendToServer(new SendRedEnvelopePayload(
                title,
                playerCount,
                this.isLuckyMoney,
                this.returnWhenExpired,
                type,
                propertyValue,
                this.selectedIconId(),
                this.selectedCardColor()));
    }

    private int selectedCardColor() {
        return RedEnvelopeStyleCatalog.CARD_COLORS.get(Math.clamp(this.selectedColorIndex, 0, RedEnvelopeStyleCatalog.CARD_COLORS.size() - 1));
    }

    private String selectedIconId() {
        return RedEnvelopeStyleCatalog.ICON_ITEM_IDS.get(Math.clamp(this.selectedIconIndex, 0, RedEnvelopeStyleCatalog.ICON_ITEM_IDS.size() - 1));
    }

    private ItemStack selectedIconStack() {
        return this.iconStack(this.selectedIconIndex);
    }

    private ItemStack iconStack(int index) {
        if (index < 0 || index >= RedEnvelopeStyleCatalog.ICON_ITEM_IDS.size()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(RedEnvelopeStyleCatalog.ICON_ITEM_IDS.get(index));
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    private int colorPopupX() {
        return COLOR_SELECTOR_X + SELECTOR_SIZE / 2 - COLOR_POPUP_W / 2;
    }

    private int iconPopupX() {
        return ICON_SELECTOR_X + SELECTOR_SIZE / 2 - ICON_POPUP_W / 2;
    }

    private int popupY() {
        return 94;
    }

    private String ellipsize(String text, int width) {
        if (this.font.width(text) <= width) return text;
        String suffix = "...";
        while (!text.isEmpty() && this.font.width(text + suffix) > width) {
            text = text.substring(0, text.length() - 1);
        }

        return text + suffix;
    }

    private int readableTextColor(int background, int fallback) {
        int r = (background >>> 16) & 0xFF;
        int g = (background >>> 8) & 0xFF;
        int b = background & 0xFF;
        return r * 299 + g * 587 + b * 114 > 180000 ? 0xFF4A120E : fallback;
    }

    private String colorHex(int color) {
        return String.format("#%06X", color & 0x00FFFFFF);
    }

    private boolean isInsideScreen(int mouseX, int mouseY, int x, int y, int width, int height) {
        return isInside(mouseX, mouseY, this.leftPos + x, this.topPos + y, width, height);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private void drawOutline(GuiGraphicsExtractor graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    private int darken(int argb, int amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.max(0, ((argb >>> 16) & 0xFF) - amount);
        int g = Math.max(0, ((argb >>> 8) & 0xFF) - amount);
        int b = Math.max(0, (argb & 0xFF) - amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
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
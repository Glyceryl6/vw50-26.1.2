package com.sqzj.vw50.client.widget;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class UniversalCheckbox extends AbstractButton {

    private boolean selected;
    private final Pair<Identifier, Identifier> sprite;
    private final OnValueChange onValueChange;

    public UniversalCheckbox(int x, int y, int size, Pair<Identifier, Identifier> sprite, OnValueChange onValueChange) {
        this(x, y, size, size, sprite, false, onValueChange);
    }

    public UniversalCheckbox(
            int x, int y, int width, int height, Pair<Identifier, Identifier> sprite,
            boolean selected, OnValueChange onValueChange) {
        super(x, y, width, height, Component.empty());
        this.sprite = sprite;
        this.selected = selected;
        this.onValueChange = onValueChange;
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.selected = !this.selected;
        this.onValueChange.onValueChange(this, this.selected);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Identifier sprite = this.selected ? this.sprite.getFirst() : this.sprite.getSecond();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, this.getX(), this.getY(), this.width, this.height, ARGB.white(this.alpha));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {

    }

    public interface OnValueChange {

        OnValueChange NOP = (checkbox, value) -> {};

        void onValueChange(UniversalCheckbox checkbox, boolean value);

    }

}
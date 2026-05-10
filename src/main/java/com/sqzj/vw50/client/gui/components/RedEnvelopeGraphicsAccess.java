package com.sqzj.vw50.client.gui.components;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;

import java.util.function.Consumer;

public class RedEnvelopeGraphicsAccess implements ChatComponent.ChatGraphicsAccess {

    public final GuiGraphicsExtractor graphics;
    public final ActiveTextCollector textRenderer;
    public ActiveTextCollector.Parameters parameters;

    public RedEnvelopeGraphicsAccess(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
        this.textRenderer = graphics.textRenderer(GuiGraphicsExtractor.HoveredTextEffects.NONE);
        this.parameters = this.textRenderer.defaultParameters();
    }

    @Override
    public void updatePose(Consumer<Matrix3x2f> updater) {
        updater.accept(this.graphics.pose());
        this.parameters = this.parameters.withPose(new Matrix3x2f(this.graphics.pose()));
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int color) {
        this.graphics.fill(x0, y0, x1, y1, color);
    }

    @Override
    public boolean handleMessage(int textTop, float opacity, FormattedCharSequence message) {
        this.textRenderer.accept(TextAlignment.LEFT, 0, textTop, this.parameters.withOpacity(opacity), message);
        return false;
    }

    @Override
    public void handleTag(int x0, int y0, int x1, int y1, float opacity, GuiMessageTag tag) {

    }

    @Override
    public void handleTagIcon(int left, int bottom, boolean forceVisible, GuiMessageTag tag, GuiMessageTag.Icon icon) {

    }

}
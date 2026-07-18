package com.sqzj.vw50.misc;

import com.sqzj.vw50.server.network.RedEnvelopeSnapshot;

import java.util.UUID;

public final class GuiMessageExtraData {

    public boolean canPlusOne;
    public boolean isRedEnvelope;
    public boolean isRedEnvelopeFinishNotice;
    public String repeatText;
    public UUID redEnvelopeId;
    public RedEnvelopeSnapshot redEnvelopeSnapshot;
    public boolean redEnvelopeWrapped;
    public int redEnvelopeCardWidth;
    public int redEnvelopeCardHeight;
    public int redEnvelopeTotalHeight;
    public int redEnvelopePlaceholderLines;

    public GuiMessageExtraData(boolean canPlusOne, boolean isRedEnvelope) {
        this.canPlusOne = canPlusOne;
        this.isRedEnvelope = isRedEnvelope;
        this.isRedEnvelopeFinishNotice = false;
        this.repeatText = "";
        this.redEnvelopeId = null;
        this.redEnvelopeSnapshot = null;
        this.redEnvelopeWrapped = false;
        this.redEnvelopeCardWidth = 158;
        this.redEnvelopeCardHeight = 32;
        this.redEnvelopeTotalHeight = 32;
        this.redEnvelopePlaceholderLines = 3;
    }

    public static GuiMessageExtraData redEnvelope(RedEnvelopeSnapshot snapshot) {
        GuiMessageExtraData data = new GuiMessageExtraData(false, true);
        data.redEnvelopeId = snapshot.id();
        data.redEnvelopeSnapshot = snapshot;
        return data;
    }

    public static GuiMessageExtraData finishNotice(RedEnvelopeSnapshot snapshot) {
        GuiMessageExtraData data = new GuiMessageExtraData(false, false);
        data.isRedEnvelopeFinishNotice = true;
        data.redEnvelopeId = snapshot.id();
        data.redEnvelopeSnapshot = snapshot;
        return data;
    }

}
package com.sqzj.vw50.client.gui;

import java.util.List;

/**
 * Ordered, client-side style catalogue used by the red-envelope editor.
 *
 * <p>The selector automatically paginates this list in groups of six, so adding
 * a new icon only requires appending its item id here. No screen layout changes
 * are needed.</p>
 */
public final class RedEnvelopeStyleCatalog {

    public static final int ICONS_PER_PAGE = 6;

    public static final List<Integer> CARD_COLORS = List.of(
            0xFF9E1503,
            0xFFB82525,
            0xFFEE5050,
            0xFFFFFFFF,
            0xFF2589B8,
            0xFF27B825,
            0xFFFAE900,
            0xFFA617B5);

    public static final List<String> ICON_ITEM_IDS = List.of(
            "vw50:empty_red_envelope",
            "minecraft:paper",
            "minecraft:red_dye",
            "minecraft:gold_ingot",
            "minecraft:diamond",
            "minecraft:emerald",
            "minecraft:apple",
            "minecraft:golden_apple",
            "minecraft:cake",
            "minecraft:cookie",
            "minecraft:firework_rocket",
            "minecraft:experience_bottle",
            "minecraft:nether_star",
            "minecraft:heart_of_the_sea",
            "minecraft:amethyst_shard",
            "minecraft:echo_shard",
            "minecraft:ender_pearl",
            "minecraft:name_tag",
            "minecraft:book",
            "minecraft:honey_bottle",
            "minecraft:slime_ball",
            "minecraft:rabbit_foot",
            "minecraft:totem_of_undying",
            "minecraft:music_disc_cat"
    );

    private RedEnvelopeStyleCatalog() {
    }

    public static int iconPageCount() {
        return Math.max(1, (ICON_ITEM_IDS.size() + ICONS_PER_PAGE - 1) / ICONS_PER_PAGE);
    }
}

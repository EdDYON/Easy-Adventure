package com.eddy1.easyadventure.config;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.Nullable;

public final class EasyAdventureServerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue PLAYER_MAX_HORIZONTAL_SIZE;
    public static final ForgeConfigSpec.IntValue PLAYER_MAX_UPWARD_SIZE;
    public static final ForgeConfigSpec.IntValue PLAYER_MAX_BELOW_SIZE;
    public static final ForgeConfigSpec.IntValue PLAYER_MAX_BLOCKS;
    public static final ForgeConfigSpec.IntValue TEAM_MAX_HORIZONTAL_SIZE;
    public static final ForgeConfigSpec.IntValue TEAM_MAX_UPWARD_SIZE;
    public static final ForgeConfigSpec.IntValue TEAM_MAX_BELOW_SIZE;
    public static final ForgeConfigSpec.IntValue TEAM_MAX_BLOCKS;
    public static final ForgeConfigSpec.IntValue ADMIN_MAX_HORIZONTAL_SIZE;
    public static final ForgeConfigSpec.IntValue ADMIN_MAX_UPWARD_SIZE;
    public static final ForgeConfigSpec.IntValue ADMIN_MAX_BELOW_SIZE;
    public static final ForgeConfigSpec.IntValue ADMIN_MAX_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue PREVENT_BASE_OVERLAP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("base_limits");
        PLAYER_MAX_HORIZONTAL_SIZE = builder
                .comment("Maximum displayed X/Z size for normal players. Odd values keep the core centered.")
                .defineInRange("playerMaxHorizontalSize", 15, 3, 127);
        PLAYER_MAX_UPWARD_SIZE = builder
                .comment("Maximum displayed upward Y size for normal players.")
                .defineInRange("playerMaxUpwardSize", 15, 2, 320);
        PLAYER_MAX_BELOW_SIZE = builder
                .comment("Maximum downward Y depth for normal players.")
                .defineInRange("playerMaxBelowSize", 8, 0, 128);
        PLAYER_MAX_BLOCKS = builder
                .comment("Maximum real block count in one base volume for normal players.")
                .defineInRange("playerMaxBlocks", 4096, 27, 262144);
        TEAM_MAX_HORIZONTAL_SIZE = builder
                .comment("Maximum displayed X/Z size for players in a vanilla scoreboard team.")
                .defineInRange("teamMaxHorizontalSize", 31, 3, 127);
        TEAM_MAX_UPWARD_SIZE = builder
                .comment("Maximum displayed upward Y size for players in a vanilla scoreboard team.")
                .defineInRange("teamMaxUpwardSize", 24, 2, 320);
        TEAM_MAX_BELOW_SIZE = builder
                .comment("Maximum downward Y depth for players in a vanilla scoreboard team.")
                .defineInRange("teamMaxBelowSize", 12, 0, 128);
        TEAM_MAX_BLOCKS = builder
                .comment("Maximum real block count in one base volume for players in a vanilla scoreboard team.")
                .defineInRange("teamMaxBlocks", 16384, 27, 524288);
        ADMIN_MAX_HORIZONTAL_SIZE = builder
                .comment("Maximum displayed X/Z size for operators.")
                .defineInRange("adminMaxHorizontalSize", 63, 3, 127);
        ADMIN_MAX_UPWARD_SIZE = builder
                .comment("Maximum displayed upward Y size for operators.")
                .defineInRange("adminMaxUpwardSize", 63, 2, 320);
        ADMIN_MAX_BELOW_SIZE = builder
                .comment("Maximum downward Y depth for operators.")
                .defineInRange("adminMaxBelowSize", 32, 0, 128);
        ADMIN_MAX_BLOCKS = builder
                .comment("Maximum real block count in one base volume for operators.")
                .defineInRange("adminMaxBlocks", 262144, 27, 2097152);
        PREVENT_BASE_OVERLAP = builder
                .comment("When true, base territories cannot overlap other registered placed bases.")
                .define("preventBaseOverlap", true);
        builder.pop();
        SPEC = builder.build();
    }

    private EasyAdventureServerConfig() {
    }

    public static SizeLimits limitsFor(@Nullable Player player) {
        boolean operator = player != null && player.hasPermissions(2);
        if (operator) {
            return new SizeLimits(
                    ADMIN_MAX_HORIZONTAL_SIZE.get(),
                    ADMIN_MAX_UPWARD_SIZE.get(),
                    ADMIN_MAX_BELOW_SIZE.get(),
                    ADMIN_MAX_BLOCKS.get()
            );
        }
        if (player != null && player.getTeam() != null) {
            return new SizeLimits(
                    TEAM_MAX_HORIZONTAL_SIZE.get(),
                    TEAM_MAX_UPWARD_SIZE.get(),
                    TEAM_MAX_BELOW_SIZE.get(),
                    TEAM_MAX_BLOCKS.get()
            );
        }
        return new SizeLimits(
                PLAYER_MAX_HORIZONTAL_SIZE.get(),
                PLAYER_MAX_UPWARD_SIZE.get(),
                PLAYER_MAX_BELOW_SIZE.get(),
                PLAYER_MAX_BLOCKS.get()
        );
    }

    public static boolean preventBaseOverlap() {
        return PREVENT_BASE_OVERLAP.get();
    }

    public record SizeLimits(int maxHorizontalSize, int maxUpwardSize, int maxBelowSize, int maxBlocks) {
    }
}

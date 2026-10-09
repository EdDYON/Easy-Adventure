package com.eddy1.easyadventure.block.core;

import com.eddy1.easyadventure.EasyAdventure;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class CoreCompat {
    private static final Set<String> CREATE_DANGEROUS_PATH_PARTS = Set.of(
            "bearing",
            "piston",
            "gantry",
            "pulley",
            "chassis",
            "cart_assembler",
            "minecart_anchor",
            "track_station",
            "contraption_controls",
            "portable_storage_interface",
            "portable_fluid_interface",
            "mechanical_arm",
            "deployer",
            "mechanical_roller",
            "chain_conveyor",
            "package_frogport",
            "packager",
            "repackager",
            "item_vault",
            "fluid_tank",
            "stock_ticker",
            "factory_gauge"
    );

    private static final Set<String> MEKANISM_DANGEROUS_PATH_PARTS = Set.of(
            "dynamic_tank",
            "dynamic_valve",
            "thermal_evaporation",
            "boiler_",
            "turbine_",
            "fission_reactor",
            "fusion_reactor",
            "induction_casing",
            "induction_port",
            "induction_cell",
            "induction_provider",
            "sps_",
            "supercharged_coil",
            "laser_focus_matrix",
            "control_rod_assembly",
            "reactor_glass"
    );

    public static final TagKey<Block> CANNOT_PACK = blockTag("cannot_pack");
    public static final TagKey<Block> IGNORED_DURING_PACK = blockTag("ignored_during_pack");
    public static final TagKey<Block> DANGEROUS_TO_PACK = blockTag("dangerous_to_pack");
    public static final TagKey<Block> DEPLOYMENT_BLOCKERS = blockTag("deployment_blockers");
    public static final TagKey<Block> TERRITORY_STORAGE_BLOCKS = blockTag("territory_storage_blocks");
    public static final TagKey<Block> TERRITORY_DEVICE_BLOCKS = blockTag("territory_device_blocks");
    public static final TagKey<EntityType<?>> SKIP_ENTITY_CAPTURE = entityTag("skip_entity_capture");
    public static final TagKey<Item> TERRITORY_BUILD_ITEMS = itemTag("territory_build_items");

    private CoreCompat() {
    }

    public static boolean isIgnoredDuringPack(BlockState state) {
        return state.is(IGNORED_DURING_PACK);
    }

    public static boolean isDangerousToPack(BlockState state, @Nullable BlockEntity blockEntity) {
        if (state.is(DANGEROUS_TO_PACK)) {
            return true;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String namespace = id.getNamespace();
        String path = id.getPath();

        if ("touhou_little_maid".equals(namespace) && blockEntity != null) {
            return true;
        }
        if ("create".equals(namespace)) {
            for (String part : CREATE_DANGEROUS_PATH_PARTS) {
                if (path.contains(part)) {
                    return true;
                }
            }
            return false;
        }
        if (!"mekanism".equals(namespace)) {
            return false;
        }

        for (String part : MEKANISM_DANGEROUS_PATH_PARTS) {
            if (path.contains(part)) {
                return true;
            }
        }
        return false;
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, EasyAdventure.id(path));
    }

    private static TagKey<EntityType<?>> entityTag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, EasyAdventure.id(path));
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, EasyAdventure.id(path));
    }
}

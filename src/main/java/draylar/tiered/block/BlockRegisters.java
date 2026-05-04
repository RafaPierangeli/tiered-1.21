package draylar.tiered.block;

import draylar.tiered.Tiered;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.CreativeModeTabs;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.resources.ResourceKey;


public class BlockRegisters {

    public static final Block REFORGE_BLOCK = new ReforgeBlock(
            BlockBehaviour.Properties.of()
                    .sound(SoundType.ANVIL)
                    .strength(1.0f, 6.0f)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
                    .setId(ResourceKey.create(Registries.BLOCK,Identifier.fromNamespaceAndPath(Tiered.MOD_ID, "reforge_block")))
    );


    public static void registerModBlocks() {
        Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(Tiered.MOD_ID, "reforge_block"), REFORGE_BLOCK);

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.accept(REFORGE_BLOCK));


        Registry.register(
                BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath(Tiered.MOD_ID, "reforge_block"),
                new net.minecraft.world.item.BlockItem(
                        REFORGE_BLOCK,
                        new net.minecraft.world.item.Item.Properties()
                                .useBlockDescriptionPrefix()
                                .setId(ResourceKey.create(Registries.ITEM,Identifier.fromNamespaceAndPath(Tiered.MOD_ID, "reforge_block")))
                )
        );

    }

}
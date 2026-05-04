package draylar.tiered.block;

import draylar.tiered.Tiered;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;


public class BlockEntities {


    public static final BlockEntityType<ReforgeBlockEntity> REFORGE_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(ReforgeBlockEntity::new, BlockRegisters.REFORGE_BLOCK).build();


    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(Tiered.MOD_ID, "reforge_block_entity"),
                REFORGE_BLOCK_ENTITY
        );
    }

}

package draylar.tiered.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ReforgeBlockEntity extends BlockEntity {

    public ReforgeBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.REFORGE_BLOCK_ENTITY, blockPos, blockState);
    }

}

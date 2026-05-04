package draylar.tiered.mixin.access;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {


    @Accessor("lastSentHealth")
    void setLastSentHealth(float syncedHealth);
}

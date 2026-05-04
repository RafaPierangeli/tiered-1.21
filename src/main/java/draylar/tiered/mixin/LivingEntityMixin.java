package draylar.tiered.mixin;

import draylar.tiered.mixin.access.ServerPlayerAccessor;
import draylar.tiered.network.TieredServerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag; // 🌟 NOVO IMPORT
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo; // 🌟 NOVO IMPORT
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

// TODO(Ravel): can not resolve target class LivingEntity
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    // TODO(Ravel): Could not determine a single target
    @Mutable
    @Shadow
    @Final
    private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    // TODO(Ravel): Could not determine a single target
    @Shadow
    public abstract float getHealth();

    // TODO(Ravel): Could not determine a single target
    @Shadow
    public abstract float getMaxHealth();

    // TODO(Ravel): Could not determine a single target
    @Shadow
    public abstract void setHealth(float health);


    // TODO(Ravel): no target class
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readCustomDataFromNbtMixin(ValueInput view, CallbackInfo ci) {
        if (view.contains("Health")) {
            this.getEntityData().set(DATA_HEALTH_ID, view.getFloatOr("Health",getHealth()));
        }
    }

    // TODO(Ravel): no target class
    @Inject(method = "collectEquipmentChanges", at = @At(value = "TAIL"))
    private void getEquipmentChangesMixin(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if (cir.getReturnValue() != null && (Object) this instanceof ServerPlayer serverPlayerEntity) {
            this.setHealth(this.getHealth() > this.getMaxHealth() ? this.getMaxHealth() : this.getHealth());
            TieredServerPacket.writeS2CHealthPacket(serverPlayerEntity);
            ((ServerPlayerAccessor) serverPlayerEntity).setLastSentHealth(serverPlayerEntity.getHealth());
        }
    }
}
package draylar.tiered.mixin;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {

    @Unique
    private boolean isGenerated = true;
    @Unique
    private boolean isClient = true;

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void writeCustomDataToNbtMixin(ValueOutput view, CallbackInfo ci) {
        view.putBoolean("IsGenerated", this.isGenerated);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void readCustomDataMixin(ValueInput view, CallbackInfo ci) {
        // 🌟 CORREÇÃO: getBoolean retorna um boolean primitivo, não um Optional.
        // Para ter um valor padrão (true), verificamos se a chave existe primeiro.
        if (view.contains("IsGenerated")) {
            this.isGenerated = view.getBooleanOr("IsGenerated",true);
        } else {
            this.isGenerated = true;
        }
    }

    @Inject(method = "interactAt", at = @At("HEAD"))
    private void interactAt(Player player, Vec3 hitPos, InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
        this.isGenerated = false;
        this.isClient = player.level().isClientSide();
    }

    @Inject(method = "swapItem", at = @At("HEAD"))
    private void equipStackMixin(Player player, EquipmentSlot slot, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        if (!this.isClient && this.isGenerated && ConfigInit.CONFIG.lootContainerModifier) {
            ModifierUtils.setItemStackAttribute(null, stack, false);
        }
    }
}
package draylar.tiered.mixin;

import draylar.tiered.api.CustomEntityAttributes;
import draylar.tiered.util.AttributeHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {


    @Unique
    private boolean isCustomCrit = false;

    private PlayerMixin(EntityType<? extends LivingEntity> type, Level world) {
        super(type, world);
    }


    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void createPlayerAttributesMixin(CallbackInfoReturnable<AttributeSupplier.Builder> info) {
        info.getReturnValue().add(CustomEntityAttributes.CRIT_CHANCE);
        info.getReturnValue().add(CustomEntityAttributes.DIG_SPEED);
        info.getReturnValue().add(CustomEntityAttributes.DURABLE);
        info.getReturnValue().add(CustomEntityAttributes.RANGE_ATTACK_DAMAGE);
        info.getReturnValue().add(CustomEntityAttributes.CRITICAL_DAMAGE);

    }


    @ModifyVariable(method = "getDestroySpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectUtil;hasDigSpeed(Lnet/minecraft/world/entity/LivingEntity;)Z"), index = 2)
    private float getBlockBreakingSpeedMixin(float f) {
        return AttributeHelper.getExtraDigSpeed((Player) (Object) this, f);
    }


    @ModifyVariable(
            method = "attack",
            at = @At("STORE"),
            ordinal = 2
    )

    private boolean forceVanillaCrit(boolean originalBl3) {

        if (originalBl3) {
            return true;
        }

        return AttributeHelper.shouldMeeleCrit((Player) (Object) this);
    }


    @ModifyConstant(
            method = "attack",
            constant = @Constant(floatValue = 1.5f)
    )
    private float modifyCritDamageMultiplier(float originalMultiplier) {

        double bonusCritDamage = this.getAttributeValue(CustomEntityAttributes.CRITICAL_DAMAGE);

        return (float) (bonusCritDamage);

    }
}






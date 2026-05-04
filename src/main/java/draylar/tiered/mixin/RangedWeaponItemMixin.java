package draylar.tiered.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import draylar.tiered.util.AttributeHelper;

@Mixin(AbstractArrow.class)
public abstract class RangedWeaponItemMixin {

// 🌟 1. A NOSSA MATEMÁTICA (Range Damage + Custom Crit)
    @ModifyVariable(
            method = "onHitEntity",
            at = @At(value = "STORE", ordinal = 0),
            ordinal = 0
    )
    private int modifyBaseDamage(int originalDamage) {
        AbstractArrow projectile = (AbstractArrow) (Object) this;
        Entity owner = projectile.getOwner();

        if (owner instanceof Player player) {
            float newDamage = originalDamage;

            newDamage = AttributeHelper.getExtraRangeDamage(player, newDamage);

            boolean isVanillaCrit = projectile.isCritArrow();
            boolean isArpgCrit = AttributeHelper.shouldMeeleCrit(player);

            if (isVanillaCrit || isArpgCrit) {
                // Aplica o SEU multiplicador (ex: 4x)
                newDamage = AttributeHelper.getExtraCritDamage(player, newDamage);

                if (!isVanillaCrit) {
                    projectile.setCritArrow(true); // Garante as partículas
                }
            }

            return Math.round(newDamage);
        }

        return originalDamage;
    }

// 🌟 2. O HACK: DESATIVANDO O CRÍTICO VANILLA PARA JOGADORES
    @Redirect(
            method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;isCritArrow()Z")
    )
    private boolean disableVanillaCritDamage(AbstractArrow projectile) {
        // Quando o Minecraft for calcular o dano extra de 50%, nós interceptamos a pergunta.
        if (projectile.getOwner() instanceof Player) {
            // Dizemos "false". O Vanilla não vai adicionar dano nenhum!
            // Como isso só afeta o cálculo de dano, a flecha CONTINUA soltando partículas no ar!
            return false;
        }
        // Se for um Esqueleto atirando, deixamos o Vanilla agir normalmente.
        return projectile.isCritArrow();
    }
}
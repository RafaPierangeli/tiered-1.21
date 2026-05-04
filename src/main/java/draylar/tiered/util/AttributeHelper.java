package draylar.tiered.util;

import draylar.tiered.api.CustomEntityAttributes; // Ajuste se o seu se chamar TieredAttributes
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public class AttributeHelper {

    // 🌟 1. CHANCE DE CRÍTICO (Agora de 0 a 100%)
    public static boolean shouldMeeleCrit(Player playerEntity) {
        AttributeInstance instance = playerEntity.getAttribute(CustomEntityAttributes.CRIT_CHANCE);
        if (instance != null) {
            double critChance = instance.getValue();
            return playerEntity.getRandom().nextDouble() < critChance;
        }
        return false;
    }

    // 🌟 2. DANO CRÍTICO (Adeus gambiarra, olá atributo novo!)
    public static float getExtraCritDamage(Player playerEntity, float oldDamage) {
        // Agora usamos o SEU novo atributo de Dano Crítico!
        AttributeInstance instance = playerEntity.getAttribute(CustomEntityAttributes.CRITICAL_DAMAGE);
        if (instance != null) {

            float bonusMultiplier = (float) instance.getValue();

            return oldDamage + (oldDamage * bonusMultiplier);
        }
        return oldDamage;
    }

    // 🌟 3. VELOCIDADE DE MINERAÇÃO (Mantido o cálculo original para não quebrar compatibilidade)
    public static float getExtraDigSpeed(Player playerEntity, float oldDigSpeed) {
        AttributeInstance instance = playerEntity.getAttribute(CustomEntityAttributes.DIG_SPEED);
        if (instance != null) {
            float extraDigSpeed = oldDigSpeed;
            for (AttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.amount();

                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    extraDigSpeed += amount;
                } else {
                    extraDigSpeed *= (amount + 1);
                }
            }
            return extraDigSpeed;
        }
        return oldDigSpeed;
    }

    // 🌟 4. DANO À DISTÂNCIA (Arcos/Bestas)
    public static float getExtraRangeDamage(Player playerEntity, float oldDamage) {
        AttributeInstance instance = playerEntity.getAttribute(CustomEntityAttributes.RANGE_ATTACK_DAMAGE);
        if (instance != null) {
            float rangeDamage = oldDamage;
            for (AttributeModifier modifier : instance.getModifiers()) {
                float amount = (float) modifier.amount();

                if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    rangeDamage += amount;
                } else {
                    rangeDamage *= (amount + 1.0f);
                }
            }
            return Math.min(rangeDamage, Integer.MAX_VALUE);
        }
        return oldDamage;
    }
}
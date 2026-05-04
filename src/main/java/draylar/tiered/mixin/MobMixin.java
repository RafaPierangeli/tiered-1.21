package draylar.tiered.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity; // 🌟 NOVO IMPORT
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;

import java.util.Set;
import java.util.function.Predicate;


@Mixin(Mob.class)
public class MobMixin {


    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void initializeMixin(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData, CallbackInfoReturnable<SpawnGroupData> info) {
        if (ConfigInit.CONFIG.entityItemModifier) {
            // 🌟 CORREÇÃO: Convertendo 'this' para LivingEntity para acessar o método nativo
            LivingEntity livingEntity = (LivingEntity) (Object) this;

            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                // 🌟 CORREÇÃO: Chamando o metodo diretamente da LivingEntity
                ItemStack itemStack = livingEntity.getItemBySlot(equipmentSlot);
                if (itemStack.isEmpty()) {
                    continue;
                }
                ModifierUtils.setItemStackAttribute(null, itemStack, false);
            }
        }
    }


// 🌟 A NOVA ESTRATÉGIA: Injetamos no exato momento da morte, antes do loot cair.
        @Inject(method = "dropPreservedEquipment(Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Predicate;)Ljava/util/Set;", at = @At("HEAD"))
        private void onDropEquipment(ServerLevel serverLevel, Predicate<ItemStack> predicate, CallbackInfoReturnable<Set<EquipmentSlot>> cir) {

            // Verifica se a configuração do mod permite modificar itens de entidades
            if (ConfigInit.CONFIG.entityDropModifier) {
                Mob mob = (Mob) (Object) this;

                // Passamos por todos os slots (Mão, Capacete, Peitoral, etc)
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = mob.getItemBySlot(slot);

                    // Se o mob estiver segurando/vestindo algo, aplicamos o Tier na hora!
                    if (!stack.isEmpty()) {
                        ModifierUtils.setItemStackAttribute(null, stack, false);
                    }
                }
            }
        }

    // 🌟 CORREÇÃO: Removemos o @Shadow problemático! Não precisamos mais dele.
}

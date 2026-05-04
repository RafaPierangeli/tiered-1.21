package draylar.tiered.mixin;

import draylar.tiered.Tiered;
import draylar.tiered.api.AttributeTemplate;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BiConsumer;

// TODO(Ravel): can not resolve target class ItemStack
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {


    // TODO(Ravel): no target class
    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("RETURN"))
    private void applyAttributeModifiersMixin(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo info) {
        ItemStack itemStack = (ItemStack) (Object) this;

        if (itemStack.get(Tiered.TIER) != null) {
            Identifier tier = ModifierUtils.getAttributeId(itemStack);
            PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            if (potentialAttribute != null) {
                for (AttributeTemplate template : potentialAttribute.getAttributes()) {

                    // 1. Processa os Slots Obrigatórios
                    List<EquipmentSlot> requiredSlots = template.getRequiredEquipmentSlots();
                    if (requiredSlots != null && requiredSlots.contains(slot)) {
                        if (Tiered.isPreferredEquipmentSlot(itemStack, slot)) {
                            template.applyModifiers(slot, consumer);
                        }
                    }

                    // 2. Processa os Slots Opcionais
                    List<EquipmentSlot> optionalSlots = template.getOptionalEquipmentSlots();
                    if (optionalSlots != null && optionalSlots.contains(slot)) {
                        if (Tiered.isPreferredEquipmentSlot(itemStack, slot)) {
                            template.applyModifiers(slot, consumer);
                        }
                    }
                }
            }
        }
    }
}
package draylar.tiered.api;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

public class CustomEntityAttributes {

    public static final Holder<Attribute> DIG_SPEED = register("tiered:dig_speed",
            new RangedAttribute("attribute.name.tiered.dig_speed", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final Holder<Attribute> CRIT_CHANCE = register("tiered:critical_chance",
            new RangedAttribute("attribute.name.tiered.critical_chance", 0.0, 0.0, 1.0).setSyncable(true));
    public static final Holder<Attribute> DURABLE = register("tiered:durable",
            new RangedAttribute("attribute.name.tiered.durable", 0.0D, 0.0D, 1D).setSyncable(true));
    public static final Holder<Attribute> RANGE_ATTACK_DAMAGE = register("tiered:range_attack_damage",
            new RangedAttribute("attribute.name.tiered.range_attack_damage", 0.0D, 0.0D, 2048.0D).setSyncable(true));
    public static final Holder<Attribute> CRITICAL_DAMAGE = register("tiered:critical_damage",
            new RangedAttribute("attribute.name.tiered.critical_damage", 1.5, 0.0, 5.0).setSyncable(true));

    public static void init() {
    }

    private static Holder<Attribute> register(String id, Attribute attribute) {
        return Registry.registerForHolder(BuiltInRegistries.ATTRIBUTE, Identifier.parse(id), attribute);
    }
}
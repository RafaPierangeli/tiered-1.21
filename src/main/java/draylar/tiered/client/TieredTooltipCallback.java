package draylar.tiered.client;

import com.mojang.blaze3d.platform.Window;
import draylar.tiered.Tiered;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.config.AttributeColorMode;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.config.TooltipDisplayMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.locale.Language;
import org.lwjgl.glfw.GLFW;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

@Environment(EnvType.CLIENT)
public class TieredTooltipCallback {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private static String[] extractIconAndName(String translationKey) {
        String rawTranslated = Language.getInstance().getOrDefault(translationKey);
        if (rawTranslated == null) rawTranslated = translationKey;

        String cleanTranslated = rawTranslated.replaceAll("§[0-9a-fk-or]", "");

        for (int i = 0; i < cleanTranslated.length(); ) {
            int cp = cleanTranslated.codePointAt(i);
            if ((cp >= 0xE000 && cp <= 0xF8FF) || (cp >= 0xF900 && cp <= 0xFAFF) ||
                    (cp >= 0x1CD00 && cp <= 0x1CDFF) || (cp >= 0x1FB00 && cp <= 0x1FBFF) ||
                    (cp >= 0xF0000 && cp <= 0xFFFFD) || (cp >= 0x100000 && cp <= 0x10FFFD)) {

                int len = Character.charCount(cp);
                String icon = new String(Character.toChars(cp));
                String nameWithoutIcon = cleanTranslated.substring(i + len).trim();
                return new String[]{icon, nameWithoutIcon};
            }
            i += Character.charCount(cp);
        }
        return new String[]{"", cleanTranslated};
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {

            if (stack.get(Tiered.TIER) != null) {
                String rawTierId = stack.get(Tiered.TIER).tier();
                Identifier tierId = Identifier.parse(rawTierId);

                PotentialAttribute potentialAttribute = Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);

                if (potentialAttribute != null) {

                    TooltipDisplayMode displayMode = ConfigInit.CONFIG.uniqueTooltipMode;

                    if (displayMode == TooltipDisplayMode.OFF) {
                        return;
                    }

                    // Verifica os mods carregados no início
                    boolean hasDynamicTooltip = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("dynamictooltips");
                    boolean isBetterCombatLoaded = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("bettercombat");

                    if (displayMode == TooltipDisplayMode.ON_SHIFT) {
                        Window window = Minecraft.getInstance().getWindow();

                        boolean isShiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
                                InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
                        boolean isCtrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL) ||
                                InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);

                        // Lógica de compatibilidade: Se Dynamic Tooltips estiver presente, exige CTRL. Senão, exige SHIFT.
                        boolean shouldShow = hasDynamicTooltip ? isCtrlDown : isShiftDown;

                        if (!shouldShow) {
                            lines.add(Component.empty());
                            if (hasDynamicTooltip) {
                                lines.add(Component.translatable("tiered.tooltip.press_ctrl").withStyle(ChatFormatting.DARK_GRAY));
                            } else {
                                lines.add(Component.translatable("tiered.tooltip.press_shift").withStyle(ChatFormatting.DARK_GRAY));
                            }
                            return;
                        }
                    }

                    // 1. Preservar Nome, Lore e Encantamentos (Agora roda sempre, independente do Dynamic Tooltips)
                    List<Component> preservedLines = new ArrayList<>();
                    for (Component line : lines) {
                        if (line.getContents() instanceof TranslatableContents translatable) {
                            if (translatable.getKey().startsWith("item.modifiers.")) {
                                break;
                            }
                        }
                        preservedLines.add(line);
                    }
                    lines.clear();
                    lines.addAll(preservedLines);

                    String attrMargin = "";
                    ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);

                    // 🌟 PASSO 1: CÁLCULO DOS TOTAIS (Matemática exata do Minecraft)
                    double baseDamage = 1.0; // Dano base do jogador com a mão vazia
                    double baseSpeed = 4.0;  // Velocidade base do jogador com a mão vazia

                    double damageAdd = 0.0;
                    double speedAdd = 0.0;

                    double damageMultBase = 0.0;
                    double speedMultBase = 0.0;

                    double damageMultTotal = 1.0;
                    double speedMultTotal = 1.0;

                    if (modifiers != null) {
                        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                            Identifier attrId = BuiltInRegistries.ATTRIBUTE.getKey(entry.attribute().value());
                            if (attrId != null) {
                                String path = attrId.getPath();
                                double val = entry.modifier().amount();
                                AttributeModifier.Operation op = entry.modifier().operation();

                                if (path.equals("attack_damage")) {
                                    if (op == AttributeModifier.Operation.ADD_VALUE) {
                                        damageAdd += val;
                                    } else if (op == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                                        damageMultBase += val;
                                    } else if (op == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                                        damageMultTotal *= (1.0 + val);
                                    }
                                } else if (path.equals("attack_speed")) {
                                    if (op == AttributeModifier.Operation.ADD_VALUE) {
                                        speedAdd += val;
                                    } else if (op == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                                        speedMultBase += val;
                                    } else if (op == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                                        speedMultTotal *= (1.0 + val);
                                    }
                                }
                            }
                        }
                    }

// Fórmula oficial do Minecraft: (Base + Add) * (1 + MultBase) * MultTotal
                    double totalDamage = (baseDamage + damageAdd) * (1.0 + damageMultBase) * damageMultTotal;
                    double totalSpeed = (baseSpeed + speedAdd) * (1.0 + speedMultBase) * speedMultTotal;

                    // 🌟 PASSO 2: O DISFARCE VANILLA
                    if (modifiers != null) {
                        Map<EquipmentSlotGroup, List<ItemAttributeModifiers.Entry>> vanillaModifiers = new HashMap<>();

                        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                            if (!entry.modifier().id().getNamespace().equals("tiered")) {
                                vanillaModifiers.computeIfAbsent(entry.slot(), k -> new ArrayList<>()).add(entry);
                            }
                        }

                        boolean isFirstVanillaGroup = true;

                        for (Map.Entry<EquipmentSlotGroup, List<ItemAttributeModifiers.Entry>> group : vanillaModifiers.entrySet()) {

                            if (!isFirstVanillaGroup) {
                                lines.add(Component.empty());
                            }
                            isFirstVanillaGroup = false;

                            lines.add(Component.translatable("item.modifiers." + group.getKey().getSerializedName()).withStyle(ChatFormatting.GRAY));

                            for (ItemAttributeModifiers.Entry entry : group.getValue()) {
                                AttributeModifier modifier = entry.modifier();
                                Holder<Attribute> attributeEntry = entry.attribute();

                                boolean isDamage = modifier.id().equals(Identifier.parse("base_attack_damage"));
                                boolean isSpeed = modifier.id().equals(Identifier.parse("base_attack_speed"));

                                String translationKey = attributeEntry.value().getDescriptionId();

                                String[] iconAndName = extractIconAndName(translationKey);
                                String icon = iconAndName[0];
                                String cleanName = iconAndName[1];

                                // Cria a linha base
                                MutableComponent finalLine = Component.literal(attrMargin);

                                // Adiciona o ícone com Formatting.WHITE para não herdar cores e manter a cor da imagem
                                if (!icon.isEmpty()) {
                                    finalLine.append(Component.literal(icon + " ").withStyle(ChatFormatting.WHITE));
                                }

                                // Cria o corpo do texto (Valor + Nome) separadamente
                                MutableComponent body;
                                if (isDamage) {
                                    body = Component.translatable("attribute.modifier.equals.0", DECIMAL_FORMAT.format(totalDamage), Component.literal(cleanName));
                                    body.withStyle(ChatFormatting.DARK_GREEN);
                                } else if (isSpeed) {
                                    body = Component.translatable("attribute.modifier.equals.0", DECIMAL_FORMAT.format(totalSpeed), Component.literal(cleanName));
                                    body.withStyle(ChatFormatting.DARK_GREEN);
                                } else {
                                    double value = modifier.amount();
                                    if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE ||
                                            modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                                        value = value * 100.0;
                                    }
                                    boolean isPositive = value > 0;
                                    String opId = String.valueOf(modifier.operation().id());
                                    String plusOrTake = isPositive ? "plus" : "take";

                                    body = Component.translatable("attribute.modifier." + plusOrTake + "." + opId, DECIMAL_FORMAT.format(Math.abs(value)), Component.literal(cleanName));
                                    body.withStyle(isPositive ? ChatFormatting.BLUE : ChatFormatting.RED);
                                }

                                // Junta o corpo colorido ao ícone neutro
                                finalLine.append(body);
                                lines.add(finalLine);
                            }
                        }

                        // 🌟 PASSO 3: ATRIBUTOS ARPG
                        boolean addedHeader = false;
                        Set<Identifier> drawnModifiers = new HashSet<>();

                        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
                            AttributeModifier modifier = entry.modifier();
                            Identifier modId = modifier.id();
                            Holder<Attribute> attributeEntry = entry.attribute();

                            if (modId.getNamespace().equals("tiered") && !drawnModifiers.contains(modId)) {

                                if (!addedHeader) {
                                    lines.add(Component.empty());
                                    lines.add(Component.literal(" ").append(Component.translatable("tiered.tooltip.tier_attributes")).withStyle(ChatFormatting.GRAY));
                                    addedHeader = true;
                                }

                                Identifier attrId = BuiltInRegistries.ATTRIBUTE.getKey(attributeEntry.value());
                                boolean isPercentageAttribute = attrId != null && (attrId.equals(Identifier.fromNamespaceAndPath("tiered", "critical_chance")));

                                double value = modifier.amount();

                                if (isPercentageAttribute && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                                    value = value * 100.0;
                                }

                                if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE ||
                                        modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                                    value = value * 100.0;
                                }

                                boolean isPositive = value > 0;
                                String sign = isPositive ? "+" : "";
                                String percent = (isPercentageAttribute || modifier.operation() != AttributeModifier.Operation.ADD_VALUE) ? "%" : "";

                                String translationKey = attributeEntry.value().getDescriptionId();
                                if (isBetterCombatLoaded && attrId != null && attrId.getPath().equals("entity_interaction_range")) {
                                    translationKey = "attribute.name.generic.attack_range";
                                }

                                String[] iconAndName = extractIconAndName(translationKey);
                                String icon = iconAndName[0];
                                String cleanName = iconAndName[1];

                                // Cria a linha base
                                MutableComponent finalLine = Component.literal(attrMargin);

                                // Adiciona o ícone com Formatting.WHITE
                                if (!icon.isEmpty()) {
                                    finalLine.append(Component.literal(icon + " ").withStyle(ChatFormatting.WHITE));
                                }

                                // Cria o texto do atributo separadamente para receber a cor do Tier
                                MutableComponent attributeText = Component.literal(sign + DECIMAL_FORMAT.format(value) + percent + " ")
                                        .append(Component.literal(cleanName));

                                AttributeColorMode mode = ConfigInit.CONFIG.attributeColorMode;
                                if (mode == AttributeColorMode.TIER_COLOR) {
                                    if (isPositive) {
                                        attributeText.setStyle(potentialAttribute.getStyle());
                                    } else {
                                        attributeText.withStyle(ChatFormatting.RED);
                                    }
                                } else {
                                    ChatFormatting color;
                                    if (mode == AttributeColorMode.GREEN_RED) {
                                        color = isPositive ? ChatFormatting.GREEN : ChatFormatting.RED;
                                    } else {
                                        color = isPositive ? ChatFormatting.BLUE : ChatFormatting.RED;
                                    }
                                    attributeText.withStyle(color);
                                }

                                // Junta o texto colorido ao ícone neutro
                                finalLine.append(attributeText);
                                lines.add(finalLine);
                            }
                        }
                    }

                    // 🌟 PASSO 4: DURABILIDADE DA ARMA
                    if (stack.isDamageableItem()) {
                        int maxDamage = stack.getMaxDamage();
                        int currentDamage = stack.getDamageValue();
                        int remainingDamage = maxDamage - currentDamage;

                        double durabilityPercent = (double) remainingDamage / maxDamage;
                        ChatFormatting durabilityColor;

                        if (durabilityPercent <= 0.05) {
                            durabilityColor = ChatFormatting.DARK_RED;
                        } else if (durabilityPercent <= 0.10) {
                            durabilityColor = ChatFormatting.RED;
                        } else if (durabilityPercent <= 0.25) {
                            durabilityColor = ChatFormatting.GOLD;
                        } else if (durabilityPercent <= 0.50) {
                            durabilityColor = ChatFormatting.YELLOW;
                        } else if (durabilityPercent <= 0.75) {
                            durabilityColor = ChatFormatting.GREEN;
                        } else {
                            durabilityColor = ChatFormatting.DARK_GREEN;
                        }

                        lines.add(Component.empty());
                        MutableComponent durabilityText = Component.literal(attrMargin)
                                .append(Component.translatable("tiered.tooltip.durability").append(": ").withStyle(ChatFormatting.GRAY))
                                .append(Component.literal(remainingDamage + " / " + maxDamage).withStyle(durabilityColor));

                        lines.add(durabilityText);
                    }
                }
            }
        });
    }
}
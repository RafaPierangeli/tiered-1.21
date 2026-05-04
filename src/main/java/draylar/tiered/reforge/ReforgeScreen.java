package draylar.tiered.reforge;

import java.text.DecimalFormat;
import java.util.*;

import com.mojang.blaze3d.systems.RenderSystem;
import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.TieredItemTags;
import draylar.tiered.config.ConfigInit;
import draylar.tiered.network.TieredClientPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Holder;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

@Environment(EnvType.CLIENT)
public class ReforgeScreen extends AbstractContainerScreen<ReforgeScreenHandler> {

    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("tiered", "textures/gui/reforging_screen3.png");
    public static final Identifier REFORGE_UNIQUE = Identifier.fromNamespaceAndPath("tiered", "textures/gui/reforging_unique.png");
    public static final Identifier REFORGE_MYTHIC = Identifier.fromNamespaceAndPath("tiered", "textures/gui/reforging_mythic.png");

    public ReforgeScreen.ReforgeButton reforgeButton;
    private ItemStack last;
    private List<Item> baseItems;

    // falha aqui
    // 🌟 Formatador para as porcentagens e sorte
    private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");

    // 🌟 VARIÁVEIS DA ANIMAÇÃO (Sucesso e Falha)
    private boolean expectingReforge = false;
    private int lastIngredientCount = 0;
    private Identifier lastTier = null;
    private Component floatingText = null;
    private int floatingTick = 0;

    public ReforgeScreen(ReforgeScreenHandler handler, Inventory playerInventory, Component title) {
        super(handler, playerInventory, title);
        this.titleLabelX = 8;
        // 🌟 UX: Abaixando o título da Forja (O padrão é 6, mudamos para 12 ou 14)
        this.titleLabelY = 8;

        // 🌟 UX: Garantindo que o título do seu inventário ("Inventário") fique no lugar certo
        this.inventoryLabelX = 8;
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;

        this.reforgeButton = this.addRenderableWidget(new ReforgeButton(i + 79, j + 56, (button) -> {
            if (button instanceof ReforgeButton reforgeBtn && !reforgeBtn.disabled) {
                TieredClientPacket.writeC2SReforgePacket();
                this.expectingReforge = true;
                this.lastIngredientCount = this.menu.getSlot(0).getItem().getCount();
                this.lastTier = ModifierUtils.getAttributeId(this.menu.getSlot(1).getItem());
            }
        }));
    }

    // 🌟 NOVO: Atualiza o botão a cada frame do jogo

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.reforgeButton != null) {
            this.reforgeButton.setDisabled(!this.menu.isReforgeReady());
        }

        // 🌟 LÓGICA DE SUCESSO OU FALHA
        if (this.expectingReforge) {
            ItemStack currentIngredient = this.menu.getSlot(0).getItem();
            ItemStack currentWeapon = this.menu.getSlot(1).getItem();

            // Se a quantidade de ingredientes diminuiu, o servidor processou a reforja!
            if (currentIngredient.getCount() < this.lastIngredientCount || (this.lastIngredientCount > 0 && currentIngredient.isEmpty())) {
                Identifier currentTier = ModifierUtils.getAttributeId(currentWeapon);

                // Compara o Tier novo com o Tier da "foto"
                if (currentTier != null && !currentTier.equals(this.lastTier)) {
                    // SUCESSO! O Tier mudou.
                    this.floatingText = Component.literal("✨ ").append(currentWeapon.getHoverName()).append(" ✨");
                } else {
                    // FALHA! Gastou o item mas o Tier continuou o mesmo.
                    this.floatingText = Component.literal("✨ ").append(currentWeapon.getHoverName()).append(" ✨");
                }

                this.floatingTick = 40; // Inicia a animação (2 segundos)
                this.expectingReforge = false; // Desliga a espera
            }
        }

        if (this.floatingTick > 0) {
            this.floatingTick--;
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.renderTooltip(context, mouseX, mouseY);

        // 🌟 DESENHA O SISTEMA DE SORTE E CHANCES (Chamada Nova)
        this.renderLuckAndChances(context, mouseX, mouseY);

        // Lógica do Tooltip quando passa o mouse no botão
        if (this.isHovering(79, 56, 18, 18, (double) mouseX, (double) mouseY)) {
            ItemStack itemStack = this.menu.getSlot(1).getItem();
            List<Component> tooltip = new ArrayList<Component>();

            // Verifica se o item é Único e se a config bloqueia a reforja dele
            Identifier tierId = ModifierUtils.getAttributeId(itemStack);
            boolean isUniqueLocked = tierId != null && tierId.getPath().contains("unique") && !ConfigInit.CONFIG.uniqueReforge;
            boolean isMythicLocked = tierId != null && tierId.getPath().contains("mythic");

            if (itemStack.isEmpty()) {
                // ESTADO 1: Mesa vazia
                tooltip.add(Component.translatable("screen.tiered.reforge_insert_equipment").withStyle(ChatFormatting.YELLOW));
            }
            else if (itemStack.is(TieredItemTags.MODIFIER_RESTRICTED)) {
                // ESTADO 2: Item proibido
                tooltip.add(Component.translatable("screen.tiered.reforge_restricted").withStyle(ChatFormatting.RED));
            }
            else if (isUniqueLocked) {
                // 🌟 ESTADO 3: ITEM ÚNICO BLOQUEADO!
                // Mostra uma mensagem roxa e em negrito, e ignora o resto (XP, ingredientes)
                tooltip.add(Component.translatable("screen.tiered.reforge_unique_locked").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            }
            else if (isMythicLocked) {
                // 🌟 ESTADO 3: ITEM MITICO BLOQUEADO!
                // Mostra uma mensagem VERMELHA e em negrito, e ignora o resto (XP, ingredientes)
                tooltip.add(Component.translatable("screen.tiered.reforge_mythic_locked").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            }
            else {
                // ESTADO 4: Equipamento válido! Checa o que falta.

                if (itemStack != last) {
                    last = itemStack;
                    baseItems = new ArrayList<Item>();
                    List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(itemStack.getItem());

                    if (!items.isEmpty()) {
                        baseItems.addAll(items);
                    } else {
                        var repairable = itemStack.get(DataComponents.REPAIRABLE);
                        if (repairable != null && repairable.items() != null) {
                            for (Holder<Item> entry : repairable.items()) {
                                baseItems.add(entry.value());
                            }
                        } else {
                            for (Holder<Item> itemRegistryEntry : BuiltInRegistries.ITEM.getOrThrow(TieredItemTags.REFORGE_BASE_ITEM)) {
                                baseItems.add(itemRegistryEntry.value());
                            }
                        }
                    }
                }

                // Checa Ingrediente Base
                if (!baseItems.isEmpty()) {
                    ItemStack ingredient = this.getMenu().getSlot(0).getItem();
                    if (ingredient.isEmpty() || !baseItems.contains(ingredient.getItem())) {
                        tooltip.add(Component.translatable("screen.tiered.reforge_ingredient").withStyle(ChatFormatting.RED));
                        for (Item item : baseItems) {
                            tooltip.add(Component.literal(" - ").append(item.getName()).withStyle(ChatFormatting.GRAY));
                        }
                    }
                }

                // Checa Catalisador
                ItemStack addition = this.getMenu().getSlot(2).getItem();
                if (addition.isEmpty() || !addition.is(TieredItemTags.REFORGE_ADDITION)) {
                    tooltip.add(Component.translatable("screen.tiered.reforge_addition").withStyle(ChatFormatting.RED));
                }

                // Checa Dano
                if (itemStack.isDamageableItem() && itemStack.isDamaged()) {
                    tooltip.add(Component.translatable("screen.tiered.reforge_damaged").withStyle(ChatFormatting.RED));
                }

                // Checa XP
                if (this.minecraft != null && this.minecraft.player != null) {
                    int xpCost = ConfigInit.CONFIG.reforgeXpCost;

                    if (this.minecraft.player.totalExperience < xpCost && !this.minecraft.player.isCreative()) {
                        tooltip.add(Component.translatable("screen.tiered.reforge_xp_missing", xpCost).withStyle(ChatFormatting.RED));
                    } else {
                        tooltip.add(Component.translatable("screen.tiered.reforge_xp_cost", xpCost).withStyle(ChatFormatting.GREEN));
                    }
                }
            }

            if (!tooltip.isEmpty()) {
                context.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
            }
        }

        // Desenha o cadeado se for único e a config não permitir
        if (!ConfigInit.CONFIG.uniqueReforge && !this.getMenu().getSlot(1).getItem().isEmpty()) {
            Identifier attrId = ModifierUtils.getAttributeId(this.getMenu().getSlot(1).getItem());
            if (attrId != null && attrId.getPath().contains("unique")) {
                context.blit(RenderPipelines.GUI_TEXTURED, REFORGE_UNIQUE, this.leftPos + 75, this.topPos + 30, 0, 0, 26, 25, 26, 25);
            }
        }
        // Desenha o cadeado se for Mythic e a config não permitir
        if (!this.getMenu().getSlot(1).getItem().isEmpty()) {
            Identifier attrId = ModifierUtils.getAttributeId(this.getMenu().getSlot(1).getItem());
            if (attrId != null && attrId.getPath().contains("mythic")) {
                context.blit(RenderPipelines.GUI_TEXTURED, REFORGE_MYTHIC, this.leftPos + 75, this.topPos + 30, 0, 0, 26, 25, 26, 25);
            }
        }

        // ... (seu código de tooltip existente) ...

        // 🌟 DESENHA O TEXTO FLUTUANTE ANIMADO
        if (this.floatingTick > 0 && this.floatingText != null) {
            // Calcula o progresso da animação (de 0.0 a 1.0)
            float progress = (40 - this.floatingTick + delta) / 40.0f;

            int textX = this.width / 2;
            // Começa perto do item e sobe 40 pixels suavemente
            int textY = (this.height / 2) - 40 - (int)(progress * 40);

            // Calcula a transparência (Fade out) - Fica invisível no final
            int alpha = (int) ((1.0f - progress) * 255);
            alpha = Math.max(5, Math.min(255, alpha)); // Trava entre 5 e 255
            int color = (alpha << 24) | 0xFFFFFF; // Aplica a transparência

            context.pose().pushMatrix();

            context.drawCenteredString(this.font, this.floatingText, textX, textY, color);

            context.pose().popMatrix();
        }

    }



    // =================================================================
    // 🌟 O SISTEMA DE SORTE E TOOLTIP DINÂMICA (Sincronizado com o Servidor)
    // =================================================================
    private void renderLuckAndChances(GuiGraphics context, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return;

        double luck = this.minecraft.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK);
        String luckText = "🍀 " + PERCENT_FORMAT.format(luck);

        int textX = this.leftPos + 142;
        int textY = this.topPos + 8; // Se quiser descer a sorte também, mude o 8 para 12
        int textWidth = this.font.width(luckText);
        int textHeight = this.font.lineHeight;

        // 🌟 UX: Diminuindo o tamanho do texto da Sorte (80% do tamanho original)
        float scale = 0.7f;

        context.pose().pushMatrix();
        context.pose().translate(textX, textY);
        context.pose().scale(scale, scale);
        context.drawString(this.font, luckText, 0, 0, 0xFF55FF55, true);
        context.pose().popMatrix();

        // Ajusta a área de colisão do mouse para o novo tamanho do texto
        int scaledWidth = (int) (textWidth * scale);
        int scaledHeight = (int) (textHeight * scale);

        if (mouseX >= textX && mouseX <= textX + scaledWidth && mouseY >= textY && mouseY <= textY + scaledHeight) {

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("tiered.tooltip.reforge_chance").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));

            double reforgeMod = ConfigInit.CONFIG.reforgeModifier;
            double luckMod = ConfigInit.CONFIG.luckReforgeModifier;

            class DynamicTier {
                String keyword;
                Component name;
                ChatFormatting color;
                double currentTotalWeight = 0.0;

                DynamicTier(String keyword, Component name, ChatFormatting color) {
                    this.keyword = keyword;
                    this.name = name;
                    this.color = color;
                }
            }

            DynamicTier[] tiers = {
                    new DynamicTier("common", Component.translatable("tiered.tooltip.reforge_common"), ChatFormatting.WHITE),
                    new DynamicTier("uncommon", Component.translatable("tiered.tooltip.reforge_uncommon"), ChatFormatting.GREEN),
                    new DynamicTier("rare", Component.translatable("tiered.tooltip.reforge_rare"), ChatFormatting.BLUE),
                    new DynamicTier("epic", Component.translatable("tiered.tooltip.reforge_epic"), ChatFormatting.DARK_PURPLE),
                    new DynamicTier("legendary", Component.translatable("tiered.tooltip.reforge_legendary"), ChatFormatting.GOLD),
                    new DynamicTier("unique", Component.translatable("tiered.tooltip.reforge_unique"), ChatFormatting.LIGHT_PURPLE),
                    new DynamicTier("mythic", Component.translatable("tiered.tooltip.reforge_mythic"), ChatFormatting.AQUA)
            };

            var allAttributes = draylar.tiered.Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes();
            if (allAttributes.isEmpty()) return;

            net.minecraft.world.item.ItemStack equipmentStack = net.minecraft.world.item.ItemStack.EMPTY;
            net.minecraft.world.item.ItemStack resultStack = net.minecraft.world.item.ItemStack.EMPTY;

            int customSlotCount = 0;
            for (Slot slot : this.menu.slots) {
                if (!(slot.container instanceof Inventory)) {
                    if (customSlotCount == 1) equipmentStack = slot.getItem();
                    if (customSlotCount == 2) resultStack = slot.getItem();
                    customSlotCount++;
                }
            }

            if (equipmentStack.isEmpty() && !resultStack.isEmpty()) {
                equipmentStack = resultStack;
            }

            net.minecraft.resources.Identifier itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(equipmentStack.getItem());
            boolean hasItem = !equipmentStack.isEmpty();

            // 🌟 UX: Separação Cirúrgica das Armaduras
            String categoryKey = "tiered.tooltip.category.global";

            if (hasItem) {
                if (equipmentStack.is(ItemTags.SWORDS)) categoryKey = "tiered.tooltip.category.sword";
                else if (equipmentStack.is(ItemTags.PICKAXES)) categoryKey = "tiered.tooltip.category.pickaxe";
                else if (equipmentStack.is(ItemTags.AXES)) categoryKey = "tiered.tooltip.category.axe";
                else if (equipmentStack.is(ItemTags.SHOVELS)) categoryKey = "tiered.tooltip.category.shovel";
                else if (equipmentStack.is(ItemTags.HOES)) categoryKey = "tiered.tooltip.category.hoe";
                    // Separação das peças de armadura
                else if (equipmentStack.is(ItemTags.HEAD_ARMOR)) categoryKey = "tiered.tooltip.category.helmet";
                else if (equipmentStack.is(ItemTags.CHEST_ARMOR)) categoryKey = "tiered.tooltip.category.chestplate";
                else if (equipmentStack.is(ItemTags.LEG_ARMOR)) categoryKey = "tiered.tooltip.category.leggings";
                else if (equipmentStack.is(ItemTags.FOOT_ARMOR)) categoryKey = "tiered.tooltip.category.boots";
                    // Restante
                else if (equipmentStack.is(Items.BOW)) categoryKey = "tiered.tooltip.category.bow";
                else if (equipmentStack.is(Items.CROSSBOW)) categoryKey = "tiered.tooltip.category.crossbow";
                else if (equipmentStack.is(Items.SHIELD)) categoryKey = "tiered.tooltip.category.shield";
                else if (equipmentStack.is(Items.TRIDENT)) categoryKey = "tiered.tooltip.category.trident";
                else if (equipmentStack.is(Items.ELYTRA)) categoryKey = "tiered.tooltip.category.elytra";
                else if (equipmentStack.is(Items.MACE)) categoryKey = "tiered.tooltip.category.mace";
                else categoryKey = "tiered.tooltip.category.item";
            }

            java.util.Map<net.minecraft.resources.Identifier, draylar.tiered.api.PotentialAttribute> validAttributes = new java.util.HashMap<>();
            for (var entry : allAttributes.entrySet()) {
                if (!hasItem || entry.getValue().isValid(itemId)) {
                    validAttributes.put(entry.getKey(), entry.getValue());
                }
            }

            if (validAttributes.isEmpty()) {
                categoryKey = "tiered.tooltip.category.global";
                for (var entry : allAttributes.entrySet()) {
                    validAttributes.put(entry.getKey(), entry.getValue());
                }
            }

            tooltip.add(Component.translatable(categoryKey).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.empty());

            double initialMaxWeight = 0.0;
            for (var attr : validAttributes.values()) {
                double w = attr.getWeight() + 1.0;
                if (w > initialMaxWeight) initialMaxWeight = w;
            }

            double newMaxWeight = 0.0;
            java.util.Map<net.minecraft.resources.Identifier, Double> reforgedWeights = new java.util.HashMap<>();

            for (var entry : validAttributes.entrySet()) {
                double w = entry.getValue().getWeight() + 1.0;
                if (w > initialMaxWeight / 2.0) {
                    w = (int) (w * reforgeMod);
                }
                reforgedWeights.put(entry.getKey(), w);

                if (w > newMaxWeight) newMaxWeight = w;
            }

            double absoluteTotalWeight = 0.0;

            for (var entry : reforgedWeights.entrySet()) {
                net.minecraft.resources.Identifier id = entry.getKey();
                double w = entry.getValue();

                if (luck > 0) {
                    if (w > newMaxWeight / 3.0) {
                        w = (int) (w * (1.0 - (0.02 * luck)));
                        w = Math.max(2.0, w);
                    } else {
                        w = (int) (w + (1.0 + (luckMod * luck)));
                    }
                }

                absoluteTotalWeight += w;

                String path = id.getPath().toLowerCase();
                for (DynamicTier tier : tiers) {
                    if (path.contains(tier.keyword)) {
                        // 🌟 A MÁGICA: Impede que a palavra "uncommon" caia na regra do "common"
                        if (tier.keyword.equals("common") && path.contains("uncommon")) {
                            continue; // Pula para o próximo (que será o uncommon de verdade)
                        }

                        tier.currentTotalWeight += w;
                        break;
                    }
                }
            }

            if (absoluteTotalWeight > 0) {
                for (DynamicTier t : tiers) {
                    if (t.currentTotalWeight <= 0) continue;

                    double chance = (t.currentTotalWeight / absoluteTotalWeight) * 100.0;
                    String chanceStr = chance < 0.01 ? "< 0.01" : PERCENT_FORMAT.format(chance);

                    tooltip.add(t.name.copy().append(": ").withStyle(t.color)
                            .append(Component.literal(chanceStr + "%").withStyle(ChatFormatting.GRAY)));
                }
            }
            context.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
        }
    }



    @Override
    protected void renderBg(GuiGraphics context, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        context.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
    }

    public static class ReforgeButton extends Button {
        private boolean disabled;

        private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath("tiered", "textures/gui/reforging_button.png");

        public ReforgeButton(int x, int y, Button.OnPress onPress) {
            super(x, y, 18, 18, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.disabled = true;
            this.active = false; // Diz pro Vanilla que não pode ser clicado
        }


@Override
protected void renderContents(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
            int u = 0; // Posição X na imagem (sempre 0, pois a imagem tem 20px de largura)
            int v = 0; // Posição Y na imagem (muda dependendo do estado)

            if (this.disabled) {
                u = 21; // Pega o 3º botão (Escuro/Bloqueado)
            } else if (this.isHovered()) {
                u = 41; // Pega o 2º botão (Azul/Mouse em cima)
            } else {
                u = 1;  // Pega o 1º botão (Normal/Liberado)
            }
            context.blit(RenderPipelines.GUI_TEXTURED, BUTTON_TEXTURE, this.getX(), this.getY(), u, 1, this.width, this.height, this.width, this.height,60,20);
        }

        public void setDisabled(boolean disable) {
            this.disabled = disable;
            this.active = !disable;
        }


    }


}
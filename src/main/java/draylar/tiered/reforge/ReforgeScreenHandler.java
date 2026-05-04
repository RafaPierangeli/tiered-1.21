package draylar.tiered.reforge;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.TieredItemTags;
import draylar.tiered.config.ConfigInit;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ReforgeScreenHandler extends AbstractContainerMenu {

    private final Container inventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ReforgeScreenHandler.this.slotsChanged(this);
        }
    };

    private final ContainerLevelAccess context;
    private final Player player;
    private BlockPos pos;

    // 🌟 NOVO: Sistema nativo para sincronizar o botão (substitui o pacote antigo)
    private final ContainerData propertyDelegate;

    // 🌟 NOVO: Construtor do Cliente (Necessário para a 1.21.11)
//    public ReforgeScreenHandler(int syncId, Inventory playerInventory) {
//        this(syncId, playerInventory, ContainerLevelAccess.NULL);
//    }

    // Construtor do Servidor
    public ReforgeScreenHandler(int syncId, Inventory playerInventory, ContainerLevelAccess context) {
        super(Tiered.REFORGE_SCREEN_HANDLER_TYPE, syncId);

        this.context = context;
        this.player = playerInventory.player;

        // Inicializa o sincronizador do botão
        this.propertyDelegate = new ContainerData() {
            private int value = 0;
            @Override
            public int get(int index) { return value; }
            @Override
            public void set(int index, int value) { this.value = value; }
            @Override
            public int getCount() {
                return 1;
            }
        };
        this.addDataSlots(this.propertyDelegate);

        // 🌟 MANTIVE SUAS COORDENADAS EM "V" AQUI!
        // Slot 0: Ingrediente Base (Esquerda)
        this.addSlot(new Slot(this.inventory, 0, 45, 47));
        // Slot 1: Item a ser reforjado (Centro/Topo)
        this.addSlot(new Slot(this.inventory, 1, 80, 35));
        // Slot 2: Adição (Direita)
        this.addSlot(new Slot(this.inventory, 2, 115, 47) {
            @Override
            public boolean mayPlace(@NonNull ItemStack stack) {
                return stack.is(TieredItemTags.REFORGE_ADDITION);
            }
        });

        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
        this.context.execute((world, pos) -> {
            ReforgeScreenHandler.this.setPos(pos);
        });
    }

    @Override
    public void slotsChanged(@NonNull Container container) {
        super.slotsChanged(container);
        if (container == this.inventory) {
            this.updateResult();
        }
    }

    private void updateResult() {
        boolean isReady = false;
        ItemStack stack = this.getSlot(1).getItem();

        if (this.getSlot(0).hasItem() && this.getSlot(1).hasItem() && this.getSlot(2).hasItem()) {
            Item item = stack.getItem();
            if (!stack.is(TieredItemTags.MODIFIER_RESTRICTED) && ModifierUtils.getRandomAttributeIDFor(null, item, false) != null && !stack.isDamaged()) {
                List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(item);
                ItemStack baseItem = this.getSlot(0).getItem();

                if (!items.isEmpty()) {
                    isReady = items.stream().anyMatch(it -> it == baseItem.getItem());
                } else {
                    var repairable = stack.get(DataComponents.REPAIRABLE);
                    if (repairable != null && repairable.items() != null) {
                        isReady = repairable.items().contains(baseItem.typeHolder());
                    } else {
                        isReady = baseItem.is(TieredItemTags.REFORGE_BASE_ITEM);
                    }
                }
            }
        }

        if (isReady && !ConfigInit.CONFIG.uniqueReforge && ModifierUtils.getAttributeId(stack) != null && ModifierUtils.getAttributeId(stack).getPath().contains("unique")) {
            isReady = false;
        }

        if (isReady && ModifierUtils.getAttributeId(stack) != null && ModifierUtils.getAttributeId(stack).getPath().contains("mythic")) {
            isReady = false;
        }

        // 🌟 TRAVA DE XP: O botão não acende se tiver menos de 30 pontos de XP
        // 🌟 TRAVA DE XP DINÂMICA
        int xpCost = ConfigInit.CONFIG.reforgeXpCost;
        if (isReady && this.player.totalExperience < xpCost && !this.player.isCreative()) {
            isReady = false;
        }

        // Atualiza o estado do botão para a tela
        this.propertyDelegate.set(0, isReady ? 1 : 0);
    }

    // 🌟 NOVO: Metodo que a Screen chama para saber se acende o botão
    public boolean isReforgeReady() {
        return this.propertyDelegate.get(0) == 1;
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        this.context.execute((world, pos) -> this.clearContainer(player, this.inventory));
    }

    @Override
    public boolean stillValid(@NonNull Player player) {
        return this.context.evaluate((world, pos) -> {
            return player.distanceToSqr((double) pos.getX() + 0.5, (double) pos.getY() + 0.5, (double) pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

//    @Override
//    public boolean stillValid(Player player) {
//        return true;
//    }

    @Override
    public @NonNull ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = (Slot) this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();

            if (index == 1) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemStack2, itemStack);
            } else if (index == 0 || index == 2) {
                if (!this.moveItemStackTo(itemStack2, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 3 && index < 39) {
                if (itemStack.is(TieredItemTags.REFORGE_ADDITION) && !this.moveItemStackTo(itemStack2, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }

                if (this.getSlot(1).hasItem()) {
                    ItemStack targetStack = this.getSlot(1).getItem();
                    Item targetItem = targetStack.getItem();

                    var repairable = targetStack.get(DataComponents.REPAIRABLE);
                    if (repairable != null && repairable.items() != null && repairable.items().contains(itemStack.typeHolder())) {
                        if (!this.moveItemStackTo(itemStack2, 0, 1, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (itemStack.is(TieredItemTags.REFORGE_BASE_ITEM) && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }

                    List<Item> items = Tiered.REFORGE_DATA_LOADER.getReforgeBaseItems(targetItem);
                    if (items.stream().anyMatch(it -> it == itemStack2.copy().getItem()) && !this.moveItemStackTo(itemStack2, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }

                if (ModifierUtils.getRandomAttributeIDFor(null, itemStack.getItem(), false) != null && !this.moveItemStackTo(itemStack2, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemStack2.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemStack2);
        }
        return itemStack;
    }

    public void reforge() {
        // 1. Pega o item que está no slot do meio
        ItemStack itemStack = this.getSlot(1).getItem();

        // 2. Descobre qual é o Tier atual dele
        Identifier attrId = ModifierUtils.getAttributeId(itemStack);

        // 🌟 TRAVA DUPLA: Bloqueia se for Único (e a config proibir) OU se for Mítico!
        if (attrId != null) {
            String tierName = attrId.getPath();
            boolean isUniqueLocked = !ConfigInit.CONFIG.uniqueReforge && tierName.contains("unique");
            boolean isMythicLocked = tierName.contains("mythic"); // Mítico sempre bloqueado

            if (isUniqueLocked || isMythicLocked) {
                return; // Aborta a reforja!
            }
        }

        // 🌟 4. COBRANÇA DE XP DINÂMICA (Só cobra se passou pela trava acima)
        int xpCost = ConfigInit.CONFIG.reforgeXpCost;
        if (!this.player.isCreative()) {
            this.player.giveExperiencePoints(-xpCost); // Subtrai o valor da config
        }

        // 5. Remove o Tier antigo e rola os dados para um Tier novo
        ModifierUtils.removeItemStackAttribute(itemStack);
        ModifierUtils.setItemStackAttribute(player, itemStack, true);

        // 6. Gasta os ingredientes (Diamante e Ametista)
        this.decrementStack(0);
        this.decrementStack(2);

        // 7. Toca o som da bigorna
        this.context.execute((world, pos) -> {
            world.playSound(
                    null,                    // player (null = todos escutam)
                    pos,                     // posição do bloco
                    SoundEvents.ANVIL_USE,   // som da bigorna
                    SoundSource.BLOCKS,      // categoria
                    1.0F,                    // volume
                    1.0F                     // pitch
            );
        });
    }

    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    private void decrementStack(int slot) {
        ItemStack itemStack = this.inventory.getItem(slot);
        itemStack.shrink(1);
        this.inventory.setItem(slot, itemStack);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.inventory && super.canTakeItemForPickAll(stack, slot);
    }

}
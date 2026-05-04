package draylar.tiered.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.api.PotentialAttribute;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class CommandInit {

    private static final List<String> TIER_LIST = List.of("common", "uncommon", "rare", "epic", "legendary", "unique","mythic");

    public static void init() {
        // 🌟 CORREÇÃO 1: Assinatura atualizada para a 1.21.11 (registryAccess)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("tiered")
                    // 🌟 CORREÇÃO 2: NÍVEL DE ACESSO! Apenas OP (nível 2) pode usar este comando
                    .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                            .then(Commands.literal("tier")
                            .then(Commands.argument("targets", EntityArgument.players())
                                    .then(Commands.literal("common").executes((context) -> executeCommand(context.getSource(), EntityArgument.getPlayers(context, "targets"), 0)))
                                    .then(Commands.literal("uncommon").executes((context) -> executeCommand(context.getSource(), EntityArgument.getPlayers(context, "targets"), 1)))
                                    .then(Commands.literal("rare").executes((context) -> executeCommand(context.getSource(), EntityArgument.getPlayers(context, "targets"), 2)))
                                    .then(Commands.literal("epic").executes((context) -> executeCommand(context.getSource(), EntityArgument.getPlayers(context, "targets"), 3)))
                                    .then(Commands.literal("legendary").executes((context) -> executeCommand(context.getSource(), EntityArgument.getPlayers(context, "targets"), 4)))
                                    .then(Commands.literal("unique").executes((context) -> executeCommand(context.getSource(), EntityArgument.getPlayers(context, "targets"), 5)))
                                    .then(Commands.literal("mythic").executes((context) -> executeCommand(context.getSource(), EntityArgument.getPlayers(context, "targets"), 6)))
                            )
                    )
                    .then(Commands.literal("untier")
                            .then(Commands.argument("targets", EntityArgument.players()).executes((context) -> {
                                return executeCommand(context.getSource(), EntityArgument.getPlayers(context, "targets"), -1);
                            }))
                    )
            );
        });
    }

    // 0: common; 1: uncommon; 2: rare; 3: epic; 4: legendary; 5: unique
    private static int executeCommand(CommandSourceStack source, Collection<ServerPlayer> targets, int tier) {
        for (ServerPlayer player : targets) {
            ItemStack itemStack = player.getMainHandItem();

            if (itemStack.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("commands.tiered.failed", player.getDisplayName()), true);
                continue;
            }

            if (tier == -1) {
                if (itemStack.get(Tiered.TIER) != null) {
                    ModifierUtils.removeItemStackAttribute(itemStack);
                    source.sendSuccess(() -> Component.translatable("commands.tiered.untier", itemStack.getItemName().getString(), player.getDisplayName()), true);
                } else {
                    source.sendSuccess(() -> Component.translatable("commands.tiered.untier_failed", itemStack.getItemName().getString(), player.getDisplayName()), true);
                }
            } else {
                List<PotentialAttribute> potentialTiers = new ArrayList<>();

                // Filtra os atributos válidos para o item e que correspondem à raridade escolhida
                Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
                    if (attribute.isValid(BuiltInRegistries.ITEM.getKey(itemStack.getItem()))) {
                        String path = id.getPath();
                        String targetRarity = TIER_LIST.get(tier);

                        if (path.contains(targetRarity)) {
                            // Evita que "uncommon" seja pego quando procuramos por "common"
                            if (targetRarity.equals("common") && path.contains("uncommon")) {
                                return;
                            }
                            potentialTiers.add(attribute);
                        }
                    }
                });

                if (potentialTiers.isEmpty()) {
                    source.sendSuccess(() -> Component.translatable("commands.tiered.tiering_failed", itemStack.getItemName().getString(), player.getDisplayName()), true);
                    continue;
                }

                // Limpa o item antes de aplicar o novo tier
                ModifierUtils.removeItemStackAttribute(itemStack);

                // Sorteia um dos tiers válidos daquela raridade
                PotentialAttribute selectedAttribute = potentialTiers.get(player.level().getRandom().nextInt(potentialTiers.size()));

                // 🌟 CORREÇÃO 3: Usa o nosso metodo perfeito para aplicar os status e a durabilidade!
                ModifierUtils.applyARPGModifiers(itemStack, selectedAttribute);

                source.sendSuccess(() -> Component.translatable("commands.tiered.tier", itemStack.getItemName().getString(), player.getDisplayName()), true);
            }
        }
        return 1;
    }
}
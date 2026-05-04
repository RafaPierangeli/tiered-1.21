package draylar.tiered.network;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import draylar.tiered.Tiered;
import draylar.tiered.TieredClient;
import draylar.tiered.api.PotentialAttribute;
import draylar.tiered.network.packet.AttributePacket;
import draylar.tiered.network.packet.HealthPacket;
import draylar.tiered.network.packet.ReforgeItemSyncPacket;
import draylar.tiered.network.packet.ReforgePacket;
import draylar.tiered.network.packet.ReforgeReadyPacket;
import draylar.tiered.reforge.ReforgeScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class TieredClientPacket {

    @SuppressWarnings("resource")
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(ReforgeReadyPacket.PACKET_ID, (payload, context) -> {
            boolean disableButton = payload.disableButton();
            context.client().execute(() -> {
                if (context.client().screen instanceof ReforgeScreen reforgeScreen) {
                    reforgeScreen.reforgeButton.setDisabled(disableButton);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(HealthPacket.PACKET_ID, (payload, context) -> {
            float health = payload.health();
            context.client().execute(() -> {
                context.player().setHealth(health);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ReforgeItemSyncPacket.PACKET_ID, (payload, context) -> {
            List<Identifier> identifiers = payload.ids();
            List<Integer> listSize = payload.listSize();
            List<Integer> itemIds = payload.itemIds();

            context.client().execute(() -> {
                Tiered.REFORGE_DATA_LOADER.clearReforgeBaseItems();

                int count = 0;
                for (int i = 0; i < identifiers.size(); i++) {
                    List<Item> items = new ArrayList<Item>();

                    for (int u = count; u < (count + listSize.get(i)); u++) {
                        items.add(BuiltInRegistries.ITEM.get(itemIds.get(u)).get().value());
                    }
                    count += listSize.get(i);
                    Tiered.REFORGE_DATA_LOADER.putReforgeBaseItems(identifiers.get(i), items);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(AttributePacket.PACKET_ID, (payload, context) -> {
            // Salva os atributos antigos
            TieredClient.CACHED_ATTRIBUTES.putAll(Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes());
            Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().clear();

            // Para cada par id/atributo, carrega usando o novo CODEC
            for (int i = 0; i < payload.attributeIds().size(); i++) {
                Identifier id = Identifier.parse(payload.attributeIds().get(i));
                String jsonString = payload.attributeJsons().get(i);

                try {
                    // 🌟 MÁGICA DO CODEC MANTIDA INTACTA!
                    PotentialAttribute pa = PotentialAttribute.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(jsonString))
                            .getOrThrow(error -> new IllegalStateException("Falha ao ler atributo da rede: " + error));

                    Tiered.ATTRIBUTE_DATA_LOADER.getItemAttributes().put(id, pa);
                } catch (Exception e) {
                    Tiered.LOGGER.error("Erro ao sincronizar atributo {} do servidor", id, e);
                }
            }
        });
    }

    // 🌟 O ÚNICO METODO DE ENVIO NECESSÁRIO AGORA: O clique no botão de Reforjar!
    public static void writeC2SReforgePacket() {
        ClientPlayNetworking.send(new ReforgePacket());
    }
}
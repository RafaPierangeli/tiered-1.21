package draylar.tiered.network.packet;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ReforgeItemSyncPacket(List<Identifier> ids, List<Integer> listSize, List<Integer> itemIds) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReforgeItemSyncPacket> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("tiered", "reforge_item_sync_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReforgeItemSyncPacket> PACKET_CODEC =
            StreamCodec.of(
            (buf, value) -> {
        buf.writeCollection(value.ids, FriendlyByteBuf::writeIdentifier);
        buf.writeCollection(value.listSize, FriendlyByteBuf::writeInt);
        buf.writeCollection(value.itemIds, FriendlyByteBuf::writeInt);
    }, buf -> new ReforgeItemSyncPacket(buf.readList(FriendlyByteBuf::readIdentifier), buf.readList(FriendlyByteBuf::readInt), buf.readList(FriendlyByteBuf::readInt)));



    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }


}

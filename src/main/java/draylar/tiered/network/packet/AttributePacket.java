package draylar.tiered.network.packet;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record AttributePacket(List<String> attributeIds, List<String> attributeJsons) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AttributePacket> PACKET_ID = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("tiered", "attribute_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttributePacket> PACKET_CODEC =
            StreamCodec.of(
                    (buf, value) -> {
                        buf.writeCollection(value.attributeIds, FriendlyByteBuf::writeUtf);
                        buf.writeCollection(value.attributeJsons, FriendlyByteBuf::writeUtf);
                    },
                    buf -> new AttributePacket(
                            buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                            buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf)
                    )
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }


}

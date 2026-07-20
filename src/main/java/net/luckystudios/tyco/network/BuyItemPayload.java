package net.luckystudios.tyco.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record BuyItemPayload(int entryIndex, int quantity) implements CustomPacketPayload {
    public static final Type<BuyItemPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("tyco", "buy_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuyItemPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BuyItemPayload::entryIndex,
                    ByteBufCodecs.VAR_INT, BuyItemPayload::quantity,
                    BuyItemPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
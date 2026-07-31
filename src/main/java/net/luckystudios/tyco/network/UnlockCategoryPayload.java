package net.luckystudios.tyco.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UnlockCategoryPayload(String category) implements CustomPacketPayload {
    public static final Type<UnlockCategoryPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("tyco", "unlock_category"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UnlockCategoryPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, UnlockCategoryPayload::category,
                    UnlockCategoryPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
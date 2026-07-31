package net.luckystudios.tyco.network;

import net.luckystudios.tyco.shop.CategoryDisplay;
import net.luckystudios.tyco.shop.ShopEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record OpenShopPayload(List<ShopEntry> entries, List<CategoryDisplay> categories) implements CustomPacketPayload {
    public static final Type<OpenShopPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("tyco", "open_shop"));

    private static final StreamCodec<RegistryFriendlyByteBuf, ShopEntry> ENTRY_CODEC = StreamCodec.of(
            (buf, entry) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, entry.itemId());
                ByteBufCodecs.VAR_INT.encode(buf, entry.price());
                ByteBufCodecs.STRING_UTF8.encode(buf, entry.category());
            },
            (buf) -> new ShopEntry(
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf)
            )
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, CategoryDisplay> CATEGORY_CODEC = StreamCodec.of(
            (buf, cat) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, cat.category());
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, cat.iconItemId());
                ByteBufCodecs.BOOL.encode(buf, cat.locked());
                ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).encode(buf, cat.unlockPrice());
                ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).encode(buf, cat.unlockItemId());
                ByteBufCodecs.VAR_INT.encode(buf, cat.unlockItemCount());
            },
            (buf) -> new CategoryDisplay(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.VAR_INT).decode(buf),
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                ByteBufCodecs.collection(ArrayList::new, ENTRY_CODEC).encode(buf, new ArrayList<>(payload.entries()));
                ByteBufCodecs.collection(ArrayList::new, CATEGORY_CODEC).encode(buf, new ArrayList<>(payload.categories()));
            },
            (buf) -> new OpenShopPayload(
                    ByteBufCodecs.collection(ArrayList::new, ENTRY_CODEC).decode(buf),
                    ByteBufCodecs.collection(ArrayList::new, CATEGORY_CODEC).decode(buf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
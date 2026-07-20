package net.luckystudios.tyco.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WeightedOutput(Item item, int weight, int minCount, int maxCount, double bonusChance, int bonusCount) {

    public static final Codec<WeightedOutput> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(WeightedOutput::item),
            Codec.INT.fieldOf("weight").forGetter(WeightedOutput::weight),
            Codec.INT.optionalFieldOf("min_count", 1).forGetter(WeightedOutput::minCount),
            Codec.INT.optionalFieldOf("max_count", 1).forGetter(WeightedOutput::maxCount),
            Codec.DOUBLE.optionalFieldOf("bonus_chance", 0.0).forGetter(WeightedOutput::bonusChance),
            Codec.INT.optionalFieldOf("bonus_count", 0).forGetter(WeightedOutput::bonusCount)
    ).apply(inst, WeightedOutput::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeightedOutput> STREAM_CODEC = StreamCodec.of(
            (buf, w) -> {
                ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM).encode(buf, w.item());
                ByteBufCodecs.VAR_INT.encode(buf, w.weight());
                ByteBufCodecs.VAR_INT.encode(buf, w.minCount());
                ByteBufCodecs.VAR_INT.encode(buf, w.maxCount());
                ByteBufCodecs.DOUBLE.encode(buf, w.bonusChance());
                ByteBufCodecs.VAR_INT.encode(buf, w.bonusCount());
            },
            (buf) -> new WeightedOutput(
                    ByteBufCodecs.registry(net.minecraft.core.registries.Registries.ITEM).decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            )
    );

    public ItemStack roll(RandomSource random) {
        int count;
        if (bonusChance > 0 && random.nextDouble() < bonusChance) {
            count = bonusCount;
        } else {
            count = minCount == maxCount ? minCount : minCount + random.nextInt(maxCount - minCount + 1);
        }
        return new ItemStack(item, count);
    }
}
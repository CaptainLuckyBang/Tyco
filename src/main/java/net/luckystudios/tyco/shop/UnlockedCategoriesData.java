package net.luckystudios.tyco.shop;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UnlockedCategoriesData extends SavedData {
    private final Map<UUID, Set<String>> unlocked = new HashMap<>();

    public boolean isUnlocked(UUID player, String category) {
        return unlocked.getOrDefault(player, Set.of()).contains(category);
    }

    public void unlock(UUID player, String category) {
        unlocked.computeIfAbsent(player, k -> new HashSet<>()).add(category);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (var entry : unlocked.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player", entry.getKey());
            ListTag categories = new ListTag();
            for (String category : entry.getValue()) {
                categories.add(StringTag.valueOf(category));
            }
            playerTag.put("categories", categories);
            list.add(playerTag);
        }
        tag.put("unlocked", list);
        return tag;
    }

    public static UnlockedCategoriesData load(CompoundTag tag, HolderLookup.Provider registries) {
        UnlockedCategoriesData data = new UnlockedCategoriesData();
        ListTag list = tag.getList("unlocked", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag playerTag = list.getCompound(i);
            UUID playerId = playerTag.getUUID("player");
            Set<String> categories = new HashSet<>();
            ListTag categoryList = playerTag.getList("categories", Tag.TAG_STRING);
            for (int j = 0; j < categoryList.size(); j++) {
                categories.add(categoryList.getString(j));
            }
            data.unlocked.put(playerId, categories);
        }
        return data;
    }

    private static final Factory<UnlockedCategoriesData> FACTORY = new Factory<>(
            UnlockedCategoriesData::new,
            UnlockedCategoriesData::load,
            null
    );

    public static UnlockedCategoriesData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "tyco_unlocked_categories");
    }
}
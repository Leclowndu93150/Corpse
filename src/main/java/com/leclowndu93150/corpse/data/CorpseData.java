package com.leclowndu93150.corpse.data;

import java.util.List;
import java.util.UUID;

public record CorpseData(
    String corpseId,
    UUID ownerUuid,
    String ownerName,
    String worldName,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    long createdAt,
    List<SerializedItemStack> hotbarItems,
    List<SerializedItemStack> storageItems,
    List<SerializedItemStack> armorItems,
    List<SerializedItemStack> utilityItems,
    UUID entityUuid
) {
    public CorpseData(
        String corpseId,
        UUID ownerUuid,
        String ownerName,
        String worldName,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        long createdAt,
        List<SerializedItemStack> hotbarItems,
        List<SerializedItemStack> storageItems,
        List<SerializedItemStack> armorItems,
        List<SerializedItemStack> utilityItems
    ) {
        this(corpseId, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch, createdAt,
             hotbarItems, storageItems, armorItems, utilityItems, null);
    }

    public CorpseData withEntityUuid(UUID newEntityUuid) {
        return new CorpseData(corpseId, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch,
                              createdAt, hotbarItems, storageItems, armorItems, utilityItems, newEntityUuid);
    }

    public CorpseData withItems(
        List<SerializedItemStack> newHotbarItems,
        List<SerializedItemStack> newStorageItems,
        List<SerializedItemStack> newArmorItems,
        List<SerializedItemStack> newUtilityItems
    ) {
        return new CorpseData(corpseId, ownerUuid, ownerName, worldName, x, y, z, yaw, pitch,
                              createdAt, newHotbarItems, newStorageItems, newArmorItems, newUtilityItems, entityUuid);
    }
    public boolean isEmpty() {
        return isListEmpty(hotbarItems) && isListEmpty(storageItems) && isListEmpty(armorItems) && isListEmpty(utilityItems);
    }

    private static boolean isListEmpty(List<SerializedItemStack> list) {
        if (list == null || list.isEmpty()) {
            return true;
        }
        for (SerializedItemStack item : list) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }
}

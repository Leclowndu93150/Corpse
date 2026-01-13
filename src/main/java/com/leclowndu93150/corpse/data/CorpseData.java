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
    List<SerializedItemStack> utilityItems
) {
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

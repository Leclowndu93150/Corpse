package com.leclowndu93150.corpse.data;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.bson.BsonDocument;

public record SerializedItemStack(
    String itemId,
    int quantity,
    double durability,
    double maxDurability,
    String metadata
) {
    public static SerializedItemStack fromItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String metadataJson = null;
        if (stack.getMetadata() != null) {
            metadataJson = stack.getMetadata().toJson();
        }
        return new SerializedItemStack(
            stack.getItemId(),
            stack.getQuantity(),
            stack.getDurability(),
            stack.getMaxDurability(),
            metadataJson
        );
    }

    public ItemStack toItemStack() {
        if (itemId == null || itemId.equals("Empty")) {
            return null;
        }
        BsonDocument meta = null;
        if (metadata != null && !metadata.isEmpty()) {
            meta = BsonDocument.parse(metadata);
        }
        return new ItemStack(itemId, quantity, durability, maxDurability, meta);
    }
}

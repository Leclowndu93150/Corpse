package com.leclowndu93150.corpse.window;

import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.window.WindowAction;
import com.hypixel.hytale.protocol.packets.window.WindowType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ItemContainerWindow;
import com.hypixel.hytale.server.core.entity.entities.player.windows.Window;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.leclowndu93150.corpse.data.CorpseData;
import com.leclowndu93150.corpse.data.SerializedItemStack;
import com.leclowndu93150.corpse.manager.CorpseManager;
import java.util.List;
import javax.annotation.Nonnull;

public class CorpseWindow extends Window implements ItemContainerWindow {
    private final JsonObject windowData;
    private final SimpleItemContainer itemContainer;
    private final CorpseManager corpseManager;
    private final String corpseId;
    private final CorpseData corpseData;
    private boolean despawnScheduled = false;

    public CorpseWindow(@Nonnull CorpseManager corpseManager, @Nonnull String corpseId, @Nonnull CorpseData corpseData) {
        super(WindowType.Container);
        this.corpseManager = corpseManager;
        this.corpseId = corpseId;
        this.corpseData = corpseData;
        this.windowData = new JsonObject();
        this.windowData.addProperty("title", corpseData.ownerName() + "'s Corpse");
        int totalSlots = getSlotCount(corpseData.hotbarItems())
                       + getSlotCount(corpseData.storageItems())
                       + getSlotCount(corpseData.armorItems())
                       + getSlotCount(corpseData.utilityItems());
        this.itemContainer = new SimpleItemContainer((short) Math.max(totalSlots, 45));
        populateContainer();
        this.itemContainer.setGlobalFilter(FilterType.ALLOW_OUTPUT_ONLY);
    }

    private int getSlotCount(List<SerializedItemStack> items) {
        return items != null ? items.size() : 0;
    }

    private void populateContainer() {
        short slot = 0;
        slot = addItemsToContainer(corpseData.hotbarItems(), slot);
        slot = addItemsToContainer(corpseData.storageItems(), slot);
        slot = addItemsToContainer(corpseData.armorItems(), slot);
        addItemsToContainer(corpseData.utilityItems(), slot);
    }

    private short addItemsToContainer(List<SerializedItemStack> items, short startSlot) {
        if (items == null) {
            return startSlot;
        }
        short slot = startSlot;
        for (SerializedItemStack serialized : items) {
            if (serialized != null) {
                ItemStack stack = serialized.toItemStack();
                if (stack != null) {
                    itemContainer.setItemStackForSlot(slot, stack, false);
                }
            }
            slot++;
        }
        return slot;
    }

    @Override
    @Nonnull
    public JsonObject getData() {
        return windowData;
    }

    @Override
    protected boolean onOpen0() {
        return true;
    }

    @Override
    protected void onClose0() {
        if (isContainerEmpty() && !despawnScheduled) {
            scheduleDespawn();
        }
    }

    private boolean isContainerEmpty() {
        for (short i = 0; i < itemContainer.getCapacity(); i++) {
            ItemStack stack = itemContainer.getItemStack(i);
            if (stack != null && !stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void scheduleDespawn() {
        if (despawnScheduled) {
            return;
        }
        despawnScheduled = true;

        Ref<EntityStore> corpseEntity = corpseManager.getCorpseEntity(corpseId);
        if (corpseEntity != null && corpseEntity.isValid()) {
            Store<EntityStore> store = corpseEntity.getStore();
            store.removeEntity(corpseEntity, RemoveReason.REMOVE);
            corpseManager.removeCorpseData(corpseId);
            return;
        }

        corpseManager.removeCorpse(corpseId);
    }

    @Override
    @Nonnull
    public ItemContainer getItemContainer() {
        return itemContainer;
    }

    @Override
    public void handleAction(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull WindowAction action) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        if (isContainerEmpty() && !despawnScheduled) {
            close();
            scheduleDespawn();
        }
    }
}

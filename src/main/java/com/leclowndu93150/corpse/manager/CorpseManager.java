package com.leclowndu93150.corpse.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.leclowndu93150.corpse.data.CorpseData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CorpseManager {
    private final Map<String, CorpseData> corpses = new ConcurrentHashMap<>();
    private final Map<String, Ref<EntityStore>> corpseEntities = new ConcurrentHashMap<>();
    private final DataManager dataManager;
    private final HytaleLogger logger;
    private boolean allowOtherPlayersToLoot;

    public CorpseManager(@Nonnull DataManager dataManager, @Nonnull HytaleLogger logger, boolean allowOtherPlayersToLoot) {
        this.dataManager = dataManager;
        this.logger = logger;
        this.allowOtherPlayersToLoot = allowOtherPlayersToLoot;
    }

    public void load() {
        corpses.clear();
        corpses.putAll(dataManager.loadCorpses());
    }

    public void save() {
        dataManager.saveCorpses(corpses);
    }

    public void addCorpse(@Nonnull CorpseData corpseData) {
        corpses.put(corpseData.corpseId(), corpseData);
        save();
    }

    public void registerCorpseEntity(@Nonnull String corpseId, @Nonnull Ref<EntityStore> entityRef) {
        corpseEntities.put(corpseId, entityRef);
    }

    public void unregisterCorpseEntity(@Nonnull String corpseId) {
        corpseEntities.remove(corpseId);
    }

    @Nullable
    public Ref<EntityStore> getCorpseEntity(@Nonnull String corpseId) {
        return corpseEntities.get(corpseId);
    }

    @Nullable
    public CorpseData getCorpse(@Nonnull String corpseId) {
        return corpses.get(corpseId);
    }

    @Nullable
    public String findCorpseIdByEntity(@Nonnull Entity entity) {
        Ref<EntityStore> targetRef = entity.getReference();
        if (targetRef == null) {
            return null;
        }
        return findCorpseIdByRef(targetRef);
    }

    @Nullable
    public String findCorpseIdByRef(@Nonnull Ref<EntityStore> targetRef) {
        for (Map.Entry<String, Ref<EntityStore>> entry : corpseEntities.entrySet()) {
            if (entry.getValue().equals(targetRef)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeCorpse(@Nonnull String corpseId) {
        corpses.remove(corpseId);
        Ref<EntityStore> entityRef = corpseEntities.remove(corpseId);
        if (entityRef != null) {
            try {
                Store<EntityStore> store = entityRef.getStore();
                if (store != null) {
                    store.removeEntity(entityRef, RemoveReason.REMOVE);
                }
            } catch (Exception e) {
            }
        }
        save();
    }

    public void removeCorpseWithStore(@Nonnull String corpseId, @Nonnull Store<EntityStore> store) {
        corpses.remove(corpseId);
        Ref<EntityStore> entityRef = corpseEntities.remove(corpseId);
        if (entityRef != null) {
            try {
                store.removeEntity(entityRef, RemoveReason.REMOVE);
            } catch (Exception e) {
            }
        }
        save();
    }

    public void updateCorpse(@Nonnull CorpseData corpseData) {
        corpses.put(corpseData.corpseId(), corpseData);
        save();
    }

    public boolean canPlayerLoot(@Nonnull UUID playerUuid, @Nonnull CorpseData corpse) {
        if (allowOtherPlayersToLoot) {
            return true;
        }
        return playerUuid.equals(corpse.ownerUuid());
    }

    public void setAllowOtherPlayersToLoot(boolean allow) {
        this.allowOtherPlayersToLoot = allow;
    }

    public Map<String, CorpseData> getAllCorpses() {
        return corpses;
    }
}

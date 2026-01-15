package com.leclowndu93150.corpse.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
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
    private final Map<String, UUID> corpseEntityUuids = new ConcurrentHashMap<>();
    private final Map<UUID, String> corpseIdsByEntityUuid = new ConcurrentHashMap<>();
    private final DataManager dataManager;
    private boolean allowOtherPlayersToLoot;
    private boolean enableLootableByAnyoneAfterSeconds;
    private int lootableByAnyoneAfterSeconds;

    public CorpseManager(@Nonnull DataManager dataManager, boolean allowOtherPlayersToLoot, boolean enableLootableByAnyoneAfterSeconds, int lootableByAnyoneAfterSeconds) {
        this.dataManager = dataManager;
        this.allowOtherPlayersToLoot = allowOtherPlayersToLoot;
        this.enableLootableByAnyoneAfterSeconds = enableLootableByAnyoneAfterSeconds;
        this.lootableByAnyoneAfterSeconds = lootableByAnyoneAfterSeconds;
    }

    public void load() {
        corpses.clear();
        corpses.putAll(dataManager.loadCorpses());
        corpseEntities.clear();
        corpseEntityUuids.clear();
        corpseIdsByEntityUuid.clear();
        for (CorpseData data : corpses.values()) {
            if (data.entityUuid() != null) {
                corpseEntityUuids.put(data.corpseId(), data.entityUuid());
                corpseIdsByEntityUuid.put(data.entityUuid(), data.corpseId());
            }
        }
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
        unregisterCorpseEntityUuid(corpseId);
    }

    public void registerCorpseEntityUuid(@Nonnull String corpseId, @Nonnull UUID entityUuid) {
        UUID previous = corpseEntityUuids.put(corpseId, entityUuid);
        if (previous != null && !previous.equals(entityUuid)) {
            corpseIdsByEntityUuid.remove(previous);
        }
        corpseIdsByEntityUuid.put(entityUuid, corpseId);
    }

    public void unregisterCorpseEntityUuid(@Nonnull String corpseId) {
        UUID previous = corpseEntityUuids.remove(corpseId);
        if (previous != null) {
            corpseIdsByEntityUuid.remove(previous);
        }
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

    @Nullable
    public String findCorpseIdByEntityUuid(@Nonnull UUID entityUuid) {
        return corpseIdsByEntityUuid.get(entityUuid);
    }

    public void removeCorpse(@Nonnull String corpseId) {
        corpses.remove(corpseId);
        Ref<EntityStore> entityRef = corpseEntities.remove(corpseId);
        unregisterCorpseEntityUuid(corpseId);
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

    public void removeCorpseData(@Nonnull String corpseId) {
        corpses.remove(corpseId);
        corpseEntities.remove(corpseId);
        unregisterCorpseEntityUuid(corpseId);
        save();
    }

    public void removeCorpseWithStore(@Nonnull String corpseId, @Nonnull Store<EntityStore> store) {
        corpses.remove(corpseId);
        Ref<EntityStore> entityRef = corpseEntities.remove(corpseId);
        unregisterCorpseEntityUuid(corpseId);
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
        if (playerUuid.equals(corpse.ownerUuid())) {
            return true;
        }
        if (enableLootableByAnyoneAfterSeconds && lootableByAnyoneAfterSeconds > 0) {
            long ageMs = System.currentTimeMillis() - corpse.createdAt();
            long ageSeconds = ageMs / 1000;
            if (ageSeconds >= lootableByAnyoneAfterSeconds) {
                return true;
            }
        }
        return false;
    }

    public void setAllowOtherPlayersToLoot(boolean allow) {
        this.allowOtherPlayersToLoot = allow;
    }

    public void setLootableByAnyoneAfterSeconds(int seconds) {
        this.lootableByAnyoneAfterSeconds = seconds;
    }

    public Map<String, CorpseData> getAllCorpses() {
        return corpses;
    }
}

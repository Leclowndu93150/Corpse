package com.leclowndu93150.corpse;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.StartWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.leclowndu93150.corpse.config.CorpseConfig;
import com.leclowndu93150.corpse.data.CorpseData;
import com.leclowndu93150.corpse.manager.CorpseManager;
import com.leclowndu93150.corpse.manager.DataManager;
import com.leclowndu93150.corpse.npc.BuilderActionOpenCorpse;
import com.leclowndu93150.corpse.system.CorpsePoseSystem;
import com.leclowndu93150.corpse.system.PlayerDeathCorpseSystem;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

public class CorpsePlugin extends JavaPlugin {
    private static final String CORPSE_ROLE_NAME = "Corpse";
    private static final float CORPSE_PITCH = (float) (Math.PI / 2.0);

    private final Config<CorpseConfig> config = this.withConfig(CorpseConfig.CODEC);
    private DataManager dataManager;
    private CorpseManager corpseManager;
    private ScheduledExecutorService cleanupScheduler;

    public CorpsePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        CorpseConfig cfg = this.config.get();
        this.config.save();
        this.dataManager = new DataManager(this.getDataDirectory());
        this.corpseManager = new CorpseManager(this.dataManager, cfg.isAllowOtherPlayersToLoot());
        this.corpseManager.load();
        BuilderActionOpenCorpse.setCorpseManager(this.corpseManager);
        this.getEventRegistry().register((short)-9, LoadAssetEvent.class, event -> {
            if (NPCPlugin.get() != null) {
                NPCPlugin.get().registerCoreComponentType("OpenCorpse", BuilderActionOpenCorpse::new);
            }
        });
        HytaleServer.get().getEventBus().registerGlobal(StartWorldEvent.class, this::onWorldStart);
    }

    private void onWorldStart(StartWorldEvent event) {
        World world = event.getWorld();
        String worldName = world.getName();
        for (CorpseData corpseData : this.corpseManager.getAllCorpses().values()) {
            if (!worldName.equals(corpseData.worldName())) {
                continue;
            }
            if (this.corpseManager.getCorpseEntity(corpseData.corpseId()) != null) {
                continue;
            }
            respawnCorpseWithChunkLoad(world, corpseData);
        }
    }

    private void respawnCorpseWithChunkLoad(World world, CorpseData corpseData) {
        Vector3d position = new Vector3d(corpseData.x(), corpseData.y(), corpseData.z());
        long chunkIndex = ChunkUtil.indexChunkFromBlock(corpseData.x(), corpseData.z());
        world.getChunkStore().getChunkReferenceAsync(chunkIndex).thenAccept(chunkRef -> {
            world.execute(() -> {
                spawnCorpseEntity(world, corpseData, position);
            });
        });
    }

    private void spawnCorpseEntity(World world, CorpseData corpseData, Vector3d position) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            return;
        }
        int roleIndex = npcPlugin.getIndex(CORPSE_ROLE_NAME);
        if (roleIndex < 0) {
            return;
        }
        Store<EntityStore> store = world.getEntityStore().getStore();
        Vector3f corpseRotation = new Vector3f(CORPSE_PITCH, corpseData.yaw(), 0.0f);
        String displayName = corpseData.ownerName() + "'s Corpse";
        try {
            Pair<Ref<EntityStore>, NPCEntity> corpsePair = npcPlugin.spawnEntity(
                store, roleIndex, position, corpseRotation, null,
                (npcEntity, holder, entityStore) -> {
                    holder.putComponent(DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(displayName)));
                    holder.putComponent(Nameplate.getComponentType(), new Nameplate(displayName));
                },
                null
            );
            if (corpsePair != null) {
                Ref<EntityStore> corpseRef = corpsePair.first();
                this.corpseManager.registerCorpseEntity(corpseData.corpseId(), corpseRef);
                UUIDComponent uuidComponent = store.getComponent(corpseRef, UUIDComponent.getComponentType());
                if (uuidComponent != null) {
                    UUID entityUuid = uuidComponent.getUuid();
                    this.corpseManager.registerCorpseEntityUuid(corpseData.corpseId(), entityUuid);
                    if (corpseData.entityUuid() == null || !corpseData.entityUuid().equals(entityUuid)) {
                        this.corpseManager.updateCorpse(corpseData.withEntityUuid(entityUuid));
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void start() {
        this.getEntityStoreRegistry().registerSystem(new PlayerDeathCorpseSystem(this.corpseManager, Player.getComponentType()));
        this.getEntityStoreRegistry().registerSystem(new CorpsePoseSystem());
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        this.cleanupScheduler.scheduleAtFixedRate(this::cleanupEmptyCorpses, 10, 10, TimeUnit.SECONDS);
    }

    private void cleanupEmptyCorpses() {
        try {
            Universe.get().getWorlds().forEach((name, world) -> {
                world.execute(() -> {
                    Store<EntityStore> store = world.getEntityStore().getStore();
                    List<Ref<EntityStore>> toRemove = new ArrayList<>();
                    store.forEachChunk(NPCEntity.getComponentType(), (archetypeChunk, commandBuffer) -> {
                        for (int i = 0; i < archetypeChunk.size(); i++) {
                            NPCEntity npc = archetypeChunk.getComponent(i, NPCEntity.getComponentType());
                            if (!CORPSE_ROLE_NAME.equals(npc.getRoleName())) {
                                continue;
                            }
                            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
                            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
                            if (uuidComponent == null) {
                                toRemove.add(ref);
                                continue;
                            }
                            String corpseId = this.corpseManager.findCorpseIdByEntityUuid(uuidComponent.getUuid());
                            if (corpseId == null) {
                                toRemove.add(ref);
                                continue;
                            }
                            CorpseData corpseData = this.corpseManager.getCorpse(corpseId);
                            if (corpseData == null || corpseData.isEmpty()) {
                                toRemove.add(ref);
                                if (corpseData != null) {
                                    this.corpseManager.removeCorpseData(corpseId);
                                }
                            }
                        }
                    });
                    for (Ref<EntityStore> ref : toRemove) {
                        store.removeEntity(ref, RemoveReason.REMOVE);
                    }
                });
            });
        } catch (Exception e) {
        }
    }

    @Override
    protected void shutdown() {
        if (this.cleanupScheduler != null) {
            this.cleanupScheduler.shutdown();
        }
        if (this.corpseManager != null) {
            this.corpseManager.save();
        }
    }
}

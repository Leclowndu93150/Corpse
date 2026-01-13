package com.leclowndu93150.corpse;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.leclowndu93150.corpse.config.CorpseConfig;
import com.leclowndu93150.corpse.data.CorpseData;
import com.leclowndu93150.corpse.manager.CorpseManager;
import com.leclowndu93150.corpse.manager.DataManager;
import com.leclowndu93150.corpse.npc.BuilderActionOpenCorpse;
import com.leclowndu93150.corpse.system.PlayerDeathCorpseSystem;
import it.unimi.dsi.fastutil.Pair;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class CorpsePlugin extends JavaPlugin {
    private final Config<CorpseConfig> config = this.withConfig(CorpseConfig.CODEC);
    private DataManager dataManager;
    private CorpseManager corpseManager;

    public CorpsePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        CorpseConfig cfg = this.config.get();
        this.dataManager = new DataManager(this.getDataDirectory(), this.getLogger());
        this.corpseManager = new CorpseManager(this.dataManager, this.getLogger(), cfg.isAllowOtherPlayersToLoot());
        this.corpseManager.load();
        BuilderActionOpenCorpse.setCorpseManager(this.corpseManager);
        this.getEventRegistry().register((short)-9, LoadAssetEvent.class, event -> {
            if (NPCPlugin.get() != null) {
                NPCPlugin.get().registerCoreComponentType("OpenCorpse", BuilderActionOpenCorpse::new);
                this.getLogger().at(Level.INFO).log("Registered OpenCorpse action before NPC asset loading");
            }
        });
    }

    @Override
    protected void start() {
        this.getEntityStoreRegistry().registerSystem(new PlayerDeathCorpseSystem(this.corpseManager, Player.getComponentType()));
        this.getEventRegistry().registerGlobal(AllWorldsLoadedEvent.class, event -> respawnAllCorpses());
        this.getLogger().at(Level.INFO).log("Corpse plugin loaded! When you die, your items will be stored in a corpse.");
    }

    private void respawnAllCorpses() {
        Map<String, CorpseData> corpses = this.corpseManager.getAllCorpses();
        if (corpses.isEmpty()) {
            return;
        }
        this.getLogger().at(Level.INFO).log("Respawning %d corpses from saved data...", corpses.size());
        Universe universe = Universe.get();
        if (universe == null) {
            this.getLogger().at(Level.WARNING).log("Universe is null, cannot respawn corpses");
            return;
        }
        for (Map.Entry<String, CorpseData> entry : corpses.entrySet()) {
            String corpseId = entry.getKey();
            CorpseData corpseData = entry.getValue();
            World world = universe.getWorld(corpseData.worldName());
            if (world == null) {
                this.getLogger().at(Level.WARNING).log("World '%s' not found for corpse %s", corpseData.worldName(), corpseId);
                continue;
            }
            world.execute(() -> {
                spawnCorpseEntity(world, corpseId, corpseData);
            });
        }
    }

    private void spawnCorpseEntity(World world, String corpseId, CorpseData corpseData) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            this.getLogger().at(Level.SEVERE).log("NPCPlugin is null, cannot spawn corpse");
            return;
        }
        int roleIndex = npcPlugin.getIndex("Corpse");
        if (roleIndex < 0) {
            this.getLogger().at(Level.SEVERE).log("Corpse role not found");
            return;
        }
        Vector3d position = new Vector3d(corpseData.x(), corpseData.y(), corpseData.z());
        Vector3f rotation = new Vector3f(
            (float)(Math.PI / 2.0),
            corpseData.yaw(),
            0.0f
        );
        Store<EntityStore> store = world.getEntityStore().getStore();
        try {
            Pair<Ref<EntityStore>, NPCEntity> corpsePair = npcPlugin.spawnEntity(
                store,
                roleIndex,
                position,
                rotation,
                null,
                null
            );
            if (corpsePair != null) {
                Ref<EntityStore> corpseRef = corpsePair.first();
                store.ensureComponent(corpseRef, Frozen.getComponentType());
                this.corpseManager.registerCorpseEntity(corpseId, corpseRef);
                this.getLogger().at(Level.INFO).log("Respawned corpse %s for %s", corpseId, corpseData.ownerName());
            }
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).withCause(e).log("Failed to respawn corpse %s: %s", corpseId, e.getMessage());
        }
    }

    @Override
    protected void shutdown() {
        if (this.corpseManager != null) {
            this.corpseManager.save();
        }
    }
    
}

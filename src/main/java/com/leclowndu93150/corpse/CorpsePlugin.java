package com.leclowndu93150.corpse;

import com.hypixel.hytale.server.core.asset.LoadAssetEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.leclowndu93150.corpse.config.CorpseConfig;
import com.leclowndu93150.corpse.manager.CorpseManager;
import com.leclowndu93150.corpse.manager.DataManager;
import com.leclowndu93150.corpse.npc.BuilderActionOpenCorpse;
import com.leclowndu93150.corpse.system.CorpsePoseSystem;
import com.leclowndu93150.corpse.system.PlayerDeathCorpseSystem;
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
    }

    @Override
    protected void start() {
        this.getEntityStoreRegistry().registerSystem(new PlayerDeathCorpseSystem(this.corpseManager, Player.getComponentType()));
        this.getEntityStoreRegistry().registerSystem(new CorpsePoseSystem());
    }

    @Override
    protected void shutdown() {
        if (this.corpseManager != null) {
            this.corpseManager.save();
        }
    }
}

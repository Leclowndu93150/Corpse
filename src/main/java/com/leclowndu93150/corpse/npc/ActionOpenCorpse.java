package com.leclowndu93150.corpse.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.leclowndu93150.corpse.data.CorpseData;
import com.leclowndu93150.corpse.manager.CorpseManager;
import com.leclowndu93150.corpse.window.CorpseWindow;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class ActionOpenCorpse extends ActionBase {
    private static final HytaleLogger LOGGER = HytaleLogger.get("Corpse");
    private final CorpseManager corpseManager;

    public ActionOpenCorpse(@Nonnull BuilderActionOpenCorpse builder, @Nonnull BuilderSupport support, @Nonnull CorpseManager corpseManager) {
        super(builder);
        this.corpseManager = corpseManager;
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
        return super.canExecute(ref, role, sensorInfo, dt, store) && role.getStateSupport().getInteractionIterationTarget() != null;
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull Role role, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
        super.execute(ref, role, sensorInfo, dt, store);
        Ref<EntityStore> playerReference = role.getStateSupport().getInteractionIterationTarget();
        if (playerReference == null) {
            LOGGER.at(Level.WARNING).log("OpenCorpse execute failed: no interaction target for npcRef=%s", ref);
            return false;
        }
        PlayerRef playerRefComponent = store.getComponent(playerReference, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            LOGGER.at(Level.WARNING).log("OpenCorpse execute failed: missing PlayerRef for target=%s npcRef=%s", playerReference, ref);
            return false;
        }
        Player playerComponent = store.getComponent(playerReference, Player.getComponentType());
        if (playerComponent == null) {
            LOGGER.at(Level.WARNING).log("OpenCorpse execute failed: missing Player component for target=%s npcRef=%s", playerReference, ref);
            return false;
        }
        LOGGER.at(Level.INFO).log("OpenCorpse execute: npcRef=%s player=%s", ref, playerRefComponent.getUsername());
        String corpseId = corpseManager.findCorpseIdByRef(ref);
        if (corpseId == null) {
            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent != null) {
                corpseId = corpseManager.findCorpseIdByEntityUuid(uuidComponent.getUuid());
            }
        }
        if (corpseId == null) {
            LOGGER.at(Level.WARNING).log("OpenCorpse execute failed: no corpseId for npcRef=%s", ref);
            return false;
        }
        CorpseData corpseData = corpseManager.getCorpse(corpseId);
        if (corpseData == null) {
            LOGGER.at(Level.WARNING).log("OpenCorpse execute failed: corpse data missing for corpseId=%s", corpseId);
            return false;
        }
        if (!corpseManager.canPlayerLoot(playerRefComponent.getUuid(), corpseData)) {
            LOGGER.at(Level.WARNING).log("OpenCorpse execute denied: player=%s corpseId=%s owner=%s", playerRefComponent.getUsername(), corpseId, corpseData.ownerName());
            return false;
        }
        CorpseWindow window = new CorpseWindow(corpseManager, corpseId, corpseData);
        boolean opened = playerComponent.getPageManager().setPageWithWindows(playerReference, store, Page.Bench, true, window);
        if (opened) {
            LOGGER.at(Level.INFO).log("OpenCorpse opened window via PageManager: corpseId=%s player=%s", corpseId, playerRefComponent.getUsername());
            return true;
        }
        LOGGER.at(Level.WARNING).log("OpenCorpse failed to open window via PageManager: corpseId=%s player=%s", corpseId, playerRefComponent.getUsername());
        return false;
    }
}

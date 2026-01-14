package com.leclowndu93150.corpse.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import com.leclowndu93150.corpse.data.CorpseData;
import com.leclowndu93150.corpse.manager.CorpseManager;
import com.leclowndu93150.corpse.window.CorpseWindow;
import javax.annotation.Nonnull;

public class ActionOpenCorpse extends ActionBase {
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
            return false;
        }
        PlayerRef playerRefComponent = store.getComponent(playerReference, PlayerRef.getComponentType());
        if (playerRefComponent == null) {
            return false;
        }
        Player playerComponent = store.getComponent(playerReference, Player.getComponentType());
        if (playerComponent == null) {
            return false;
        }
        String corpseId = corpseManager.findCorpseIdByRef(ref);
        if (corpseId == null) {
            UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
            if (uuidComponent != null) {
                corpseId = corpseManager.findCorpseIdByEntityUuid(uuidComponent.getUuid());
            }
        }
        if (corpseId == null) {
            return false;
        }
        CorpseData corpseData = corpseManager.getCorpse(corpseId);
        if (corpseData == null) {
            return false;
        }
        if (!corpseManager.canPlayerLoot(playerRefComponent.getUuid(), corpseData)) {
            return false;
        }
        CorpseWindow window = new CorpseWindow(corpseManager, corpseId, corpseData);
        return playerComponent.getPageManager().setPageWithWindows(playerReference, store, Page.Bench, true, window);
    }
}

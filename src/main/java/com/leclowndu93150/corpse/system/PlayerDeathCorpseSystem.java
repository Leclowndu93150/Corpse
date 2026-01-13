package com.leclowndu93150.corpse.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.PlayerSkin;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.leclowndu93150.corpse.data.CorpseData;
import com.leclowndu93150.corpse.data.SerializedItemStack;
import com.leclowndu93150.corpse.manager.CorpseManager;
import it.unimi.dsi.fastutil.Pair;

import com.hypixel.hytale.logger.HytaleLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class PlayerDeathCorpseSystem extends RefChangeSystem<EntityStore, DeathComponent> {
    private static final String CORPSE_ROLE_NAME = "Corpse";
    private static final HytaleLogger LOGGER = HytaleLogger.get("Corpse");
    private final CorpseManager corpseManager;
    private final ComponentType<EntityStore, Player> playerComponentType;

    public PlayerDeathCorpseSystem(@Nonnull CorpseManager corpseManager, @Nonnull ComponentType<EntityStore, Player> playerComponentType) {
        this.corpseManager = corpseManager;
        this.playerComponentType = playerComponentType;
    }

    @Override
    @Nonnull
    public ComponentType<EntityStore, DeathComponent> componentType() {
        return DeathComponent.getComponentType();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return playerComponentType;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component,
                                  @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        LOGGER.at(Level.INFO).log("onComponentAdded called - DeathComponent detected");
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            LOGGER.at(Level.INFO).log("PlayerRef is null, skipping");
            return;
        }
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        HeadRotation headRotation = commandBuffer.getComponent(ref, HeadRotation.getComponentType());
        Vector3d position = new Vector3d(transform.getPosition());
        Vector3f playerRotation = headRotation != null ? headRotation.getRotation() : new Vector3f(0, 0, 0);
        World world = commandBuffer.getExternalData().getWorld();
        Inventory inventory = player.getInventory();
        if (inventory == null) {
            return;
        }
        List<SerializedItemStack> hotbarItems = extractItems(inventory.getHotbar());
        List<SerializedItemStack> storageItems = extractItems(inventory.getStorage());
        List<SerializedItemStack> armorItems = extractItems(inventory.getArmor());
        List<SerializedItemStack> utilityItems = extractItems(inventory.getUtility());
        if (isAllEmpty(hotbarItems) && isAllEmpty(storageItems) && isAllEmpty(armorItems) && isAllEmpty(utilityItems)) {
            LOGGER.at(Level.INFO).log("All inventory slots empty, no corpse needed");
            return;
        }
        LOGGER.at(Level.INFO).log("Player has items, creating corpse");
        String corpseId = UUID.randomUUID().toString();
        PlayerSkinComponent skinComponent = commandBuffer.getComponent(ref, PlayerSkinComponent.getComponentType());
        PlayerSkin playerSkin = skinComponent != null ? skinComponent.getPlayerSkin() : null;
        CorpseData corpseData = new CorpseData(
            corpseId,
            playerRef.getUuid(),
            playerRef.getUsername(),
            world.getName(),
            position.getX(),
            position.getY(),
            position.getZ(),
            playerRotation.getY(),
            playerRotation.getX(),
            System.currentTimeMillis(),
            hotbarItems,
            storageItems,
            armorItems,
            utilityItems
        );
        corpseManager.addCorpse(corpseData);
        inventory.clear();
        world.execute(() -> {
            spawnCorpseEntity(world, store, corpseId, position, playerRotation, playerSkin);
        });
    }

    private void spawnCorpseEntity(World world, Store<EntityStore> store, String corpseId,
                                   Vector3d position, Vector3f playerRotation, PlayerSkin playerSkin) {
        LOGGER.at(Level.INFO).log("Attempting to spawn corpse entity at %s", position);
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            LOGGER.at(Level.SEVERE).log("NPCPlugin is null!");
            return;
        }
        int roleIndex = npcPlugin.getIndex(CORPSE_ROLE_NAME);
        LOGGER.at(Level.INFO).log("Role index for '%s': %d", CORPSE_ROLE_NAME, roleIndex);
        if (roleIndex < 0) {
            LOGGER.at(Level.SEVERE).log("Role '%s' not found! Cannot spawn corpse.", CORPSE_ROLE_NAME);
            return;
        }
        Vector3f corpseRotation = new Vector3f(
            (float)(Math.PI / 2.0),
            playerRotation.getY(),
            0.0f
        );
        Model corpseModel = null;
        TriConsumer<NPCEntity, Ref<EntityStore>, Store<EntityStore>> skinApplyingFunction = null;
        if (playerSkin != null) {
            LOGGER.at(Level.INFO).log("Player has skin, creating model");
            try {
                corpseModel = CosmeticsModule.get().createModel(playerSkin);
                if (corpseModel != null && corpseModel.getScale() <= 0.0f) {
                    LOGGER.at(Level.WARNING).log("Model has invalid scale, using null model instead");
                    corpseModel = null;
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).withCause(e).log("Failed to create model from skin: %s", e.getMessage());
                corpseModel = null;
            }
            final PlayerSkin skin = playerSkin;
            skinApplyingFunction = (npcEntity, entityStoreRef, entityStore) -> {
                entityStore.putComponent(entityStoreRef, PlayerSkinComponent.getComponentType(), new PlayerSkinComponent(skin));
            };
        } else {
            LOGGER.at(Level.INFO).log("Player has no skin");
        }
        try {
            LOGGER.at(Level.INFO).log("Calling npcPlugin.spawnEntity...");
            Pair<Ref<EntityStore>, NPCEntity> corpsePair = npcPlugin.spawnEntity(
                store,
                roleIndex,
                position,
                corpseRotation,
                corpseModel,
                skinApplyingFunction
            );
            if (corpsePair != null) {
                Ref<EntityStore> corpseRef = corpsePair.first();
                store.ensureComponent(corpseRef, Frozen.getComponentType());
                corpseManager.registerCorpseEntity(corpseId, corpseRef);
                LOGGER.at(Level.INFO).log("Corpse entity spawned successfully! ID: %s", corpseId);
            } else {
                LOGGER.at(Level.SEVERE).log("npcPlugin.spawnEntity returned null!");
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Exception spawning corpse entity: %s", e.getMessage());
        }
    }

    private List<SerializedItemStack> extractItems(ItemContainer container) {
        List<SerializedItemStack> items = new ArrayList<>();
        short capacity = container.getCapacity();
        for (short i = 0; i < capacity; i++) {
            ItemStack stack = container.getItemStack(i);
            items.add(SerializedItemStack.fromItemStack(stack));
        }
        return items;
    }

    private boolean isAllEmpty(List<SerializedItemStack> items) {
        for (SerializedItemStack item : items) {
            if (item != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref, DeathComponent oldComponent,
                               @Nonnull DeathComponent newComponent, @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component,
                                    @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }
}

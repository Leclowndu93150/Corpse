package com.leclowndu93150.corpse.system;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.function.consumer.TriConsumer;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.leclowndu93150.corpse.data.CorpseData;
import com.leclowndu93150.corpse.data.SerializedItemStack;
import com.leclowndu93150.corpse.manager.CorpseManager;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

public class PlayerDeathCorpseSystem extends RefChangeSystem<EntityStore, DeathComponent> {
    private static final String CORPSE_ROLE_NAME = "Corpse";
    private static final float CORPSE_PITCH = (float) (Math.PI / 2.0);
    private static final float CORPSE_YAW_OFFSET = (float) (-Math.PI / 2.0);
    private static final float YAW_SNAP_STEP = (float) (Math.PI / 2.0);
    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
        new SystemDependency<>(Order.AFTER, DeathSystems.PlayerDropItemsConfig.class),
        new SystemDependency<>(Order.BEFORE, DeathSystems.DropPlayerDeathItems.class)
    );
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
    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component,
                                  @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
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
        float playerYaw = transform.getRotation().getYaw();
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
            return;
        }
        component.setItemsLossMode(DeathConfig.ItemsLossMode.NONE);
        String corpseId = UUID.randomUUID().toString();
        String playerName = playerRef.getUsername();
        float corpseYaw = snapYawToCardinal(playerYaw + CORPSE_YAW_OFFSET);
        CorpseData corpseData = new CorpseData(
            corpseId,
            playerRef.getUuid(),
            playerRef.getUsername(),
            world.getName(),
            position.getX(),
            position.getY(),
            position.getZ(),
            corpseYaw,
            playerRotation.getX(),
            System.currentTimeMillis(),
            hotbarItems,
            storageItems,
            armorItems,
            utilityItems
        );
        corpseManager.addCorpse(corpseData);
        inventory.clear();

        String locationMessage = String.format("A corpse has been created at: X: %.0f, Y: %.0f, Z: %.0f",
            position.getX(), position.getY(), position.getZ());
        player.sendMessage(Message.raw(locationMessage));

        world.execute(() -> {
            spawnCorpseEntity(world, store, corpseId, position, corpseYaw, playerName);
        });
    }

    private void spawnCorpseEntity(World world, Store<EntityStore> store, String corpseId,
                                   Vector3d position, float corpseYaw, String playerName) {
        NPCPlugin npcPlugin = NPCPlugin.get();
        if (npcPlugin == null) {
            return;
        }
        int roleIndex = npcPlugin.getIndex(CORPSE_ROLE_NAME);
        if (roleIndex < 0) {
            return;
        }
        Vector3f corpseRotation = new Vector3f(CORPSE_PITCH, corpseYaw, 0.0f);
        String displayName = playerName + "'s Corpse";
        TriConsumer<NPCEntity, Holder<EntityStore>, Store<EntityStore>> preAddToWorld = (npcEntity, holder, entityStore) -> {
            holder.putComponent(DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.raw(displayName)));
            holder.putComponent(Nameplate.getComponentType(), new Nameplate(displayName));
        };
        try {
            Pair<Ref<EntityStore>, NPCEntity> corpsePair = npcPlugin.spawnEntity(
                store, roleIndex, position, corpseRotation, null, preAddToWorld, null
            );
            if (corpsePair != null) {
                Ref<EntityStore> corpseRef = corpsePair.first();
                corpseManager.registerCorpseEntity(corpseId, corpseRef);
                UUIDComponent uuidComponent = store.getComponent(corpseRef, UUIDComponent.getComponentType());
                if (uuidComponent != null) {
                    UUID entityUuid = uuidComponent.getUuid();
                    corpseManager.registerCorpseEntityUuid(corpseId, entityUuid);
                    CorpseData existingData = corpseManager.getCorpse(corpseId);
                    if (existingData != null) {
                        corpseManager.updateCorpse(existingData.withEntityUuid(entityUuid));
                    }
                }
            }
        } catch (Exception e) {
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

    private float snapYawToCardinal(float yaw) {
        return Math.round(yaw / YAW_SNAP_STEP) * YAW_SNAP_STEP;
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

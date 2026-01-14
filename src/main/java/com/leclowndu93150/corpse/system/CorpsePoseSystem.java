package com.leclowndu93150.corpse.system;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.protocol.EntityPart;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelParticle;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.system.TransformSystems;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.systems.RoleSystems;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

public class CorpsePoseSystem extends EntityTickingSystem<EntityStore> {
    private static final String CORPSE_ROLE_NAME = "Corpse";
    private static final float CORPSE_PITCH = (float) (Math.PI / 2.0);
    private static final String[] EXACT_PARTICLE_IDS = {"Effect_Heal", "Aura_Sphere"};
    private static String resolvedParticleId = null;
    private static final AtomicBoolean particleSearchDone = new AtomicBoolean(false);
    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
        new SystemDependency<>(Order.AFTER, RoleSystems.PostBehaviourSupportTickSystem.class),
        new SystemDependency<>(Order.BEFORE, TransformSystems.EntityTrackerUpdate.class)
    );
    private final ComponentType<EntityStore, NPCEntity> npcComponentType = NPCEntity.getComponentType();
    private final ComponentType<EntityStore, TransformComponent> transformComponentType = TransformComponent.getComponentType();
    private final ComponentType<EntityStore, HeadRotation> headRotationComponentType = HeadRotation.getComponentType();
    private final ComponentType<EntityStore, BoundingBox> boundingBoxComponentType = BoundingBox.getComponentType();
    private final ComponentType<EntityStore, ModelComponent> modelComponentType = ModelComponent.getComponentType();
    private final Query<EntityStore> query = Archetype.of(npcComponentType, transformComponentType);
    private static final float CORPSE_EYE_HEIGHT = 0.3f;

    @Override
    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        NPCEntity npc = archetypeChunk.getComponent(index, npcComponentType);
        if (npc == null || !CORPSE_ROLE_NAME.equals(npc.getRoleName())) {
            return;
        }
        TransformComponent transform = archetypeChunk.getComponent(index, transformComponentType);
        if (transform == null) {
            return;
        }
        Vector3f bodyRotation = transform.getRotation();
        bodyRotation.setPitch(CORPSE_PITCH);
        bodyRotation.setRoll(0.0f);

        HeadRotation headRotation = archetypeChunk.getComponent(index, headRotationComponentType);
        if (headRotation != null) {
            Vector3f lookRotation = headRotation.getRotation();
            lookRotation.setYaw(bodyRotation.getYaw());
            lookRotation.setPitch(CORPSE_PITCH);
            lookRotation.setRoll(0.0f);
        }

        ModelComponent modelComponent = archetypeChunk.getComponent(index, modelComponentType);
        if (modelComponent == null) {
            return;
        }
        Model model = modelComponent.getModel();
        Model corpseModel = stripDetailBoxesAndAddParticles(model);
        if (corpseModel != model) {
            commandBuffer.putComponent(archetypeChunk.getReferenceTo(index), modelComponentType, new ModelComponent(corpseModel));
        }

        BoundingBox boundingBox = archetypeChunk.getComponent(index, boundingBoxComponentType);
        if (boundingBox != null) {
            Box baseBox = corpseModel.getBoundingBox();
            if (baseBox != null && baseBox.width() > 0.0 && baseBox.height() > 0.0 && baseBox.depth() > 0.0) {
                double width = baseBox.width();
                double height = baseBox.height();
                double depth = baseBox.depth();
                double halfHeight = height / 2.0;
                Box lyingBox = new Box(
                    -width / 2.0, 0.0, -halfHeight,
                    width / 2.0, depth, halfHeight
                );
                boundingBox.setBoundingBox(lyingBox);
            }
        }

        if (!particleSearchDone.getAndSet(true)) {
            searchAndLogParticles();
        }
    }

    private static void searchAndLogParticles() {
        try {
            var assetMap = ParticleSystem.getAssetMap().getAssetMap();
            Set<String> allParticleIds = assetMap.keySet();
            for (String exactId : EXACT_PARTICLE_IDS) {
                for (String registryId : allParticleIds) {
                    if (registryId.endsWith(exactId) || registryId.equals(exactId)) {
                        if (resolvedParticleId == null) {
                            resolvedParticleId = registryId;
                        }
                        break;
                    }
                }
                if (resolvedParticleId != null) break;
            }
        } catch (Exception e) {
        }
    }

    private static Model stripDetailBoxesAndAddParticles(@Nonnull Model model) {
        boolean hasDetailBoxes = model.getDetailBoxes() != null && !model.getDetailBoxes().isEmpty();
        boolean needsParticles = resolvedParticleId != null && (model.getParticles() == null || model.getParticles().length == 0);
        boolean needsLowerEyeHeight = model.getEyeHeight() > CORPSE_EYE_HEIGHT;

        if (!hasDetailBoxes && !needsParticles && !needsLowerEyeHeight) {
            return model;
        }

        ModelParticle[] particles = model.getParticles();
        if (needsParticles) {
            ModelParticle corpseParticle = new ModelParticle(
                resolvedParticleId,
                EntityPart.Self,
                null,
                null,
                1.0f,
                new com.hypixel.hytale.protocol.Vector3f(0.0f, 0.5f, 0.0f),
                null,
                false
            );
            particles = new ModelParticle[]{corpseParticle};
        }

        return new Model(
            model.getModelAssetId(),
            model.getScale(),
            model.getRandomAttachmentIds(),
            model.getAttachments(),
            model.getBoundingBox(),
            model.getModel(),
            model.getTexture(),
            model.getGradientSet(),
            model.getGradientId(),
            CORPSE_EYE_HEIGHT,
            model.getCrouchOffset(),
            model.getAnimationSetMap(),
            model.getCamera(),
            model.getLight(),
            particles,
            model.getTrails(),
            model.getPhysicsValues(),
            hasDetailBoxes ? null : model.getDetailBoxes(),
            model.getPhobia(),
            model.getPhobiaModelAssetId()
        );
    }
}

package com.leclowndu93150.corpse.mixin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import java.util.Map;
import java.util.logging.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Model.ModelReference.class)
public abstract class ModelReferenceMixin {
    private static final HytaleLogger LOGGER = HytaleLogger.get("Corpse");

    @Shadow private String modelAssetId;
    @Shadow private float scale;
    @Shadow private Map<String, String> randomAttachmentIds;
    @Shadow private boolean staticModel;

    @Inject(method = "toModel", at = @At("HEAD"), cancellable = true)
    private void corpse$clampScale(CallbackInfoReturnable<Model> cir) {
        if (this.modelAssetId == null || this.scale > 0.0f) {
            return;
        }
        ModelAsset modelAsset = ModelAsset.getAssetMap().getAsset(this.modelAssetId);
        if (modelAsset == null) {
            modelAsset = ModelAsset.DEBUG;
        }
        float effectiveScale = modelAsset.generateRandomScale();
        LOGGER.at(Level.WARNING).log(
            "Clamped invalid model scale %.3f for model %s to %.3f",
            this.scale,
            this.modelAssetId,
            effectiveScale
        );
        cir.setReturnValue(Model.createScaledModel(modelAsset, effectiveScale, this.randomAttachmentIds, null, this.staticModel));
    }
}

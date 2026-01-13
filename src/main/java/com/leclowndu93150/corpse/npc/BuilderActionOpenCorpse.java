package com.leclowndu93150.corpse.npc;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.asset.builder.InstructionType;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.leclowndu93150.corpse.manager.CorpseManager;
import java.util.EnumSet;
import javax.annotation.Nonnull;

public class BuilderActionOpenCorpse extends BuilderActionBase {
    private static CorpseManager corpseManager;

    public static void setCorpseManager(@Nonnull CorpseManager manager) {
        corpseManager = manager;
    }

    @Override
    @Nonnull
    public String getShortDescription() {
        return "Open the corpse inventory UI for the current player";
    }

    @Override
    @Nonnull
    public String getLongDescription() {
        return this.getShortDescription();
    }

    @Override
    @Nonnull
    public Action build(@Nonnull BuilderSupport builderSupport) {
        return new ActionOpenCorpse(this, builderSupport, corpseManager);
    }

    @Override
    @Nonnull
    public BuilderDescriptorState getBuilderDescriptorState() {
        return BuilderDescriptorState.Stable;
    }

    @Nonnull
    public BuilderActionOpenCorpse readConfig(@Nonnull JsonElement data) {
        this.requireInstructionType(EnumSet.of(InstructionType.Interaction));
        return this;
    }
}

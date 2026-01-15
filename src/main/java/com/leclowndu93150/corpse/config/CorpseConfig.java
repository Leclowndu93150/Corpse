package com.leclowndu93150.corpse.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CorpseConfig {
    public static final BuilderCodec<CorpseConfig> CODEC = BuilderCodec
        .builder(CorpseConfig.class, CorpseConfig::new)
        .append(new KeyedCodec<>("AllowOtherPlayersToLoot", Codec.BOOLEAN), CorpseConfig::setAllowOtherPlayersToLoot, CorpseConfig::isAllowOtherPlayersToLoot).add()
        .append(new KeyedCodec<>("EnableLootableByAnyoneAfterSeconds", Codec.BOOLEAN), CorpseConfig::setEnableLootableByAnyoneAfterSeconds, CorpseConfig::isEnableLootableByAnyoneAfterSeconds).add()
        .append(new KeyedCodec<>("LootableByAnyoneAfterSeconds", Codec.INTEGER), CorpseConfig::setLootableByAnyoneAfterSeconds, CorpseConfig::getLootableByAnyoneAfterSeconds).add()
        .append(new KeyedCodec<>("EnableDespawnAfterSeconds", Codec.BOOLEAN), CorpseConfig::setEnableDespawnAfterSeconds, CorpseConfig::isEnableDespawnAfterSeconds).add()
        .append(new KeyedCodec<>("DespawnAfterSeconds", Codec.INTEGER), CorpseConfig::setDespawnAfterSeconds, CorpseConfig::getDespawnAfterSeconds).add()
        .build();

    private boolean allowOtherPlayersToLoot = false;
    private boolean enableLootableByAnyoneAfterSeconds = false;
    private int lootableByAnyoneAfterSeconds = 300;
    private boolean enableDespawnAfterSeconds = false;
    private int despawnAfterSeconds = 3600;

    public CorpseConfig() {
    }

    public boolean isAllowOtherPlayersToLoot() {
        return allowOtherPlayersToLoot;
    }

    public void setAllowOtherPlayersToLoot(boolean allowOtherPlayersToLoot) {
        this.allowOtherPlayersToLoot = allowOtherPlayersToLoot;
    }

    public int getLootableByAnyoneAfterSeconds() {
        return lootableByAnyoneAfterSeconds;
    }

    public void setLootableByAnyoneAfterSeconds(int lootableByAnyoneAfterSeconds) {
        this.lootableByAnyoneAfterSeconds = lootableByAnyoneAfterSeconds;
    }

    public int getDespawnAfterSeconds() {
        return despawnAfterSeconds;
    }

    public void setDespawnAfterSeconds(int despawnAfterSeconds) {
        this.despawnAfterSeconds = despawnAfterSeconds;
    }

    public boolean isEnableLootableByAnyoneAfterSeconds() {
        return enableLootableByAnyoneAfterSeconds;
    }

    public void setEnableLootableByAnyoneAfterSeconds(boolean enableLootableByAnyoneAfterSeconds) {
        this.enableLootableByAnyoneAfterSeconds = enableLootableByAnyoneAfterSeconds;
    }

    public boolean isEnableDespawnAfterSeconds() {
        return enableDespawnAfterSeconds;
    }

    public void setEnableDespawnAfterSeconds(boolean enableDespawnAfterSeconds) {
        this.enableDespawnAfterSeconds = enableDespawnAfterSeconds;
    }
}

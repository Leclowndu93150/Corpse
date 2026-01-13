package com.leclowndu93150.corpse.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class CorpseConfig {
    public static final BuilderCodec<CorpseConfig> CODEC = BuilderCodec
        .builder(CorpseConfig.class, CorpseConfig::new)
        .append(new KeyedCodec<>("AllowOtherPlayersToLoot", Codec.BOOLEAN), CorpseConfig::setAllowOtherPlayersToLoot, CorpseConfig::isAllowOtherPlayersToLoot).add()
        .build();

    private boolean allowOtherPlayersToLoot = false;

    public CorpseConfig() {
    }

    public boolean isAllowOtherPlayersToLoot() {
        return allowOtherPlayersToLoot;
    }

    public void setAllowOtherPlayersToLoot(boolean allowOtherPlayersToLoot) {
        this.allowOtherPlayersToLoot = allowOtherPlayersToLoot;
    }
}

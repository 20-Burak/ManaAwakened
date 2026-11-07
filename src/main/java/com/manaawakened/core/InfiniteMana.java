package com.manaawakened.core;

import com.manaawakened.config.ManaConfig;
import com.manaawakened.network.ModNetwork;
import com.manaawakened.registry.AttachmentRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

/**
 * Whether Circle 10 has infinite mana (1.0.4): the server config {@code allowInfiniteMana} and the gamerule
 * {@code manaAwakenedInfiniteMana}, both on by default. The server keeps each player's answer in {@link ManaData#infiniteManaAllowed()}
 * and syncs it; everything that treats Circle 10 as infinite asks {@link ManaData#hasInfiniteMana()}.
 */
public final class InfiniteMana {
    public static GameRules.Key<GameRules.BooleanValue> RULE;

    private InfiniteMana() {
    }

    /**
     * From common setup's enqueued work, on the main thread (mods are constructed in parallel and vanilla's rule table is a plain map),
     * still before any world's rules exist, so the rule is in Create World and in /gamerule.
     */
    public static void register() {
        RULE = GameRules.register("manaAwakenedInfiniteMana", GameRules.Category.PLAYER,
                GameRules.BooleanValue.create(true, (server, value) -> refreshAll(server)));
    }

    /** The effective value for this player: the config and the gamerule of the player's level. */
    public static boolean active(ServerPlayer player) {
        return ManaConfig.SERVER.allowInfiniteMana.get() && player.serverLevel().getGameRules().getBoolean(RULE);
    }

    /** Sets the player's answer; when it turns off, a Circle 10 core is filled once. The caller syncs. */
    public static void refresh(ServerPlayer player) {
        ManaData data = player.getData(AttachmentRegistries.MANA_DATA);
        boolean now = active(player);
        if (data.infiniteManaAllowed() && !now && data.isTranscendent()) {
            data.setMana(data.maxMana());
        }
        data.setInfiniteMana(now);
    }

    /** Every online player, each in their own level: after the gamerule changed or the server config was reloaded. */
    public static void refreshAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refresh(player);
            ModNetwork.syncNow(player, player.getData(AttachmentRegistries.MANA_DATA));
        }
    }
}

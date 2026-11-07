package com.manaawakened.gametest;

import com.manaawakened.ManaAwakened;
import com.manaawakened.client.hud.HudNumbers;
import com.manaawakened.command.ManaCommands;
import com.manaawakened.compat.MagicAdapters;
import com.manaawakened.config.ManaConfig;
import com.manaawakened.core.CoreModel;
import com.manaawakened.core.ExternalMagic;
import com.manaawakened.core.InfiniteMana;
import com.manaawakened.core.ManaData;
import com.manaawakened.network.SyncManaPayload;
import com.manaawakened.registry.AttachmentRegistries;
import com.manaawakened.server.CultivationRules;
import com.manaawakened.server.CultivationTicker;
import com.manaawakened.spell.Sigil;
import com.manaawakened.spell.SpellBook;
import com.manaawakened.spell.SpellCaster;
import com.manaawakened.spell.SpellRecipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 1.0.4: Circle 10's infinite mana is the server config {@code allowInfiniteMana} and the gamerule {@code manaAwakenedInfiniteMana}
 * (both on by default). The server syncs its answer to every player and fills a Circle 10 core once when it turns off. The rule and
 * the config are global, so every test sets and restores them within its own tick, in a batch of its own.
 */
@GameTestHolder(ManaAwakened.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InfiniteManaGameTests {
    private static final String EMPTY = "empty";
    private static final String BATCH = "infinite_mana";

    private InfiniteManaGameTests() {
    }

    private static void setRule(GameTestHelper helper, boolean value) {
        helper.getLevel().getGameRules().getRule(InfiniteMana.RULE).set(value, helper.getLevel().getServer());
    }

    private static void setConfig(GameTestHelper helper, boolean value) {
        ManaConfig.SERVER.allowInfiniteMana.set(value);
        // what the config reload event runs
        InfiniteMana.refreshAll(helper.getLevel().getServer());
    }

    private static void restore(GameTestHelper helper) {
        setRule(helper, true);
        setConfig(helper, true);
    }

    /** The last mana snapshot the server sent this player since the last call, or null. */
    private static SyncManaPayload lastSync(ServerPlayer player) {
        SyncManaPayload last = null;
        for (Object sent : TestPlayers.sentSinceLastCall(player)) {
            if (sent instanceof SyncManaPayload payload) {
                last = payload;
            }
        }
        return last;
    }

    private static ServerPlayer transcendent(GameTestHelper helper) {
        ServerPlayer player = TestPlayers.mock(helper);
        ManaData data = player.getData(AttachmentRegistries.MANA_DATA);
        data.setCircle(CoreModel.CIRCLES);
        data.setCoreCapacity(CultivationRules.required(CoreModel.CIRCLES - 1));
        CultivationRules.normalizeCapacity(data);
        data.setMana(data.maxMana());
        return player;
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void theFlagSentFollowsLoginAndTheGamerule(GameTestHelper helper) {
        try {
            helper.assertTrue(helper.getLevel().getGameRules().getBoolean(InfiniteMana.RULE) && ManaConfig.SERVER.allowInfiniteMana.get(),
                    "both are on by default");
            ServerPlayer player = TestPlayers.mock(helper);
            ManaData data = player.getData(AttachmentRegistries.MANA_DATA);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
            SyncManaPayload sent = lastSync(player);
            helper.assertTrue(sent != null && sent.infiniteMana() && data.infiniteManaAllowed(), "on after login");

            setRule(helper, false);
            sent = lastSync(player);
            helper.assertTrue(sent != null && !sent.infiniteMana() && !data.infiniteManaAllowed(), "the rule turned off is sent at once");

            ServerPlayer late = TestPlayers.mock(helper);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(late));
            sent = lastSync(late);
            helper.assertTrue(sent != null && !sent.infiniteMana(), "a login into a world with the rule off is told so");

            setRule(helper, true);
            sent = lastSync(player);
            helper.assertTrue(sent != null && sent.infiniteMana() && data.infiniteManaAllowed(), "and on again at once");
            player.discard();
            late.discard();
        } finally {
            restore(helper);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void theConfigTurnsItOffWhateverTheGameruleSays(GameTestHelper helper) {
        try {
            ServerPlayer player = transcendent(helper);
            ManaData data = player.getData(AttachmentRegistries.MANA_DATA);
            TestPlayers.sentSinceLastCall(player);
            setConfig(helper, false);
            SyncManaPayload sent = lastSync(player);
            helper.assertTrue(helper.getLevel().getGameRules().getBoolean(InfiniteMana.RULE), "the rule is still on");
            helper.assertTrue(!InfiniteMana.active(player) && !data.hasInfiniteMana(), "config false wins over the rule");
            helper.assertTrue(sent != null && !sent.infiniteMana(), "and the client is told");
            setConfig(helper, true);
            sent = lastSync(player);
            helper.assertTrue(data.hasInfiniteMana() && sent != null && sent.infiniteMana(), "config back on: infinite again");
            player.discard();
        } finally {
            restore(helper);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, batch = BATCH)
    public static void turningItOffFillsACircleTenCoreOnce(GameTestHelper helper) {
        try {
            ServerPlayer player = transcendent(helper);
            ManaData data = player.getData(AttachmentRegistries.MANA_DATA);
            ServerPlayer mortal = TestPlayers.mock(helper);
            ManaData mortalData = mortal.getData(AttachmentRegistries.MANA_DATA);
            data.setMana(data.maxMana() * 0.25);
            mortalData.setMana(1.0);
            setRule(helper, false);
            helper.assertTrue(data.mana() == data.maxMana(), "ON -> OFF fills the Circle 10 core: " + data.mana() + " / " + data.maxMana());
            helper.assertTrue(mortalData.mana() == 1.0, "a lower Circle is not filled");
            data.setMana(data.maxMana() * 0.25);
            setRule(helper, false);
            helper.assertTrue(data.mana() == data.maxMana() * 0.25, "only once: staying off fills nothing (the sync's capacity check included)");
            player.discard();
            mortal.discard();
        } finally {
            restore(helper);
        }
        helper.succeed();
    }

    /** Saves and loads {@code player}'s mana data as a relog would, optionally without the infinite_mana field (a 1.0.3 save). */
    private static ManaData relog(GameTestHelper helper, ServerPlayer player, boolean dropFlag) {
        CompoundTag saved = (CompoundTag) ManaData.SERIALIZER.write(player.getData(AttachmentRegistries.MANA_DATA), helper.getLevel().registryAccess());
        if (dropFlag) {
            saved.remove("infinite_mana");
        }
        ManaData loaded = ManaData.SERIALIZER.read(null, saved, helper.getLevel().registryAccess());
        player.setData(AttachmentRegistries.MANA_DATA, loaded);
        TestPlayers.sentSinceLastCall(player);
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(player));
        return loaded;
    }

    /** A relog into a world where it is off: the core was filled when it turned off, and the saved false keeps it from filling again. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void aRelogIntoAnOffWorldFillsNothing(GameTestHelper helper) {
        try {
            ServerPlayer player = transcendent(helper);
            setRule(helper, false);
            ManaData data = player.getData(AttachmentRegistries.MANA_DATA);
            data.setMana(data.maxMana() * 0.25);
            double quarter = data.mana();
            ManaData loaded = relog(helper, player, false);
            SyncManaPayload sent = lastSync(player);
            helper.assertTrue(loaded.mana() == quarter, "a relog refills nothing: " + loaded.mana() + " / " + loaded.maxMana());
            helper.assertTrue(!loaded.infiniteManaAllowed() && sent != null && !sent.infiniteMana(), "and the flag stays off");
            player.discard();
        } finally {
            restore(helper);
        }
        helper.succeed();
    }

    /** A save from before 1.0.4 has no flag: it reads as on, so the first login into a world where it is off fills the core once. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void anOldSaveIsFilledOnceOnItsFirstLoginIntoAnOffWorld(GameTestHelper helper) {
        try {
            ServerPlayer player = transcendent(helper);
            setRule(helper, false);
            ManaData data = player.getData(AttachmentRegistries.MANA_DATA);
            data.setMana(data.maxMana() * 0.25);
            ManaData loaded = relog(helper, player, true);
            SyncManaPayload sent = lastSync(player);
            helper.assertTrue(loaded.mana() == loaded.maxMana(), "the old save is filled once: " + loaded.mana() + " / " + loaded.maxMana());
            helper.assertTrue(!loaded.infiniteManaAllowed() && sent != null && !sent.infiniteMana(), "and is told it is off");
            loaded.setMana(loaded.maxMana() * 0.25);
            ManaData again = relog(helper, player, false);
            helper.assertTrue(again.mana() == again.maxMana() * 0.25, "the next relog fills nothing");
            player.discard();
        } finally {
            restore(helper);
        }
        helper.succeed();
    }

    /** Our own spells: free while infinite mana is on (the default), paid in full while it is off, refused from a short pool. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void aCircleTenSpellIsFreeWhileOnAndPaidWhileOff(GameTestHelper helper) {
        try {
            ServerPlayer player = transcendent(helper);
            ManaData data = player.getData(AttachmentRegistries.MANA_DATA);
            SpellBook book = player.getData(AttachmentRegistries.SPELL_BOOK);
            book.unlock(Sigil.WARD);
            SpellRecipe onWard = book.create(Sigil.WARD, "on");
            SpellRecipe offWard = book.create(Sigil.WARD, "off");
            SpellRecipe shortWard = book.create(Sigil.WARD, "short");
            double full = data.mana();
            helper.assertTrue(SpellCaster.costFor(player, data, onWard) == 0.0 && SpellCaster.cast(player, onWard.id()) && data.mana() == full,
                    "on: the cast costs nothing and the pool is untouched");

            setRule(helper, false);
            double cost = SpellCaster.costFor(player, data, offWard);
            helper.assertTrue(cost > 0.0, "off: a Circle 10 spell has its real cost");
            helper.assertTrue(SpellCaster.cast(player, offWard.id()) && Math.abs((full - data.mana()) - cost) < 0.01,
                    "off: the cast spends exactly " + cost + ", took " + (full - data.mana()));
            data.setMana(cost / 2.0);
            helper.assertTrue(!SpellCaster.cast(player, shortWard.id()) && data.mana() == cost / 2.0, "off: a short pool refuses the cast and pays nothing");
            player.discard();
        } finally {
            restore(helper);
        }
        helper.succeed();
    }

    /** Off: other mods' spells are charged, the core refills like Circle 9's and every readout shows real numbers. */
    @GameTest(template = EMPTY, batch = BATCH)
    public static void offExternalSpellsChargeRegenRefillsAndNumbersAreReal(GameTestHelper helper) {
        try {
            ServerPlayer player = transcendent(helper);
            ManaData data = player.getData(AttachmentRegistries.MANA_DATA);
            helper.assertTrue(MagicAdapters.reportSpent(player, data, ExternalMagic.IRONS, 1.0) == 0.0 && "∞".equals(ManaCommands.manaText(data))
                    && "∞".equals(HudNumbers.amount(data).getString()), "on: external spells are free and the readouts show ∞");

            setRule(helper, false);
            double full = data.mana();
            double cost = MagicAdapters.scaledCost(data, ExternalMagic.IRONS, 1.0);
            double paid = MagicAdapters.reportSpent(player, data, ExternalMagic.IRONS, 1.0);
            helper.assertTrue(cost > 0.0 && Math.abs(paid - cost) < 1.0E-6 && Math.abs((full - data.mana()) - cost) < 1.0E-3,
                    "off: an external spell pays its scaled cost " + cost + ", paid " + paid);
            data.setMana(cost / 2.0);
            helper.assertTrue(MagicAdapters.reportSpent(player, data, ExternalMagic.IRONS, 1.0) == 0.0 && data.mana() == cost / 2.0,
                    "off: all or nothing, as for every other Circle");
            helper.assertTrue(MagicAdapters.mirrorUnits(data, ExternalMagic.IRONS) == 0L, "off: a mod is shown what the core can pay");

            double half = data.maxMana() * 0.5;
            data.setMana(half);
            CultivationTicker.tick(player, data);
            helper.assertTrue(data.mana() > half && data.mana() < data.maxMana(), "off: the core refills by regeneration, not at once: " + data.mana());
            helper.assertTrue(!data.isManaFull() && data.manaFraction() < 1.0, "off: the pool is not full");
            helper.assertTrue(!"∞".equals(ManaCommands.manaText(data)) && !"∞".equals(HudNumbers.amount(data).getString()), "off: real numbers");
            player.discard();
        } finally {
            restore(helper);
        }
        helper.succeed();
    }
}

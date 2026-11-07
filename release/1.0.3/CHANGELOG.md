# 1.0.3 - the Dragon Heart floats above the portal

A fix from a play-test of 1.0.2. **Back up your world once before you update** (as with every update).

- **The Ender Dragon's heart no longer falls into the void.** The Primordial Dragon Heart was dropped under the End island (at the height of 3 blocks, below the island's underside) and fell out of the world, so nobody ever saw it. Now, when the dragon's death animation ends and the exit portal opens, the heart appears **floating two blocks above the dragon egg's spot**, on top of the portal's pillar, spinning slowly in a haze of dragon breath.
- **Every dragon leaves a heart**: the first one and every respawned one (the chance is still `dragon_heart.dropChance`, default 1.0). `/kill` on a dragon gives one too.
- **The floating heart is safe**: nothing harms it, it never falls and never despawns. Walk into it or right-click it to take it; with a full inventory it drops at your feet. Hearts nobody has taken yet stack in the one floating heart (shown as "×3").
- **Other bosses in `mana_awakened:dragon_heart_bearers`** drop the heart where they die, and it is guarded there: it never despawns, nothing harms it, and if it falls out of the world it comes back to where it dropped.
- Nothing else changes: the egg, the exit portal and the vanilla fight are as before. Worlds and characters from 1.0.2, 1.0.1 and 1.0.0 load as they are.

## Coming from 1.0.0?

1.0.1 and 1.0.2 were not uploaded here on their own; 1.0.3 carries both. In short:

- **1.0.1, your own thresholds:** after Circle 0 your next threshold is five times the core you break through with, and your cap is twice your threshold; breaking through at the cap makes every later number ten times bigger per Circle, and every Circle you leave full doubles your training speed from then on. Your 1.0.0 character keeps its Circle, core, purity, refinements and passives; its new threshold comes from how full it left each Circle. Big numbers are short (1.23M, 4.56B, 7.89T). The Rune-Plate structure is now called a **Meditation Formation**. Spells of Iron's Spells, Ars Nouveau and Mahou Tsukai cost ten times less than in 1.0.0 (`integration.externalCostScale`).
- **1.0.2, no more spells from an empty core:** a spell of another magic mod starts only when your core holds its whole price, and then pays exactly that price, once; held and channelled spells stop when the core cannot pay the next step; "Not enough mana in your core: this spell costs N" appears for refused Iron's, Ars and Mahou spells.

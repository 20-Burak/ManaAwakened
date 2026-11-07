# 1.0.4 - a switch for Circle 10's infinite mana

A small update from a player's wish: Circle 10's infinite mana can now be turned off. **Back up your world once before you update** (as with every update).

- **New gamerule `manaAwakenedInfiniteMana`**: in Create World > More > Game Rules (under Player) or with `/gamerule manaAwakenedInfiniteMana false`. It holds for that world.
- **New server config `core.allowInfiniteMana`** (in `mana_awakened-server.toml`): `false` turns infinite mana off in every world of the server, whatever the gamerule says. For modpack makers.
- **Both are on by default**: nothing changes unless you turn one of them off. Circle 10 is exactly as in 1.0.3.
- **With it off, Circle 10 is a normal mana user**: it keeps its title, colours, aura, its doubled core and its shorter cooldown, but every spell, rite and passive pays its cost (Iron's Spells, Ars Nouveau and Mahou Tsukai spells too), a spell the core cannot pay is refused, the core refills like Circle 9's (a 1.95B core from empty in about 13 minutes), body heat builds again, and the HUD, the spell wheel and `/mana` show real numbers.
- **Turning it off fills every Circle 10 core once** (online players at once, the others on their next login). Turning it back on makes Circle 10 infinite again at once.
- **Client and server must both run 1.0.4.** Worlds and characters from 1.0.3 and earlier load as they are.

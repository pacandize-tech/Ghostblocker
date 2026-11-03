# Ghostblocker (Fabric, Minecraft 26.3)

A Hypixel SkyBlock quality-of-life mod. It can run alongside SkyHanni and Skyblocker.

## Features
### Stash: you choose what to take
- Ghostblocker never runs `/pickupstash` for you.
- Hypixel's "You have N items/materials stashed away!" message gets a **[Choose items]** button, which opens `/viewstash item` or `/viewstash material`.
- The stash menu gets a **search box**. Matching items are highlighted, everything else is dimmed, and you click only what you want. Ghostblocker never clicks for you.
- When Hypixel warns that your stash is almost full, you get an on-screen alert plus the choose buttons.

### Locks
| Keybind (default) | What it does |
|---|---|
| **L** (hover a slot in any inventory) | Lock or unlock that slot. Locked slots can't be clicked, number-key swapped, shift-clicked or dropped with Q. Locks are saved. |
| **K** | Hotbar slot lock: the scroll wheel and number keys can't change your held slot. Server-forced slot changes still go through. |
| **J** | Camera lock: mouse movement doesn't turn your view. |
| **Right Shift** or `/ghostblocker` | Settings |

### Dungeons
- **Secret routes:**
  - Detects which Catacombs room you're in and draws the best community route for it: a path line, then marked spots for **Etherwarp**, **Stonk**, **Superboom**, **Interact**, **Pearl** and **Bonzo**, then the secret itself.
  - A step only advances when you actually get the secret: open the chest or flip the lever, pick up the item, or kill the bat.
  - Pick between 4 route sets in settings: FOW (default, 110 rooms), Standard, Pearl and 3ppopka.
- **Show all secrets** (optional): marks every secret in the room from the Dungeon Rooms Mod database, and hides each one once you've collected it.
- **Terminal solver (F7):** highlights the correct slots for *Click in order!*, *Correct all the panes!*, *What starts with…?* and *Select all the COLOR items!*. Highlights only, no auto-clicking.
- **Three Weirdos solver:** tells you which NPC's chest has the reward.
- **Starred mob glow:** starred mobs glow while you're in the Catacombs.

| Keybind (default) | Secret routes |
|---|---|
| **]** | Next step |
| **[** | Previous step |
| **\\** | Switch route variant, for rooms with more than one route |

Settings also control how many upcoming steps are shown (1–5).

## Build and test
```
gradlew build
gradlew test
gradlew runClientGameTest
```
- `gradlew build` writes the jar to `build/libs/ghostblocker-1.0.0.jar`. Drop it in a 26.3 Fabric profile with Fabric API.
- `gradlew test` runs the solver and route-data unit tests.
- `gradlew runClientGameTest` runs an in-game test of every lock, overlay and the secret-route system, and saves screenshots to `build/run/clientGameTest/screenshots`.

## Credits and license
Ghostblocker is licensed under the **GNU GPL v3.0 or later** (see `LICENSE`), because it includes GPL-3.0 data and code from:
- **[Secret Routes Mod](https://github.com/yourboykyle/SecretRoutes)**, by yourboykyle, wyannnnn (R-aMcC), zzyyrraa, Itplays and 3ppopka. Ghostblocker uses its secret route files, and its route format and rendering approach.
- **[Dungeon Rooms Mod](https://github.com/Quantizr/DungeonRoomsMod)**, by Quantizr. Ghostblocker uses its secret locations database (`secretlocations.json`).

Room detection and `rooms.json` come from **[Odin](https://github.com/odtheking/Odin)** by odtheking, by way of Secret Routes Mod, under the BSD 3-Clause License (see `LICENSE-ODIN.txt`).

If you share Ghostblocker, you must also share its source code under the GPL.

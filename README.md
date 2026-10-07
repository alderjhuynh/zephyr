# Zephyr

Zephyr is a feature-rich client-side utility mod for Minecraft Fabric. It bundles combat helpers, movement tweaks, quality-of-life improvements, render / HUD tools, a seed cracker, a pathfinding bot, structure copy tools, fake players for testing, and a fully configurable click-GUI, all in one jar, with no extra downloads needed.

- Minecraft: `26.3`
- Loader: Fabric Loader `>= 0.19.5`
- Java: `25`
- Requires: Fabric API

---

## Installation

1. Install **Java 25**.
2. Install **Fabric Loader 0.19.5+** for Minecraft 26.3.
3. Install **Fabric API** for 26.3.
4. Download the Zephyr jar and drop it into your `mods/` folder.
5. Launch the game.

Your files live in:

```
.minecraft/config/zephyr/
  client.json       theme, HUD, Discord, stealth, autosave, popups
  modules.json      every module's on/off + settings
  keybinds.json     module + system keybinds
  profiles.json     named setups (see Profiles)
  cornerstone/      saved structure regions
  notebook.json     your personal notebook pages
```

To reset Zephyr, close the game and delete that folder.

---

## Quick start

| Action | Default key | What it does |
|---|---|---|
| Open menu | `L` + `Enter` (together) | Opens the click-GUI. Press again to close. |
| Cycle screen | `Tab` (while a Zephyr screen is open) | Switches Menu → Keybinds → Config → Profiles → … |
| Stealth Mode | `F6` | Hides all Zephyr HUD, toasts, and overlays instantly. Press again to restore. |

In the menu:

- Pick a tab on the left: **Combat / Movement / QoL / Disable**.
- Click a module to turn it on/off. Right-click (or expand) for its settings.
- Use the top bar / other screens for **Keybinds**, **Config** (theme, HUD, Discord…), **Profiles**, and **Credits**.

Type `.z` alone for version, active profile, enabled-module count, and prefix diagnostics.

---

## Combat

Helpers for crystals, anchors, maces, carts, and targeting. Open each module in the GUI for range / delay / safety settings.

| Module | Description |
|---|---|
| Anchor Aura | Charges a respawn anchor in a target's face and detonates it, shielding you behind glowstone. |
| Anchor Helper | Place → charge → shield → detonate flow. |
| Anime Protagonist | Automated combo fighter with its own target manager. |
| Auto Crystal | Places and detonates end crystals on a target's obsidian support. |
| Auto Place | Automatically places a chosen block on a hit entity. |
| Breach Swap / Density Swap | Weapon-swap helpers keyed to fall distance / attack state for mace tech. |
| Lunge Swap | Lunge with the spear. |
| Criticals | Sends falling packets to force crits / mace slams. |
| Crystal Helper | While holding right-click on obsidian, keeps placing + attacking crystals. |
| Hit Assist | Still sends the attack when you barely miss. |
| Insta Cart | Places rail + TNT minecart to catch your own flaming arrows. |
| Kill Aura | Automatic melee with target manager. |
| Knockback | Reduces knockback taken. |
| Pearl Catch | Catches a thrown Ender Pearl with a Wind Charge. |
| Reach | Extends attack reach. |
| Shield Breaker | Breaks shields automatically. |
| Spear Damage | Spoofs speed for spear damage without moving. |
| Target Strafe | Orbits your current target while you move. |
| Totem Pop Notifier | Chat/toast when nearby players pop totems. |
| Trigger Bot | Attacks whenever your crosshair is on an entity and cooldown is ready. |
| XBow Cart | Places rail + TNT minecart + fire to catch crossbow arrows. |

---

## Movement

| Module | Description |
|---|---|
| Aerodynamics | Extra air acceleration while sprinting. |
| Air Jump / Double Jump | Jump again mid-air. |
| Anti Hunger | Skips unnecessary sprint packets. |
| Auto Walk | Walk forward without holding `W`. |
| Blink | Holds back your position packets until disabled. |
| Elytra Boost | Accelerate while gliding. |
| Flight | Creative-style fly with speed settings. |
| High Jump | Jump higher. |
| Ice Speed | No more uncontrolled sliding on ice. |
| Jesus | Walk on water. |
| No Fall | Blocks fall-damage packets. |
| No Slowdown | Ignore slow from using items, webs, water. |
| Scaffold | Places a block under you as you walk. |
| Sprint | Auto-sprint. |
| Step | Step up full blocks. |
| Trident Boost | Dry Riptide boosts. |

---

## Quality of life + rendering

Everyday helpers, HUD widgets, and visual tools:

| Module | Description |
|---|---|
| AppleSkin | Hunger / saturation / exhaustion + predicted food healing on the HUD. |
| Armor Renderer / Inventory Renderer | Armor, held items + durability, and full inventory on the HUD. |
| Auto Tool | Swaps to the right tool as you mine. |
| Container ESP / Player ESP / Tracers | Outlines for chests/shulkers, glowing players (including invisible ones with Render Invisibility), and lines to nearby players. |
| Durability Swap | Stops you mining with an almost-broken tool. |
| Fast Attack / Fast Use / Hold Attack / Hold Use / Periodic Attack / Periodic Use | Repeat clicks for you, either as fast as possible or on a timer. |
| FreeCam | Detach the camera and fly while your body stays put. |
| FullBright | Full brightness anywhere. |
| Gui Move | Keep moving while a GUI is open. |
| Inventory Packets | Use crafting slots as extra storage by skipping close packets. |
| Item Restock | Refills totems / items from your inventory. |
| Jade | In-world tooltip (block state, crop growth, redstone, beehive, horse stats, entity health, mod name…). |
| Mouse Tweaks | RMB dragging, LMB dragging, scroll-wheel quick-move. |
| Pick Before Place | Pick-block automatically before placing. |
| Potion Saver | Tries to stretch potion durations. |
| Safe Walk | No walking off edges until you jump. |
| Shulker Box Tooltip | Full inventory preview on hover, with merged counts. |
| Sneak | Auto-sneak. |
| Speed Mine | Faster breaking via Haste / predicted damage. |
| Time Changer | Client-side time of day. |
| Xray | Outline chosen ores/blocks through walls (with a bypass-friendly mode). |
| Zoom | Smooth FOV zoom while held. |

---

## Disable

One toggle each, all in the **Disable** tab. Great for FPS, recording, or focus:

| Disabled |
|---|
| No axe stripping |
| No shovel pathing |
| No block-breaking cooldown |
| No block-breaking particles |
| No block outline |
| No bossbar |
| No scoreboard |
| No damage tilt |
| No dead-mob rendering or interaction |
| No explosion particles |
| No fire (first-person and world) |
| No first-person potion particles |
| No fluid fog |
| No distance fog |
| No nausea overlay |
| No rain effects |
| No nether-portal sound |
| No totem animation |
| Portals no longer close your GUI |

---

## Seedcracker

Crack the world seed just by exploring. Enable **Seedcracker** and walk near structures.

Tracked by default: Buried Treasure, Desert / Jungle Temples, End Cities + Pillars, Monuments, Swamp Huts, Shipwrecks, Outposts, Igloos, Trial Chambers, Dungeons. Optional: End Gateways, Emerald Ore, Desert Wells, Warped Fungus, Biomes.

- Render mode: X-ray boxes or normal highlights.
- Command: `.z seedcracker <cracker|data|finder|render|version|database|gui>`: inspect progress, toggle finders, open its screen.
- Data resets automatically when you leave the server.

---

## Bots: auto-walk and mining

- `.z path <x> <y> <z> [destructive]`: A* walk to coordinates. Add `destructive` to mine through walls and bridge gaps.
- `.z path stop`: cancel.
- `.z path task mine <block>`: walk to the nearest matching block and mine it.

---

## Cornerstone: copy areas as commands

Save a region once, replay it anywhere as `/setblock` + `/fill` commands (singleplayer / creative with permission).

```
.z cornerstone select            # selection mode: click two corners
.z cornerstone pos1 <x> <y> <z>  # or set corners manually (~ supported)
.z cornerstone pos2 <x> <y> <z>
.z cornerstone save <name> [air] # store it (air = include air)
.z cornerstone run <name> [here | at <x> <y> <z>]
.z cornerstone list
.z cornerstone delete <name>
.z cornerstone clear
.z cornerstone cancel
```

---

## Fake players (singleplayer only)

Spawn client-side bots for testing farms, combat, or redstone. Works offline / in singleplayer worlds only and never joins real servers.

```
.z player <name> spawn
.z player <name> kill | stop | use | attack | jump | sneak | sprint | …
```

---

## Notebook, kits, and the rest of `.z`

- `.z notebook [clear]`: personal book UI.
- `.z kit <create|delete|edit|load|list|preview> <name>`: item kits.
- `.z hotbar <save|restore> <1-9>`: hotbar snapshots.
- `.z find <type|name>` / `.z findblock <block>`: locate entities / blocks.
- `.z ghostblock <set|fill> …`: client-side preview blocks.
- `.z glow …`: highlight entities / blocks / areas for a few seconds.
- `.z look <block|angles|cardinal> …`: precise aiming.
- `.z pos [to|from <overworld|nether|end>]`: Nether coordinate conversion.
- `.z getdata …` / `.z uuid …` / `.z ping …` / `.z gamemode …` / `.z time …` / `.z tp …` — inspection and client utilities.
- `.z give …` / `.z creativetab …` / `.z enchant …` / `.z crackrng …`: creative / RNG tools (some are stubs or need creative/permissions).
- `.z alias …` / `.z config …` / `.z relog`: aliases, raw config get/set, quick reconnect.

Every command supports Tab-completion. Commands starting with `c` (e.g. `.z cfind …`) are the same command under a shorter alias.

---

## Profiles, keybinds, HUD & Discord

- **Profiles**: snapshot *everything* (every module + setting) under a name. Make `PvP`, `Building`, `Recording`… profiles and switch in one click. Switching re-applies exactly what you saved. Your current setup auto-updates the active profile as you change things.
- **Keybinds screen**: click any bind, press up to 3 keys for a combo, `Esc` to clear. Conflicts are highlighted and toasted.
- **Config screen**: theme color (presets or custom HSV), HUD overlay mode, notification corner + lifetime, menu animation speed, autosave interval, hotkey popups, keybind warnings, Discord presence, Stealth Mode.
- **HUD**: enabled-module list, armor/inventory widgets, AppleSkin bars, notifications, Jade tooltip, totem pops, and pathing/seedcracker gizmos. Turn the whole overlay off with `HudMode OFF` or hide everything instantly with Stealth Mode (`F6`).
- **Discord Rich Presence**: shows Minecraft version + where you are with session timer. Toggle in Config. No account linking needed.

---

## Fair play

Zephyr is powerful. Many features are considered cheats on survival / competitive servers.

- Use freely in singleplayer and on servers where you have permission.
- Check the server's rules before enabling combat / movement / Xray modules online.
- Stealth Mode only hides Zephyr's *display*. It does not make cheats undetectable.

You are responsible for how you use it.

Zephyr has no anticheat protection. It does not attempt to hide what you're doing.

---

## Building from source

```bash
./gradlew build
```

Requires JDK 25. CI builds on every push and uploads `build/libs/`.

---

## License / credits

- Author: **Auraea**
- Built on Fabric + Fabric API, with bundled Discord IPC and Seedfinding libraries. QoL pieces adapted from AppleSkin, Jade, ShulkerBoxTooltip, SeedcrackerX, Carpet, and Cornerstone concepts. Thanks to their authors.

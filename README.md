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

- **Anchor Aura**: charges a respawn anchor in a target's face and detonates it, shielding you behind glowstone.
- **Anchor Helper**: place → charge → shield → detonate flow.
- **Anime Protagonist**: automated combo fighter with its own target manager.
- **Auto Crystal**: places and detonates end crystals on a target's obsidian support.
- **Auto Place**: automatically places a chosen block on a hit entity.
- **Breach Swap / Density Swap**: weapon-swap helpers keyed to fall distance / attack state for mace tech.
- **Lunge Swap**: lunge with the spear.
- **Criticals**: sends falling packets to force crits / mace slams.
- **Crystal Helper**: while holding right-click on obsidian, keeps placing + attacking crystals.
- **Hit Assist**: still sends the attack when you barely miss.
- **Insta Cart**: places rail + TNT minecart to catch your own flaming arrows.
- **Kill Aura**: automatic melee with target manager.
- **Knockback**: reduces knockback taken.
- **Pearl Catch**: catches a thrown Ender Pearl with a Wind Charge.
- **Reach**: extends attack reach.
- **Shield Breaker**: breaks shields automatically.
- **Spear Damage**: spoofs speed for spear damage without moving.
- **Target Strafe**: orbits your current target while you move.
- **Totem Pop Notifier**: chat/toast when nearby players pop totems.
- **Trigger Bot**: attacks whenever your crosshair is on an entity and cooldown is ready.
- **XBow Cart**: places rail + TNT minecart + fire to catch crossbow arrows.

---

## Movement

- **Aerodynamics**: extra air acceleration while sprinting.
- **Air Jump / Double Jump**: jump again mid-air.
- **Anti Hunger**: skips unnecessary sprint packets.
- **Auto Walk**: walk forward without holding `W`.
- **Blink**: holds back your position packets until disabled.
- **Elytra Boost**: accelerate while gliding.
- **Flight**: creative-style fly with speed settings.
- **High Jump**: jump higher.
- **Ice Speed**: no more uncontrolled sliding on ice.
- **Jesus**: walk on water.
- **No Fall**: blocks fall-damage packets.
- **No Slowdown**: ignore slow from using items, webs, water.
- **Scaffold**: places a block under you as you walk.
- **Sprint**: auto-sprint.
- **Step**: step up full blocks.
- **Trident Boost**: dry Riptide boosts.

---

## Quality of life + rendering

Everyday helpers, HUD widgets, and visual tools:

- **AppleSkin**: hunger / saturation / exhaustion + predicted food healing on the HUD.
- **Armor Renderer / Inventory Renderer**: armor, held items + durability, and full inventory on the HUD.
- **Auto Tool**: swaps to the right tool as you mine.
- **Container ESP / Player ESP / Tracers**: outlines for chests/shulkers, glowing players (including invisible ones with Render Invisibility), and lines to nearby players.
- **Durability Swap**: stops you mining with an almost-broken tool.
- **Fast Attack / Fast Use / Hold Attack / Hold Use / Periodic Attack / Periodic Use**: repeat clicks for you, either as fast as possible or on a timer.
- **FreeCam**: detach the camera and fly while your body stays put.
- **FullBright**: full brightness anywhere.
- **Gui Move**: keep moving while a GUI is open.
- **Inventory Packets**: use crafting slots as extra storage by skipping close packets.
- **Item Restock**: refills totems / items from your inventory.
- **Jade**: in-world tooltip (block state, crop growth, redstone, beehive, horse stats, entity health, mod name…).
- **Mouse Tweaks**: RMB dragging, LMB dragging, scroll-wheel quick-move.
- **Pick Before Place**: pick-block automatically before placing.
- **Potion Saver**: tries to stretch potion durations.
- **Safe Walk**: no walking off edges until you jump.
- **Shulker Box Tooltip**: full inventory preview on hover, with merged counts.
- **Sneak**: auto-sneak.
- **Speed Mine**: faster breaking via Haste / predicted damage.
- **Time Changer**: client-side time of day.
- **Xray**: outline chosen ores/blocks through walls (with a bypass-friendly mode).
- **Zoom**: smooth FOV zoom while held.

---

## Disable

One toggle each, all in the **Disable** tab. Great for FPS, recording, or focus:

No axe stripping, no shovel pathing, no block-breaking cooldown, no block-breaking particles, no block outline, no bossbar, no scoreboard, no damage tilt, no dead-mob rendering or interaction, no explosion particles, no fire (first-person and world), no first-person potion particles, no fluid fog, no distance fog, no nausea overlay, no rain effects, no nether-portal sound, no totem animation, and portals no longer close your GUI.

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

## Troubleshooting

- **Menu won't open?** Press `L` and `Enter` *at the same time* while no screen is open. If you rebound it, check the Keybinds screen.
- **`.` doesn't open commands?** Your Command Prefix bind changed it. The prefix is whatever that bind is set to.
- **Module won't toggle from a key?** You may be typing in chat, in a Zephyr screen (module keys only work with no screen open), or have a conflict — look for the conflict toast.
- **Settings not saving?** They autosave every 60s by default (configurable) and always on quit. Check `config/zephyr/` is writable.
- **Discord status not showing?** Enable *Discord Presence* in Config and make sure the desktop Discord app is running.
- **Fresh start?** Close the game, delete `.minecraft/config/zephyr/`, relaunch.

---

## Building from source

You don't need this to *use* Zephyr — only to develop it.

```bash
./gradlew build
# jar lands in build/libs/
```

Requires JDK 25. CI builds on every push (`ubuntu-24.04`, Microsoft JDK 25) and uploads `build/libs/`.

---

## License / credits

- Author: **Auraea**
- License: **CC0-1.0** (public domain) — see `LICENSE`.
- Built on Fabric + Fabric API, with bundled Discord IPC and Seedfinding libraries. QoL pieces adapted from AppleSkin, Jade, ShulkerBoxTooltip, SeedcrackerX, Carpet, and Cornerstone concepts. Thanks to their authors.

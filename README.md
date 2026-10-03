# Zephyr

A client-side hacked client / utility mod for **Minecraft 1.21.1**, built on [Fabric](https://fabricmc.net/). Zephyr packs 89 modules into four categories, Movement, Combat, QoL, and Disable, with a fully clickable GUI, a chat command system, configurable keybinds, configurable profiles, and Discord Rich Presence.

> **Use at your own risk.** Zephyr modifies client behavior and may violate the rules of the servers you play on. Use it only on servers where such modifications are allowed.

---

## Requirements

| Dependency                                           | Version                     |
|------------------------------------------------------|-----------------------------|
| [Minecraft](https://www.minecraft.net/)              | 1.21.1                      |
| [Fabric Loader](https://fabricmc.net/use/installer/) | 0.19.3+                     |
| [Fabric API](https://fabricmc.net/use/)              | any recent build for 1.21.1 |
| Java                                                 | 21+                         |

---

## Installation

1. Install **Fabric Loader** for Minecraft 1.21.1 using the [Fabric installer](https://fabricmc.net/use/installer/).
2. Install **Fabric API** for Minecraft 1.21.1 (from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://curseforge.com/minecraft/mc-mods/fabric-api)).
3. Download the Zephyr `.jar` and drop it into your `.minecraft/mods` folder.
4. Launch Minecraft with the Fabric profile.

---

## Getting started

- Press **`L` + `Enter`** to open the main menu (a clickable module list with search and category tabs).
- Press **`Tab`** while the menu is open to cycle through the other screens (keybinds, profiles, global settings, and back to the module list). Holding **`Down`** or **`Up`** while pressing Tab cycles through the two hidden easter-egg screens, and holding **`Left`** cycles backward through the normal screens.
- **Left-click** a module to toggle it on or off; **right-click** it to open its settings panel.
- Press the **Command Prefix** key (default **`.`**) to open chat with the Zephyr prefix pre-typed and run a command like `.z module Flight on` (see [Commands](#commands)).
- Toggle state, settings, keybinds, and the active profile are all saved to `config/zephyr/` when you quit the game.

Everything, including the menu itself, can be rebound from the **Keybinds** screen. Keybinds support up to three keys held together (e.g. `G` or `Ctrl` + `B`). Stealth Mode is bound to **`F6`** by default.

## Global settings

The **Settings** screen holds client-wide options:

| Setting                   | Description                                                                                                 |
|---------------------------|-------------------------------------------------------------------------------------------------------------|
| Hotkey Popups             | Shows a toast whenever a module is toggled from a keybind                                                   |
| Theme Color               | Picks the accent color from six presets (Lavender, Sky, Mint, Gold, Coral, Rose)                            |
| Use Custom Color          | Replaces the preset with Hue/Saturation/Value sliders plus a live swatch                                    |
| Menu Animation Speed      | Scales the panel slide and toast slide-in speed                                                             |
| Notification Corner       | Which corner hotkey toasts slide into                                                                       |
| Notification Lifetime     | How long a toast holds before sliding out                                                                   |
| HUD Overlay               | Off, Minimal (watermark + profile), or Full (active modules plus an info stack with FPS, server, and theme) |
| Autosave Interval         | Seconds between periodic config saves; `0` disables autosave                                                |
| Keybind Conflict Warnings | Warns via toast when a new bind collides with an existing one                                               |
| Discord Presence          | Enables or disables Discord Rich Presence                                                                   |
| Stealth Mode              | Snapshots and force-disables every module, restoring them when turned off (default keybind: `F6`)           |

## Commands

Zephyr's chat commands give you quick control over the client without opening the GUI. The command prefix is set by the **Command Prefix** keybind (default **`.`**); any chat message starting with it is intercepted client-side and never sent to the server.

| Command                              | Description                                                                    |
|--------------------------------------|--------------------------------------------------------------------------------|
| `.z`                                 | Shows diagnostics: mod version, active profile, enabled/total module count, and the current command prefix |
| `.z module <name> <on\|off\|toggle>` | Controls a module by name, e.g. `.z module KillAura on`                        |
| `.z path <x> <y> <z> [destructive]`  | Walks to coordinates via A* (Pathing module); `destructive` mines/bridges     |
| `.z path stop` / `.z path task mine <block>` | Cancels pathing / walks to the nearest block and mines it            |
| `.z notebook [clear]`                | Opens the personal client-side notebook                                        |
| `.z player <name> spawn\|kill\|...`   | Singleplayer fake players for testing (spawn, kill, stop, use, attack, jump, sneak, sprint, look, turn, move, hotbar, drop, mount, shadow, list) |
| `.z cornerstone <subcommand>`        | Copies areas as setblock/fill commands (select, pos1/pos2, save, run, list, delete, clear, cancel) |
| `.z kit / .z hotbar / .z alias`      | Item kits, hotbar snapshots, command aliases                                   |
| `.z find / .z findblock`             | Locates entities / blocks                                                      |
| `.z ghostblock / .z glow`            | Client-side preview blocks / entity-area highlights                            |
| `.z look / .z pos / .z tp`           | Precise aiming, Nether coordinate conversion, teleport utilities               |
| `.z getdata / .z uuid / .z ping`     | Inspection utilities                                                           |
| `.z gamemode / .z time`              | Client gamemode / time helpers                                                 |
| `.z give / .z creativetab / .z enchant` | Creative inventory tools                                                    |
| `.z config / .z relog / .z permissionlevel` | Raw config get/set, quick reconnect, op-level check                     |

- Every subcommand also works with a `c` prefix alias (e.g. `.z cfind ...`).
- Tab-completion works in the chat box: type the prefix and start typing, and commands and arguments are suggested as you go.

- Tab-completion works in the chat box: type the prefix and start typing, and commands and arguments are suggested as you go.
- Names or arguments containing spaces can be double-quoted, e.g. `.z module "Anime Protagonist" toggle`.

## AppleSkin

**AppleSkin** is a QoL module that brings AppleSkin's food-related HUD improvements to Zephyr. While enabled it draws overlays on the vanilla HUD:

- **Saturation Overlay**: shows your current saturation as translucent icons over the food bar, plus the saturation the held food would add.
- **Exhaustion Underlay**: draws an exhaustion bar underneath the food bar.
- **Food Values Overlay**: while holding food, shows the hunger (and saturation) it would restore as outlined food icons over the bar.
- **Health Values Overlay**: while holding food, shows the health that food would eventually regenerate as extra hearts over the health bar.

Enable it from the click-GUI (Category: QoL) or with `.z module AppleSkin on`. Its settings panel controls each overlay independently, whether the offhand is checked when the main hand isn't food, whether the overlays mirror the vanilla bar bobbing animations, and the pulsing overlay flash strength.

> **Note:** this module is a port of [AppleSkin](https://github.com/squeek502/AppleSkin), adapted to Zephyr's module and rendering systems and 1.21.1 MojMaps.

## ShulkerBoxTooltip

**ShulkerBoxTooltip** is a QoL module that shows the contents of shulker boxes and other containers right inside their tooltip, without needing to open them. It is a port of the Fabric mod [ShulkerBoxTooltip](https://github.com/Minenash/ShulkerBoxTooltip).

While enabled it renders a preview of the hovered container's inventory in the item tooltip. You can preview a **Shulker Box** (in every dye color), **Chest**, **Trapped Chest**, **Barrel**, **Furnace**, **Blast Furnace**, **Smoker**, **Dropper**, **Dispenser**, **Hopper**, **Brewing Stand**, **Chiseled Bookshelf**, and **Decorated Pot**.

The module's settings control:

- **Tooltip Type**: `Mod` renders the custom ShulkerBoxTooltip window with per-item colored backgrounds, `Vanilla` renders a vanilla-style grid of slots.
- **Preview Mode**: `Full` shows the container's whole inventory, `Compact` collapses identical stacks and hides empty slots.
- **Max Row Size**: the maximum number of items shown per preview row.
- **Short Item Counts**: abbreviates large item counts (e.g. `1,000,000` -> `1M`).
- **Use Box Colors**: colors the preview window using the container's dye color (or grey for undyed containers).
- **Hide Shulker Box Lore**: hides shulker box lore text from the tooltip.

Enable it from the click-GUI (Category: QoL) or with `.z module ShulkerBoxTooltip on`.

> **Note:** The original mod's preview keybind/locking features are intentionally left out — in Zephyr the preview is simply always shown while the module is enabled. Shulker box colors are rendered using a dedicated Zephyr texture.

## Bots: auto-walk and mining

The **Pathing** module (Movement tab) walks to coordinates with A* pathfinding:

- `.z path <x> <y> <z> [destructive]`: walk there; `destructive` mines through walls and bridges gaps.
- `.z path stop`: cancel.
- `.z path task mine <block>`: walk to the nearest matching block and mine it.

The destination, route waypoints, and mine/place targets render as HUD markers while pathing.

## Cornerstone: copy areas as commands

Save a region once, replay it anywhere as `/setblock` + `/fill` commands (singleplayer / creative with permission):

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

## Fake players (singleplayer only)

Spawn client-side bots for testing farms, combat, or redstone. Works in singleplayer worlds only and never joins real servers:

```
.z player <name> spawn
.z player <name> kill | stop | use | attack | jump | sneak | sprint | ...
```

## Features

- **Click GUI**: searchable module list with per-category tabs and per-module settings panels
- **Commands**: chat-based control with `.z`, including tab-completion and argument suggestions
- **Global settings**: theme/custom accent colors, toast popups, HUD overlay, and other client-wide options
- **Profiles**: save and switch between different module/setting configurations
- **Keybinds**: bind any module or system action to up to three simultaneous keys, all editable in-game
- **Discord Rich Presence**: show "Playing Minecraft" plus your current location as your Discord status instead of Minecraft
- **Config system**: settings, toggles, keybinds, and profiles persist across restarts

### Movement

| Module                              | Description                                                                                      |
|-------------------------------------|--------------------------------------------------------------------------------------------------|
| Aerodynamics                        | Boosts velocity while sprinting                                                                  |
| Air Jump                            | Allows jumping in the air                                                                        |
| Anti Hunger                         | Avoids unnecessary sprint packets                                                                |
| Auto Walk                           | Walks forward automatically without holding W                                                    |
| Blink                               | Suppresses your position packets while enabled                                                   |
| Elytra Boost                        | Accelerates while gliding                                                                        |
| Flight                              | Enables client flight                                                                            |
| High Jump                           | Increases jump height                                                                            |
| Ice Speed                           | Stops you from sliding uncontrollably on ice                                                     |
| Jesus                               | Lets you walk on water as if it were solid ground                                                |
| No Fall                             | Prevents fall damage packets                                                                     |
| No Slowdown                         | Cancels the movement speed reduction from using items, walking in webs, or pushing through water |
| Pathing                             | Walks to coordinates using A* pathfinding (`.z path`)                                           |
| Scaffold                            | Places a block under your feet while you walk                                                    |
| Sprint                              | Automatically sprints while moving                                                               |
| Step                                | Raises the step height                                                                           |
| Trident Boost                       | Enables dry Riptide boosts                                                                       |

### Combat

| Module            | Description                                                                                 |
|-------------------|---------------------------------------------------------------------------------------------|
| AnchorAura        | Charges a respawn anchor in a target's face and detonates it, shielding yourself behind a glowstone block |
| Anchor Helper     | Place → charge → shield → detonate flow for respawn anchors                                 |
| Anime Protagonist | Attempts to teleport behind a hit entity                                                    |
| Auto Crystal      | Places and detonates end crystals on a target's obsidian support                             |
| Auto Place        | Automatically places a specified block on a hit entity                                      |
| Breach Swap       | Enables Breach Swapping under a certain fall distance                                       |
| Criticals         | Creates falling packets to enable crits and mace slams                                      |
| Crystal Helper    | Keeps placing + attacking crystals while holding right-click on obsidian                    |
| Density Swap      | Enables Density Swapping over a certain fall distance                                       |
| Hit Assist        | Sends the attack packet anyway when you miss, if you were looking close enough to an entity |
| InstaCart         | Automatically places a rail and TNT minecart to catch your own flaming arrows               |
| KillAura          | Automatically attacks for you                                                               |
| Knockback         | Reduces the amount of knockback you take                                    |
| Pearl Catch       | Catches a thrown Ender Pearl with a Wind Charge                                             |
| Reach             | Increases reach distance                                                    |
| Shieldbreaker     | Automatically breaks shields                                                                |
| Target Strafe     | Orbits around a nearby target while you move                                                |
| Totem Pop Notifier| Notifies you when nearby players pop their totems                                           |
| TriggerBot        | Attacks whenever an entity is in your crosshair and your cooldown is full                   |
| XBowCart          | Places a rail, TNT minecart, and fire to catch your own crossbow arrows                     |

### QoL

| Module              | Description                                                                                          |
|---------------------|------------------------------------------------------------------------------------------------------|
| AppleSkin           | Food-related HUD improvements: saturation, exhaustion, and hunger/health restored while holding food |
| ArmorRenderer       | Shows equipped armor and held items on the HUD with their durability                                 |
| Auto Tool           | Swaps to the correct tool to mine a block                                                            |
| Container ESP       | Outlines containers                                                                                  |
| Durability Swap     | Saves tools with low durability from being used to mine blocks                                       |
| Fast Attack         | Simulates attack actions multiple times per tick                                                     |
| Fast Use            | Simulates use actions multiple times per tick                                                        |
| FreeCam             | Detaches the camera to fly freely while your player stays in place; configure flight mode, speed, perspective, hand, and interaction behavior |
| FullBright          | Increases gamma                                                                                      |
| Gui Move            | Allows movement inputs while GUIs are open                                                           |
| Hold Attack         | Continually simulates pressing the attack key                                                        |
| Hold Use            | Continually simulates pressing the use key                                                           |
| Inventory Packets   | Skips packets when closing the inventory, letting you use crafting slots as storage                  |
| Inventory Renderer  | Shows your entire inventory on the HUD with item counts and durability                               |
| Item Restock        | Swaps a totem or item for a matching one from your inventory                                         |
| Jade                | In-world tooltip: block state, crop growth, redstone, beehive, horse stats, entity health, mod name  |
| MouseTweaks         | Right-drag distributing, left-drag crafting, scroll-wheel quick-move in containers                    |
| Periodic Attack     | Automatically attacks on a fixed interval                                                            |
| Periodic Use        | Automatically right-clicks on a fixed interval                                                       |
| Pick Before Place   | Forces a block pick action before placing a block                                                    |
| PlayerESP           | Glows nearby players                                                                                 |
| Potion Saver        | Attempts to extend the duration of potions                                                           |
| Render Invisibility | Renders invisible players as translucent                                                             |
| Safe Walk           | Prevents you from walking off block edges until you jump                                             |
| ShulkerBoxTooltip   | Shows the contents of shulker boxes and other containers in their tooltip                            |
| Sneak               | Automatically sneaks                                                                                 |
| Speed Mine          | Speeds up block breaking via synthetic Haste or predicted damage packets                             |
| Time Changer        | Changes the time of day client-side                                                                  |
| Tracers             | Draws lines from your crosshair to nearby players                                                    |
| Xray                | Outlines blocks in a list through walls, with ore presets or a custom list with per-block colors     |
| Zoom                | Smoothly zooms your FOV in to a custom level while enabled                                           |

### Disable

| Module                         | Description                                                   |
|--------------------------------|---------------------------------------------------------------|
| Disable Axe Stripping          | Prevents axe stripping                                        |
| Disable Block Cooldown         | Removes block breaking cooldown                               |
| Disable Block Outline          | Hides the black outline on the targeted block                 |
| Disable Block Particles        | Hides block breaking particles                                |
| Disable Bossbar                | Hides bossbars                                                |
| Disable Damage Tilt            | Removes the camera tilt when you take damage                  |
| Disable Dead Mob Interaction   | Blocks interactions with dead mobs                            |
| Disable Dead Mob Rendering     | Hides dead mobs                                               |
| Disable Explosion Particles    | Hides explosion, explosion emitter, poof, and smoke particles |
| Disable Fire Rendering         | Hides world fire and burning-entity flames                    |
| Disable First-Person Fire      | Lowers or removes the first-person fire overlay while on fire |
| Disable First-Person Particles | Hides your own status particles                               |
| Disable Fluid Fog              | Removes fog while underwater or in lava for better visibility |
| Disable Fog                    | Hides fog rendering                                           |
| Disable Nausea                 | Hides nausea overlays                                         |
| Disable Portal GUI Closing     | Keeps GUIs open in portals                                    |
| Disable Portal Sound           | Mutes nether portal ambience                                  |
| Disable Rain                   | Hides rain and rain sounds                                    |
| Disable Scoreboard             | Hides the sidebar scoreboard                                  |
| Disable Shovel Pathing         | Prevents shovel pathing                                       |
| Disable Totem Animation        | Prevents the totem of undying pop-up animation and effects    |

---

## Building from source

Requires JDK 21+ (the Gradle build targets Java 21) and an internet connection for Gradle.

```bash
./gradlew build
```

The finished mod jar will be in `build/libs/`.

---

## License

This project is licensed under the [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/) license. See [LICENSE](LICENSE) for details.

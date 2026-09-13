# Zephyr

A client-side hacked client / utility mod for **Minecraft 26.2**, built on [Fabric](https://fabricmc.net/). Zephyr packs 88 modules into four categories, Movement, Combat, QoL, and Disable, with a fully clickable GUI, a chat command system, configurable keybinds, configurable profiles, and Discord Rich Presence.

> **Use at your own risk.** Zephyr modifies client behavior and may violate the rules of the servers you play on. Use it only on servers where such modifications are allowed.

---

## Requirements

| Dependency                                           | Version                   |
|------------------------------------------------------|---------------------------|
| [Minecraft](https://www.minecraft.net/)              | 26.2                      |
| [Fabric Loader](https://fabricmc.net/use/installer/) | 0.19.3+                   |
| [Fabric API](https://fabricmc.net/use/)              | any recent build for 26.2 |
| Java                                                 | 25+                       |

---

## Installation

1. Install **Fabric Loader** for Minecraft 26.2 using the [Fabric installer](https://fabricmc.net/use/installer/).
2. Install **Fabric API** for Minecraft 26.2 (from [Modrinth](https://modrinth.com/mod/fabric-api) or [CurseForge](https://curseforge.com/minecraft/mc-mods/fabric-api)).
3. Download the Zephyr `.jar` and drop it into your `.minecraft/mods` folder.
4. Launch Minecraft with the Fabric profile.

---

## Getting started

- Press **`L` + `Enter`** to open the main menu (a clickable module list with search and category tabs).
- Press **`Tab`** while the menu is open to cycle between the other screens (global settings, keybinds, profiles, and back).
- **Left-click** a module to toggle it on or off; **right-click** it to open its settings panel.
- Press the **Command Prefix** key (default **`.`**) to open chat with the Zephyr prefix pre-typed and run a command like `.z module Flight on` (see [Commands](#commands)).
- Toggle state, settings, keybinds, and the active profile are all saved to `config/zephyr/` when you quit the game.

Everything, including the menu itself, can be rebound from the **Keybinds** screen. Keybinds support up to three keys held together (e.g. `G` or `Ctrl` + `B`).

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
| Stealth Mode              | Snapshots and force-disables every module, restoring them when turned off                                   |

## Commands

Zephyr's chat commands give you quick control over the client without opening the GUI. The command prefix is set by the **Command Prefix** keybind (default **`.`**); any chat message starting with it is intercepted client-side and never sent to the server.

| Command                              | Description                                                                    |
|--------------------------------------|--------------------------------------------------------------------------------|
| `.z`                                 | Shows diagnostics: mod version, active profile, and enabled/total module count |
| `.z module <name> <on\|off\|toggle>` | Controls a module by name, e.g. `.z module KillAura on`                        |
| `.z path <x> <y> <z> [destructive]`  | Walks to coordinates via A*; `destructive` mines through walls and bridges gaps |
| `.z path task mine <block>`          | Walks to the nearest instance of a block and mines it (e.g. `minecraft:deepslate_diamond_ore`) |
| `.z path stop`                       | Stops the current path walk                                                    |
| `.z <command> [args]`                | Delegates to any ported [ClientCommands](#clientcommands) command (see below)  |
| `.z player <name> <action>`          | Spawns/controls a singleplayer fake player (see [Fake Players](#fake-players)) |

- Tab-completion works in the chat box: type the prefix and start typing, and commands and arguments are suggested as you go.
- Names or arguments containing spaces can be double-quoted, e.g. `.z module "Anime Protagonist" toggle`.

## Pathing

**Pathing** is a Movement module that walks you to a set of coordinates using client-side A* pathfinding. Set a destination with:

```
.z path <x> <y> <z>
.z path <x> <y> <z> destructive
.z path task mine <block>
.z path stop
```

To just grab a specific resource, point it at any block and it will walk to the nearest one and mine it:

```
.z path task mine minecraft:deepslate_diamond_ore
```

The bot scans the loaded chunks around you for the nearest block of that type (the name can be full `minecraft:deepslate_diamond_ore` or just `deepslate_diamond_ore`), paths there destructively — digging through walls and bridging gaps as needed — and breaks the block once it is in reach, then announces the task is complete. It works with any block id, including ores, logs, and other resources. Unbreakable blocks (e.g. `minecraft:bedrock`) report that no path exists since they cannot be mined.

The route is drawn as dots on the HUD: passed waypoints green, upcoming ones cyan, and the waypoint you are heading toward yellow. The destination itself gets a red marker with its distance, and a mine task's target block is highlighted red.

By default the bot only walks over solid ground. If a wall or a gap blocks the way it reports that no path exists. Appending `destructive` enables two extra behaviors:

- **Mining**: the pathfinder may carve through mineable blocks (a full wall mines the feet block and the block above it). While walking, the bot breaks the nearest reachable block, aiming at it so breaking cracks show normally.
- **Placing**: gaps too large to fall across are bridged by placing support blocks under each step. The bot uses whatever block item is in the hotbar, including blocks it picked up from mining, and aims its placement so the floor lands exactly where the path needs it.

The A* search accounts for these edits with relative costs: walking a flat step costs `1.0`, climbing `1.3`, falling `1.0 + 0.1/block`, mining a block `+2.5`, and placing a bridge block `+2.0`, so it prefers the cheapest route (e.g. stepping up instead of mining through). When destructive mode is active the start message reports the chosen route's cost and how many blocks it plans to mine and place, and the HUD colors those blocks red (to mine) and blue (to place).

If a bridge is needed but no placeable block is in the hotbar, the bot holds still at the edge instead of walking off it, and warns you once.

## Seedcracker

**Seedcracker** is a QoL module that recovers the world seed of the server you're playing on by scanning newly generated chunks for structure and decorator fingerprints. Once enough data has been collected, a reduction process narrows the candidates down to the actual world seed, which is printed to chat as click-to-copy text.

Enable it from the click-GUI (Category: QoL) or with `.z module Seedcracker on`. Its settings panel controls which structures and decorators are scanned (Buried Treasure, Desert Temple, End City, ... , Biome), the render mode for found structure outlines (`OFF` / `ON` / `XRAY`), and debug/anti-xray options.

### Commands

All Seedcracker commands live under the Zephyr command prefix:

| Command                                               | Description                                                                       |
|-------------------------------------------------------|-----------------------------------------------------------------------------------|
| `.z seedcracker cracker [debug] [ON\|OFF]`            | Toggles the seed cracker (and debug mode) on or off                               |
| `.z seedcracker data clear`                           | Wipes all collected seed data                                                     |
| `.z seedcracker data bits`                            | Shows how many bits of information have been collected                            |
| `.z seedcracker data restore`                         | Restores structures from the previous session                                     |
| `.z seedcracker database`                             | Opens the shared seed database in your browser                                    |
| `.z seedcracker finder type <TYPE> [ON\|OFF]`         | Enables/disables a single finder type, e.g. `MONUMENT`                            |
| `.z seedcracker finder category <CATEGORY> [ON\|OFF]` | Enables/disables all finders in a category (`STRUCTURES`, `DECORATORS`, `BIOMES`) |
| `.z seedcracker finder reload`                        | Re-scans every chunk in your render distance                                      |
| `.z seedcracker render outlines [OFF\|ON\|XRAY]`      | Gets/sets the structure outline render mode                                       |
| `.z seedcracker version <version>`                    | Sets the Minecraft version used for seed reduction                                |

> **Note:** this module is a port of [SeedcrackerX](https://github.com/19MisterX98/seedcrackerX), reworked to fit Zephyr's module, command, and rendering systems. All credit for the seed-cracking logic goes to its original author, 19MisterX98, and the SeedcrackerX contributors. Use at your own risk: recovering world seeds may violate the rules of some servers.

## AppleSkin

**AppleSkin** is a QoL module that brings AppleSkin's food-related HUD improvements to Zephyr. While enabled it draws overlays on the vanilla HUD:

- **Saturation Overlay**: shows your current saturation as translucent icons over the food bar, plus the saturation the held food would add.
- **Exhaustion Underlay**: draws an exhaustion bar underneath the food bar.
- **Food Values Overlay**: while holding food, shows the hunger (and saturation) it would restore as outlined food icons over the bar.
- **Health Values Overlay**: while holding food, shows the health that food would eventually regenerate as extra hearts over the health bar.

Enable it from the click-GUI (Category: QoL) or with `.z module AppleSkin on`. Its settings panel controls each overlay independently, whether the offhand is checked when the main hand isn't food, whether the overlays mirror the vanilla bar bobbing animations, and the pulsing overlay flash strength.

> **Note:** I couldn't find [AppleSkin](https://github.com/squeek502/AppleSkin)'s 26.2 source, so this is a rewrite of the 1.21.x version, reworked to fit Zephyr's module and rendering systems and adapted to 26.2 MojMaps.

## ShulkerBoxTooltip

**ShulkerBoxTooltip** is a QoL module that shows the contents of shulker boxes and other containers right inside their tooltip, without needing to open them. It is a port of the Fabric mod [ShulkerBoxTooltip](https://github.com/Minenash/ShulkerBoxTooltip).

While enabled it renders a preview of the hovered container's inventory in the item tooltip. You can preview a **Shulker Box**, **Barrel**, **Chest** (including trapped, copper and ender variants), **Decorated Pot**, **Chiseled Book Shelf**, **Shelf**, and any item holding an `ItemContainerContents` component.

The module's settings control:

- **Preview Type**: `Full` shows the container's whole inventory, `Compact` collapses identical stacks and shows a merged, sorted view.
- **Preview Position**: `Inside` keeps the preview within the tooltip, `Outside` draws it to the right of the tooltip.
- **Tooltip Type**: `Vanilla` keeps the vanilla "Contains..." line, `Mod` replaces it with a compact item count line.
- **Window Color**: colors the preview background using the container's dye color (or grey for undyed containers), with an optional compact window.

Enable it from the click-GUI (Category: QoL) or with `.z module ShulkerBoxTooltip on`.

> **Note:** The original mod's preview keybind/locking features are intentionally left out, in Zephyr the preview is simply always shown while the module is enabled. Shulker box colors are rendered using a dedicated Zephyr texture.

## ClientCommands

Zephyr includes a partial port of [ClientCommands](https://github.com/Earthcomputer/clientcommands), exposed through `.z` (and its `c`-prefixed aliases). Every ported command is delegated by `ZCommand` and benefits from the same prefix/tab-completion as the built-in commands.

| Command | Description | Alias |
|---------|-------------|-------|
| `alias` | Manage command aliases stored in `config/zephyr/aliases.json`: `add <key> <command>`, `list`, `remove <key>`, `exec <key> [args]`; supports `%` placeholders and direct `.z <alias>` execution | `calias` |
| `config` | View/set client configs: `list`, `get <key>`, `set <key> <value>` | `cconfig` |
| `crackrng` | RNG cracking is incomplete in this port  (I PROMISE IM WOKRING ON IT)| `ccrackrng` |
| `creativetab` | Manage custom creative tabs: `add`/`remove`/`modify` ... | `ccreativetab` |
| `enchant` | Yeah... I need to finish rng cracking before I port this... | `cenchant` |
| `find` | Find nearby entities by type/name | `cfind` |
| `findblock` | Find nearest block: `findblock <block>` | `cfindblock` |
| `gamemode` | Query gamemodes: `query <player>`, `list <gamemode>` | `cgamemode` |
| `getdata` | Get NBT data: `entity <uuid>` or `block <x> <y> <z>` with optional path | `cgetdata` |
| `ghostblock` | Ghost blocks client-side: `set <x> <y> <z> <block>` or `fill <x1> <y1> <z1> <x2> <y2> <z2> <block> [replace <filter>]` | `cghostblock` |
| `give` | Give items in creative: `give <item> [count]` | `cgive` |
| `glow` | Glow entities/blocks: `entities <type>`, `block <x> <y> <z>`, `area ...` with optional duration | `cglow` |
| `hotbar` | Hotbar save/restore: `save`/`restore <1-9>` | `chotbar` |
| `kit` | Kit management: `create`/`delete`/`edit`/`load`/`list`/`preview <name>` | `ckit` |
| `look` | Look at block/angles: `block <x> <y> <z>`, `angles <yaw> <pitch>`, `cardinal <north|...>` | `clook` |
| `permissionlevel` | Show your permission level | `cpermissionlevel` |
| `ping` | Show ping: `ping [player]` | `cping` |
| `pos` | Convert dimension coords: `pos [to/from any vanilla dimension] [<x> <y> <z>]` | `cpos` |
| `relog` | Relog to the current server | `crelog` |
| `time` | Client time: `query`/`set <time>`/`reset` | `ctime` |
| `tp` | Spectator teleport: `tp <player|uuid>` | `ctp` |
| `uuid` | Get UUID: `uuid <player|uuid>` | `cuuid` |

Usage is `.z <command> [args]` or `.z c<command> [args]` (e.g. `.z give minecraft:diamond 64`, `.z cpos to nether`). Entity selectors and the full RNG tooling from upstream ClientCommands remain incomplete.

## Fake Players

**Fake Players** (`src/client/java/com/zephyr/client/fakeplayer/`) is a singleplayer-only port of `carpet.commands.PlayerCommand`, wired as `.z player`. It is gated by `FakePlayerManager.isSingleplayer()` so joining vanilla/multiplayer servers without Zephyr on the server is always safe.

All state lives in `src/client/java` and uses an `EmbeddedChannel` `FakeClientConnection` + reflection-set `channel` to keep enderpearl teleport/chunk tracking alive on the integrated server. `ZephyrClient` clears fake players on `DISCONNECT`.

```
.z player <name> spawn [at <x> <y> <z>] [facing <yaw> <pitch>] [in <survival|creative|adventure|spectator>] [in <dimension>]
.z player <name> kill
.z player <name> stop
.z player <name> use [once|continuous|interval [ticks]]
.z player <name> attack [once|continuous|interval [ticks]]
.z player <name> jump [once|continuous|interval [ticks]]
.z player <name> sneak / unsneak
.z player <name> sprint / unsprint
.z player <name> look <north|south|east|west|up|down|at <x> <y> <z>|<yaw> <pitch>>
.z player <name> turn <left|right|back|<yaw> <pitch>>
.z player <name> move <forward|backward|left|right|stop>
.z player <name> hotbar <1-9>
.z player <name> drop [all|mainhand|offhand]
.z player <name> dropStack [all|mainhand|offhand]
.z player <name> mount / dismount
.z player <name> shadow        # shadow the real player (disconnects you and spawns a bot copying you)
.z player list
```
## Mouse Tweaks

**Mouse Tweaks** (`src/client/java/com/zephyr/client/module/qol/MouseTweaks.java:19`) is a QoL inventory module *inspired by* [Mouse Tweaks](https://github.com/YaLTeR/MouseTweaks). I did not use the source or any code from anything related. All of the code within Zephyr is of my own mind, though the specific actions it aims to replicate are not. While enabled it replaces vanilla container dragging:

- **RMB Tweak**: vanilla-like RMB drag but revisiting slots places again (configurable).
- **LMB Tweak (with item)**: drag with an item on cursor to pick up matching items; with Shift, quick-moves them.
- **LMB Tweak (without item)**: Shift + LMB drag to quick-move every visited slot.
- **Wheel Tweak**: scroll over a stack to push one item per tick to the other inventory (scroll down) or pull from it (scroll up).

## Features

- **Click GUI**: searchable module list with per-category tabs and per-module settings panels
- **Commands**: chat-based control with `.z`, including tab-completion and argument suggestions.
- **Global settings**: theme/custom accent colors, toast popups, HUD overlay, and other client-wide options
- **Profiles**: save and switch between different module/setting configurations
- **Keybinds**: bind any module or system action to up to three simultaneous keys, all editable in-game
- **Discord Rich Presence**: show "Zephyr Client" as your Discord status instead of Minecraft
- **Config system**: settings, toggles, keybinds, and profiles persist across restarts

### Movement

| Module        | Description                                                           |
|---------------|-----------------------------------------------------------------------|
| Aerodynamics  | Boosts velocity while sprinting                                       |
| Air Jump      | Allows jumping in the air                                             |
| Anti Hunger   | Avoids unnecessary sprint packets                                     |
| Auto Walk     | Walks forward automatically without holding W                         |
| Blink         | Suppresses your position packets while enabled                        |
| Elytra Boost  | Accelerates while gliding                                             |
| Flight        | Enables client flight                                                 |
| High Jump     | Increases jump height                                                 |
| Ice Speed     | Stops you from sliding uncontrollably on ice                          |
| Jesus         | Lets you walk on water as if it were solid ground                     |
| No Fall       | Prevents fall damage packets                                          |
| No Slowdown   | Cancels the movement speed reduction from using items, webs, or water |
| Pathing       | Walks to coordinates via A*; optional destructive mode mines and bridges |
| Scaffold      | Places a block under your feet while you walk                         |
| Sprint        | Automatically sprints while moving                                    |
| Step          | Raises the step height                                                |
| Trident Boost | Enables dry riptide boosts                                            |

### Combat

| Module            | Description                                                                                               |
|-------------------|-----------------------------------------------------------------------------------------------------------|
| AnchorAura        | Charges a respawn anchor in a target's face and detonates it, shielding yourself behind a glowstone block |
| Anime Protagonist | Attempts to teleport behind a hit entity                                                                  |
| Auto Crystal      | Places and detonates end crystals on a target's obsidian support                                          |
| Auto Place        | Automatically places a specified block on a hit entity                                                    |
| Breach Swap       | Enables Breach Swapping under a certain fall distance                                                     |
| Criticals         | Creates falling packets to enable crits and mace slams                                                    |
| Density Swap      | Enables Density Swapping over a certain fall distance                                                     |
| Hit Assist        | Sends the attack packet anyway when you miss, if you were looking close enough to an entity               |
| InstaCart         | Automatically places a rail and TNT minecart to catch your flaming arrows |
| KillAura          | Automatically attacks for you                                                                             |
| Knockback         | Reduces the amount of knockback you take                                                                  |
| Lunge Swap        | Automatically attempts a Lunge Swap when attacking without a target                |
| Pearl Catch       | Catches a thrown Ender Pearl with a Wind Charge                             |
| Reach             | Increases reach distance                                                                                  |
| Shieldbreaker     | Automatically breaks shields                                                                              |
| Spear Damage      | Spoofs speed for massive spear stabs without moving                                                       |
| Target Strafe     | Orbits around a nearby target while you move                                                              |
| Totem Pop Notifier| Notifies you when nearby players pop their totems                                                         |
| TriggerBot        | Attacks whenever an entity is in your crosshair and your cooldown is full                                 |
| XBowCart          | Places a rail, TNT minecart, and fire to catch your own crossbow arrows          |

### QoL

| Module              | Description                                                                                                                                   |
|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| AppleSkin           | Food-related HUD improvements: saturation, exhaustion, and hunger/health restored while holding food                                          |
| ArmorRenderer       | Shows equipped armor and held items on the HUD with their durability                                                                          |
| Auto Tool           | Swaps to the correct tool to mine a block                                                                                                     |
| Container ESP       | Outlines containers                                                                                |
| Durability Swap     | Saves tools with low durability from being used to mine blocks                                                                                |
| Fast Attack         | Simulates attack actions multiple times per tick                                                                                              |
| Fast Use            | Simulates use actions multiple times per tick                                                                                                 |
| FreeCam             | Detaches the camera to fly freely while your player stays in place; configure flight mode, speed, perspective, hand, and interaction behavior |
| FullBright          | Increases gamma                                                                                                                               |
| Gui Move            | Allows movement inputs while GUIs are open                                                                                                    |
| Hold Attack         | Continually simulates pressing the attack key                                                                                                 |
| Hold Use            | Continually simulates pressing the use key                                                                                                    |
| Inventory Packets   | Skips packets when closing the inventory, letting you use crafting slots as storage                                                           |
| Inventory Renderer  | Shows your entire inventory on the HUD with item counts and durability                                                                        |
| Item Restock        | Swaps a totem or item for a matching one from your inventory                                                                                  |
| Jade                | Shows a tooltip with details about the block or entity you're looking at                                                                      |
| Mouse Tweaks        | Inventory drag/scroll tweaks                              |
| Periodic Attack     | Automatically attacks on a fixed interval                                                                                                     |
| Periodic Use        | Automatically right-clicks on a fixed interval                                                                                                |
| Pick Before Place   | Forces a block pick action before placing a block                                                                                             |
| PlayerESP           | Glows nearby players                                                                                                                          |
| Potion Saver        | Attempts to extend the duration of potions                                                                                                    |
| Render Invisibility | Renders invisible players as translucent                                                                                                      |
| Safe Walk           | Prevents you from walking off block edges until you jump                                                                                      |
| Seedcracker         | Recovers the world seed by scanning generated structures (see [Seedcracker](#seedcracker))                                                    |
| ShulkerBoxTooltip   | Shows the contents of shulker boxes and other containers in their tooltip                                                                     |
| Sneak               | Automatically sneaks                                                                                                                          |
| Speed Mine          | Speeds up block breaking via synthetic Haste or predicted damage packets                                                                      |
| Time Changer        | Changes the time of day client-side                                                                                                           |
| Tracers             | Draws lines from your crosshair to nearby players                                                         |
| Xray                | Outlines blocks in a list through walls, with presets or a custom list with per-block colors            |
| Zoom                | Smoothly zooms your FOV in to a custom level while enabled                                                                                    |

### Disable

| Module                         | Description                                                   |
|--------------------------------|---------------------------------------------------------------|
| Disable Axe Stripping          | Prevents axe stripping                                        |
| Disable Block Cooldown         | Removes block breaking cooldown                               |
| Disable Block Outline          | Hides the black outline on the targeted block                 |
| Disable Block Particles        | Hides block breaking particles                                |
| Disable Bossbar                | Hides bossbars                                                |
| Disable Dead Mob Interaction   | Blocks interactions with dead mobs                            |
| Disable Dead Mob Rendering     | Hides dead mobs                                               |
| Disable Damage Tilt            | Removes the camera tilt when you take damage                   |
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

Requires JDK 25+ and an internet connection for Gradle.

```bash
./gradlew build
```

The finished mod jar will be in `build/libs/`.

---

## Developer

Zephyr is a Fabric mod with two source sets: `main` (the common entry point and mixin configs) and `client` (everything else). The client source set lives under `src/client/java/com/zephyr/client` and is where all of the actual client logic resides.

### Project layout

```
src/main/java/com/zephyr/          Mod entry point (Zephyr) and the common mixin config
src/main/resources/                fabric.mod.json, mixin configs, icon
src/client/java/com/zephyr/client/
├── ZephyrClient.java              Client initializer: wires up every system
├── TickScheduler.java / MillisScheduler.java   Tick and wall-clock schedulers (used by InstaCart/XBowCart legit timing — 57fc1e0)
├── configplusgui/                 The core framework ("config plus GUI")
│   ├── config/                    GlobalConfig, ConfigManager, ProfileManager, StealthManager
│   ├── module/                    Module (base class), Category, ModuleManager
│   ├── setting/                   Setting<T> + Boolean/Number/Enum/String/List settings
│   ├── keybind/                   Keybind (up to 3 GLFW keys), KeybindManager, GuiKeybindHandler
│   ├── screen/                    The Zephyr menu screens (ClickGui, Keybinds, Profiles, Config, ...)
│   ├── hud/                       HudRenderer, NotificationManager, PartyManager, ThemeColor, ...
│   └── secretsettings/            The hidden "Better Movement" feature (dash, glide, double jump, ...)
├── commands/                      Chat command system (.z ...) — ZCommand delegates to SeedcrackerCommand, PathCommand, + ClientCommands port (Alias, Give, GhostBlock, ...)
├── fakeplayer/                    Singleplayer fake players (FakePlayerEntity/Manager/ActionPack/Connection — .z player, see Fake Players)
├── discord/                       Discord Rich Presence
├── mixin/                         One package per feature area; each class targets one vanilla class
│   ├── bettermovement/  combat/  disable/  movement/  qol/  commands/
└── module/                        The modules themselves, grouped by Category
    ├── bots/pathing/              AStarPathfinder, Pathing, BlockLocator, TargetRender (destructive + task mine)
    ├── combat/  disable/  movement/
    └── qol/                       Includes large self-contained features: seedcracker/, jade/,
                                   shulkerboxtooltip/, appleskin/, freecam/, mousetweaks/
```

### How a module works

A module is a class that extends `Module` and is registered once through `ModuleManager.init()`. The framework does the rest:

- `ModuleManager` exposes it to the click-GUI, the `.z module <name> ...` command, and keybind toggles.
- While a module is enabled, `Module.tick(Minecraft)` is called every client tick.
- `onEnable()` / `onDisable()` fire on state transitions; use them to hook or unhook mixins, or to start/stop renderers.
- Settings created with `addSetting(...)` are automatically rendered as controls, saved to `config/zephyr/modules.json`, and included in profiles.

To add a new module:

1. Create a class extending `Module` with a private constructor calling `super(name, description, category)`, and a `public static final` singleton instance (modules are singletons: see any existing module).
2. Add settings with `addSetting(...)` if needed.
3. Override `tick` / `onEnable` / `onDisable` as appropriate.
4. Register it in `ModuleManager.init()`.
5. If it needs to change vanilla behavior, add a mixin in the matching `mixin/` package and register it in `src/client/resources/zephyr.client.mixins.json`.

### Mixins

Mixins are organized in `src/client/java/com/zephyr/client/mixin` mirroring the module they serve (e.g. `mixin/qol/FreeCam/` backs `module/qol/FreeCam`). Every mixin class documents the vanilla class it targets, the method it injects into, and which module it backs. Mixin accessors/invokers live alongside their mixins. New mixins must be added to the `"client"` array in `src/client/resources/zephyr.client.mixins.json`.

### The module framework (`configplusgui`)

| Component        | Purpose                                                                                          |
|------------------|--------------------------------------------------------------------------------------------------|
| `Module`         | Base class for every toggleable feature (name, description, category, settings, lifecycle)       |
| `ModuleManager`  | Registry of all modules; tick dispatch, lookup by name, save orchestration                       |
| `Setting<T>`     | Base of a typed setting value; subtypes are rendered and persisted automatically                 |
| `KeybindManager` | Maps up to three simultaneous GLFW keys to a module or a `SystemAction` (open menu, prefix, ...) |
| `GlobalConfig`   | Client-wide settings: theme color, animation speed, HUD mode, autosave interval, stealth mode    |
| `ConfigManager`  | (De)serializes every module's state/settings to `config/zephyr/modules.json`                     |
| `ProfileManager` | Named snapshots of the full module state, switchable at runtime                                  |
| `ZephyrScreen`   | Base for all menu screens: shared chrome, panel sizing, and the slide animation                  |

### Commands

`commands/Command` is the abstract base; `CommandManager` handles registration, dispatch, and tab-completion (via Brigadier `Suggestions`). The `.z` command is `ZCommand`, and Seedcracker's sub-commands are handled by `SeedcrackerCommand`. `CommandPrefixHandler` intercepts the configurable prefix keybind and opens chat pre-filled; `mixin/commands/ChatInterceptMixin` makes sure prefixed messages never reach the server. Ported [ClientCommands](https://github.com/Earthcomputer/clientcommands) live as `AliasCommand`, `GiveCommand`, `GhostBlockCommand`, etc. and are resolved via `ZCommand.resolveDelegate()` with the same `c`-prefix aliasing (`.z cplayer` → `FakePlayerCommand`). `FakePlayerCommand` gates on `FakePlayerManager.isSingleplayer()` and drives `fakeplayer/FakePlayerEntity` on the integrated server only.

### Config, profiles, and persistence

All client data is persisted under `.minecraft/config/zephyr/`:

| File            | Contents                                                        |
|-----------------|-----------------------------------------------------------------|
| `modules.json`  | Every module's enabled state and setting values                 |
| `keybinds.json` | System-action and per-module keybinds                           |
| `profiles/`     | One JSON snapshot per named profile                             |

Saves happen on quit and on a configurable autosave interval (`GlobalConfig.autosaveIntervalSeconds`). Profiles are captured through the same (de)serialization used by `ConfigManager`.

### Generated documentation

Run `./gradlew javadoc` to build the HTML API docs into `build/docs/javadoc/`. Every public class and method across the codebase is documented.

---

## License

This project is licensed under the [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/) license. See [LICENSE](LICENSE) for details.

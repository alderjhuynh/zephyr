# Zephyr

A client-side hacked client / utility mod for **Minecraft 26.2**, built on [Fabric](https://fabricmc.net/). Zephyr packs over 60 modules into four categories, Movement, Combat, QoL, and Disable, with a fully clickable GUI, a chat command system, configurable keybinds, configurable profiles, and Discord Rich Presence.

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

- Tab-completion works in the chat box: type the prefix and start typing, and commands and arguments are suggested as you go.
- Names or arguments containing spaces can be double-quoted, e.g. `.z module "Anime Protagonist" toggle`.

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

## Features

- **Click GUI**: searchable module list with per-category tabs and per-module settings panels
- **Commands**: chat-based control with `.z`, including tab-completion and argument suggestions
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
| Elytra Boost  | Accelerates while gliding                                             |
| Flight        | Enables client flight                                                 |
| High Jump     | Increases jump height                                                 |
| No Fall       | Prevents fall damage packets                                          |
| No Slowdown   | Cancels the movement speed reduction from using items, webs, or water |
| Sprint        | Automatically sprints while moving                                    |
| Step          | Raises the step height                                                |
| Trident Boost | Enables dry riptide boosts                                            |

### Combat

| Module            | Description                                                                                 |
|-------------------|---------------------------------------------------------------------------------------------|
| AnchorAura        | Charges a respawn anchor in a target's face and detonates it, shielding yourself behind a glowstone block |
| Anime Protagonist | Attempts to teleport behind a hit entity                                                    |
| Auto Place        | Automatically places a specified block on a hit entity                                      |
| Breach Swap       | Enables Breach Swapping under a certain fall distance                                       |
| Criticals         | Creates falling packets to enable crits and mace slams                                      |
| Density Swap      | Enables Density Swapping over a certain fall distance                                       |
| Hit Assist        | Sends the attack packet anyway when you miss, if you were looking close enough to an entity |
| InstaCart         | Automatically places a rail and TNT minecart to catch your own flaming arrows               |
| KillAura          | Automatically attacks for you                                                               |
| Knockback         | Reduces the amount of knockback you take                                                    |
| Lunge Swap        | Automatically attempts a Lunge Swap when attacking without a target                         |
| Reach             | Increases reach distance                                                                    |
| Shieldbreaker     | Automatically breaks shields                                                                |
| TriggerBot        | Attacks whenever an entity is in your crosshair and your cooldown is full                   |
| XBowCart          | Places a rail, TNT minecart, and fire to catch your own crossbow arrows                     |

### QoL

| Module              | Description                                                                                          |
|---------------------|------------------------------------------------------------------------------------------------------|
| AppleSkin           | Food-related HUD improvements: saturation, exhaustion, and hunger/health restored while holding food |
| ArmorRenderer       | Shows equipped armor and held items on the HUD with their durability                                 |
| Auto Tool           | Swaps to the correct tool to mine a block                                                            |
| Container ESP       | Outlines containers                                                                                  |
| Discord Presence    | Shows Zephyr Client as your Discord presence                                                         |
| Durability Swap     | Saves tools with low durability from being used to mine blocks                                       |
| Fast Attack         | Simulates attack actions multiple times per tick                                                     |
| Fast Use            | Simulates use actions multiple times per tick                                                        |
| FreeCam             | Detaches the camera to fly freely while your player stays in place; configure flight mode, speed, perspective, hand, and interaction behavior |
| FullBright          | Increases gamma                                                                                      |
| Gui Move            | Allows movement inputs while GUIs are open                                                           |
| Hold Attack         | Continually simulates pressing the attack key                                                        |
| Hold Use            | Continually simulates pressing the use key                                                           |
| Inventory Packets   | Skips packets when closing the inventory, letting you use crafting slots as storage                  |
| Item Restock        | Swaps a totem or item for a matching one from your inventory                                         |
| Periodic Attack     | Automatically attacks on a fixed interval                                                            |
| Periodic Use        | Automatically right-clicks on a fixed interval                                                       |
| Pick Before Place   | Forces a block pick action before placing a block                                                    |
| PlayerESP           | Glows nearby players                                                                                 |
| Potion Saver        | Attempts to extend the duration of potions                                                           |
| Render Invisibility | Renders invisible players as translucent                                                             |
| Safe Walk           | Prevents you from walking off block edges until you jump                                             |
| Seedcracker         | Recovers the world seed by scanning generated structures (see [Seedcracker](#seedcracker))           |
| Sneak               | Automatically sneaks                                                                                 |
| Speed Mine          | Speeds up block breaking via synthetic Haste or predicted damage packets                             |
| Time Changer        | Changes the time of day client-side                                                                  |
| Tracers             | Draws lines from your crosshair to nearby players                                                    |
| Xray                | Outlines blocks in a list through walls, with presets or a custom list with per-block colors        |
| Zoom                | Smoothly zooms your FOV in to a custom level while enabled                                           |

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

## License

This project is licensed under the [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/) license. See [LICENSE](LICENSE) for details.

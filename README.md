# Zephyr

A client-side hacked client / utility mod for **Minecraft 26.2**, built on [Fabric](https://fabricmc.net/). Zephyr packs over 50 modules into four categories, Movement, Combat, QoL, and Disable, with a fully clickable GUI, configurable keybinds, configurable profiles, and Discord Rich Presence.

> **Use at your own risk.** Zephyr modifies client behavior and may violate the rules of the servers you play on. Use it only on servers where such modifications are allowed.

---

## Requirements

| Dependency | Version |
| --- | --- |
| [Minecraft](https://www.minecraft.net/) | 26.2 |
| [Fabric Loader](https://fabricmc.net/use/installer/) | 0.19.3+ |
| [Fabric API](https://fabricmc.net/use/) | any recent build for 26.2 |
| Java | 25+ |

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
- Toggle state, settings, keybinds, and the active profile are all saved to `config/zephyr/` when you quit the game.

Everything, including the menu itself, can be rebound from the **Keybinds** screen. Keybinds support up to three keys held together (e.g. `G` or `Ctrl` + `B`).

## Global settings

The **Settings** screen holds client-wide options:

| Setting | Description |
| --- | --- |
| Hotkey Popups | Shows a toast whenever a module is toggled from a keybind |
| Theme Color | Picks the accent color from six presets (Lavender, Sky, Mint, Gold, Coral, Rose) |
| Use Custom Color | Replaces the preset with Hue/Saturation/Value sliders plus a live swatch |
| Menu Animation Speed | Scales the panel slide and toast slide-in speed |
| Notification Corner | Which corner hotkey toasts slide into |
| Notification Lifetime | How long a toast holds before sliding out |
| HUD Overlay | Off, Minimal (watermark + profile), or Full (active modules plus an info stack with FPS, server, and theme) |
| Autosave Interval | Seconds between periodic config saves; `0` disables autosave |
| Keybind Conflict Warnings | Warns via toast when a new bind collides with an existing one |
| Stealth Mode | Snapshots and force-disables every module, restoring them when turned off |

## Features

- **Click GUI**: searchable module list with per-category tabs and per-module settings panels
- **Global settings**: theme/custom accent colors, toast popups, HUD overlay, and other client-wide options
- **Profiles**: save and switch between different module/setting configurations
- **Keybinds**: bind any module or system action to up to three simultaneous keys, all editable in-game
- **Discord Rich Presence**: show "Zephyr Client" as your Discord status instead of Minecraft
- **Config system**: settings, toggles, keybinds, and profiles persist across restarts

### Movement

| Module | Description |
| --- | --- |
| Aerodynamics | Boosts velocity while sprinting |
| Air Jump | Allows jumping in the air |
| Anti Hunger | Avoids unnecessary sprint packets |
| Elytra Boost | Accelerates while gliding |
| Flight | Enables client flight |
| High Jump | Increases jump height |
| No Fall | Prevents fall damage packets |
| Sprint | Automatically sprints while moving |
| Step | Raises the step height |
| Trident Boost | Enables dry riptide boosts |

### Combat

| Module | Description |
| --- | --- |
| Anime Protagonist | Attempts to teleport behind a hit entity |
| Auto Place | Automatically places a specified block on a hit entity |
| Breach Swap | Enables Breach Swapping under a certain fall distance |
| Criticals | Creates falling packets to enable crits and mace slams |
| Density Swap | Enables Density Swapping over a certain fall distance |
| KillAura | Automatically attacks for you |
| Lunge Swap | Automatically attempts a Lunge Swap when attacking without a target |
| Reach | Increases reach distance |
| Shieldbreaker | Automatically breaks shields |

### QoL

| Module | Description |
| --- | --- |
| Auto Tool | Swaps to the correct tool to mine a block |
| Container ESP | Outlines containers |
| Discord Presence | Shows Zephyr Client as your Discord presence |
| Durability Swap | Saves tools with low durability from being used to mine blocks |
| Fast Attack | Simulates attack actions multiple times per tick |
| Fast Use | Simulates use actions multiple times per tick |
| FullBright | Increases gamma |
| Gui Move | Allows movement inputs while GUIs are open |
| Hold Attack | Continually simulates pressing the attack key |
| Hold Use | Continually simulates pressing the use key |
| Inventory Packets | Skips packets when closing the inventory, letting you use crafting slots as storage |
| Item Restock | Swaps a totem or item for a matching one from your inventory |
| Periodic Attack | Automatically attacks on a fixed interval |
| Periodic Use | Automatically right-clicks on a fixed interval |
| Pick Before Place | Forces a block pick action before placing a block |
| PlayerESP | Glows nearby players |
| Potion Saver | Attempts to extend the duration of potions |
| Render Invisibility | Renders invisible players as translucent |
| Sneak | Automatically sneaks |
| Speed Mine | Speeds up block breaking via synthetic Haste or predicted damage packets |
| Xray | Outlines blocks in a list through walls |

### Disable

| Module | Description |
| --- | --- |
| Disable Axe Stripping | Prevents axe stripping |
| Disable Block Cooldown | Removes block breaking cooldown |
| Disable Block Particles | Hides block breaking particles |
| Disable Dead Mob Interaction | Blocks interactions with dead mobs |
| Disable Dead Mob Rendering | Hides dead mobs |
| Disable First-Person Particles | Hides your own status particles |
| Disable Fog | Hides fog rendering |
| Disable Nausea | Hides nausea overlays |
| Disable Portal GUI Closing | Keeps GUIs open in portals |
| Disable Portal Sound | Mutes nether portal ambience |
| Disable Rain | Hides rain and rain sounds |
| Disable Shovel Pathing | Prevents shovel pathing |

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

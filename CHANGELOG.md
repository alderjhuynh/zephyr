# Changelog

All notable changes to this project are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) and entries are grouped by commit (newest first) with short hash and date.

---

## 2026-09-03 - *Changes to be committed*
### Added

- `src/client/java/com/zephyr/client/TickScheduler.java:1` - Tick-based task scheduler. `schedule(int delay, Runnable)` queues work; `tick()` is driven from `ZephyrClient` each `END_CLIENT_TICK`. Drains tasks with `ticksRemaining <= 0` and runs them on the client thread. Enables multi-tick sequences without blocking.
- `src/client/java/com/zephyr/client/MillisScheduler.java:1` - Millisecond scheduler backed by `Executors.newSingleThreadScheduledExecutor` (`MillisScheduler` daemon thread). `schedule(long delayMillis, Runnable)` marshals execution back via `Minecraft#execute`.
- `src/client/java/com/zephyr/client/mixin/combat/XBowCart/MultiPlayerGameModeMixin.java:1` - Mixin on `net.minecraft.client.multiplayer.MultiPlayerGameMode#useItemOn` (inject at `RETURN`). Delegates to `XBowCart.INSTANCE.onRailPlace(...)` when `cir.getReturnValue().consumesAction()`. Drives legit-mode for XBowCart without cancelling the player's rail placement.
- `src/client/java/com/zephyr/client/mixin/qol/Tracer/LevelExtractorMixin.java:1` *(untracked - `??`)* - Mixin on `net.minecraft.client.renderer.extract.LevelExtractor#extract`. Injects before `extractGizmos()` to call `Tracer.INSTANCE.renderPerFrame(client, partialTick)` at render framerate instead of tick rate.

### Changed

#### `src/client/java/com/zephyr/client/ZephyrClient.java:70`
- Register `TickScheduler.tick()` at the top of `ClientTickEvents.END_CLIENT_TICK` before `guiKeybindHandler`, `CommandPrefixHandler`, and `ModuleManager` ticks.

#### `src/client/java/com/zephyr/client/module/combat/InstaCart.java:1`
- Imports `TickScheduler`, `BooleanSetting`, `NumberSetting`, `InteractionHand`.
- New constant `LEGIT_LOOKAHEAD_TICKS = 12` alongside `LOOKAHEAD_TICKS = 8`; physics constants `ARROW_GRAVITY`, `ARROW_DRAG` retained.
- New state: `cartPending: Set<Integer>`, new settings `legit:BooleanSetting(false)` and `placementDelay:NumberSetting(1.0, 0.0–20.0, 1.0)`.
- `tick()` now selects lookahead dynamically (`legit ? 12 : 8`), passes it to `predict(client, arrow, lookaheadTicks)`, and splits placement path: legit-mode calls `placeRail(..., swing, hold)` then `scheduleCart(...)`; non-legit keeps atomic `place(...)`.
- `predict` now `predict(Minecraft, Arrow, int lookaheadTicks)`; loop bound parameterized.
- Refactor `place(pos)` into `placeRail(pos, swingHand, holdSlot)` + `placeCart(pos, swingHand, holdSlot)` + delegating `place(pos)` - each handles single-slot swap, optional `swing`, optional `holdSlot` restore, and its own `useItemOn` with `BlockHitResult`.
- New `scheduleCart(id, pos, originalSlot)` using `TickScheduler.schedule(delay)` for cart and `TickScheduler.schedule(delay*2)` for slot restore; checks `isEnabled`, entity validity, `AbstractArrowInvoker#zephyr$isInGround`.
- `cleanup` now also prunes `cartPending`; new `onDisable()` clears `predictions`, `processed`, `cartPending`.

#### `src/client/java/com/zephyr/client/module/combat/XBowCart.java:1`
- Imports `TickScheduler`, `BooleanSetting`, `NumberSetting`, `Player`, `CrossbowItem`, `Random`.
- New constants: `MAX_PREDICTION_TICKS=300`, `ARROW_GRAVITY`, `ARROW_DRAG`, `CROSSBOW_POWER=3.15`.
- New state: `Random random`, `sequenceActive:boolean`, `suppressIntercept:boolean`, `sequenceSlot:int`.
- New settings: `legit:BooleanSetting(false)`, `placementDelay:NumberSetting(2.0, 0.0–10.0, 1.0)`, `jitter:NumberSetting(2.0, 0.0–6.0, 1.0)`.
- `tick()` short-circuits in legit mode (cleanup only); preserves legacy prediction+place path otherwise.
- New `onRailPlace(LocalPlayer, InteractionHand, BlockHitResult)` - entry point from mixin. Validates `isEnabled && legit`, same player, `MAIN_HAND`, not using item, not `sequenceActive`, rail in hand, charged crossbow in hotbar, derives `railPos = hit.getBlockPos().relative(hit.getDirection())`, computes velocity from `getLookAngle()*CROSSBOW_POWER`, finds `firePos`, validates `minecart`+`flintAndSteel` slots, sets `sequenceActive=true` and calls `startSequence`.
- Stub `onUseItemFireAttempt(Player, InteractionHand):false` kept for compatibility.
- New `startSequence(...)` - captures `sequenceSlot`, computes `cartDelay=1+random(0..jit)`, `fireDelay=step+random`, `shootDelay=step+random`; schedules 4 tasks via `TickScheduler`: cart → fire → `fireCrossbow` → restore slot & clear `sequenceActive`. Abort on placement failure.
- Helpers: `isSequenceRunning(Minecraft):boolean`, `abortSequence(Minecraft)`, `fireCrossbow(Minecraft, InteractionHand)` (switches to charged crossbow slot, checks `CrossbowItem.isCharged`, toggles `suppressIntercept`, `player.swing` + `gameMode.useItem`), `findChargedCrossbowSlot(Player):int`.
- New `predictFromAim(Minecraft):BlockPos` - eye-position + look-angle * power, ray-casts with `ClipContext(Block.COLLIDER, Fluid.NONE)` up to 300 ticks with drag/gravity.
- Refactor `place(client, Arrow, railPos)` to use `placeFire(...,false,false)` + `placeRail` + `placeCart`; `findFirePos` now `findFirePos(Minecraft, BlockPos railPos, Vec3 velocity)` (signature change).
- New `placeRail(Minecraft, BlockPos, boolean, boolean)`, `placeCart(...)`, `placeFire(Minecraft, BlockPos, boolean, boolean)` - each single-item, slot-swap, optional swing/hold.
- Helpers `findFlintAndSteelSlot`, `findRailSlot`, `findMinecartSlot` now take `Player` instead of `LocalPlayer`.
- New `onDisable()` clears `predictions`, `processed`, resets `sequenceActive`/`suppressIntercept`.

#### `src/client/java/com/zephyr/client/module/combat/AutoPlace.java:4`
- Chore: remove unused import `org.lwjgl.opengl.ARBUniformBufferObject`.

#### `src/client/java/com/zephyr/client/module/qol/ContainerESP.java:1` 
- Resolve merge markers in `MillisScheduler`/`TickScheduler` import blocks (package `com.zephyr.client`).
- Performance rewrite: replace `BlockPos.betweenClosedStream(min,max)` + `getBlockState` per-block with chunk-section iteration. Computes clamped world bounds from `searchRadius` and player `blockPosition`, iterates `LevelChunk` via `getChunkSource().getChunk(x,z,ChunkStatus.FULL,false)`, skips empty chunks/sections, uses `SectionPos.sectionToBlockCoord`, palette cull via `section.maybeHas(...)`, then inner `x/y/z` loops (0..15) via `section.getBlockState(x,y,z).getBlock()`.
- Lazy `shulkerSet:Set<Block>` built once by scanning `BuiltInRegistries.BLOCK` for `path.endsWith("shulker_box")` plus `Blocks.SHULKER_BOX`; guarded by `shulkerInit`.
- New constant `SHULKER_COLOR=0xFF941694`; deduped double-draw bug (previous code called `Gizmos.cuboid` twice for same pos).

#### `src/client/java/com/zephyr/client/module/qol/Tracer.java:1` 
- New setting `distance:NumberSetting("Tracer Start Distance", 2, 0, 10, 1)`.
- `tick()` becomes no-op (comment: rendering moved to `LevelExtractor` mixin for per-frame timing).
- New `renderPerFrame(Minecraft client, float partialTick)` - uses `getBetterTracerStartLmaoMyOtherOneWasAss(client, partialTick)` (interpolated `getEyePosition(partialTick) + getViewVector(partialTick).scale(distance)`) as start, iterated `entitiesForRendering` with `instanceof RemotePlayer`, range check, `Gizmos.line(start, end, color, WIDTH)` with optional `setAlwaysOnTop()` via `throughWalls`. No longer opens `collectPerTickGizmos` (collector already open as `LevelExtractor.collectPerFrameMainThreadGizmos`).
- New helper `getBetterTracerStartLmaoMyOtherOneWasAss` reads `Tracer.INSTANCE.distance`.

#### `src/client/java/com/zephyr/client/module/qol/Xray.java:1` 
- New caches: `presetCache:Map<BlockType, Map<Block,Integer>>` built lazily synchronized from `BLOCKS` by `path.contains(needle)`; `cachedCustomColors` + `cachedCustomSnapshot` + `cachedCustomHash` for `LIST` mode.
- Helpers `getPresetMap(BlockType)` and `getCustomColors()` (parses `customBlocks` entries via `Identifier.tryParse`, handles `minecraft:` prefix, filters `Blocks.AIR`, `ListSetting.parseColor`).
- `tick()` now resolves `targetMap` via `BlockType.LIST ? getCustomColors() : getPresetMap(mode)`, early-returns on empty. Replaces `BlockPos.betweenClosedStream` with chunk-section iteration mirroring `ContainerESP` (bounds clamp, chunk loop, `maybeHas(targetMap.containsKey)`, local `x/y/z` loops, `Gizmos.cuboid`).
- Removes former `renderPreset` and `renderCustom` methods.

#### `src/client/resources/zephyr.client.mixins.json:1`
- Staged: add `"combat.XBowCart.MultiPlayerGameModeMixin"` under `mixins` for combat.
- Unstaged: add `"qol.Tracer.LevelExtractorMixin"` under `qol` mixins.
### Fixed
- `MillisScheduler`/`TickScheduler` merge conflict markers (`<<<<<<< Updated upstream` / `package com.aura.combatassistance.client` / `>>>>>>> Stashed changes`) resolved to `com.zephyr.client`.
- `ContainerESP` double-draw of the same `Gizmos.cuboid` call removed.

---

## `06be6fe` - 2026-08-26 - mace module bug is literally unfixable :pensive: (or i can't because im not good enough)

- `src/client/java/com/zephyr/client/mixin/combat/BreachSwap/AttackMixin.java:1` - ~44 lines changed (swap logic tweak, attempted mace-damage fix)
- `src/client/java/com/zephyr/client/mixin/combat/DensitySwap/AttackMixin.java:1` - ~48 lines changed (mirrored tweak)
- `src/client/java/com/zephyr/client/module/combat/MaceSwapGuard.java:1` - New 24-line guard module (mace swap protection)
- **Stat:** 3 files, 69+/47-

## `31253e2` - 2026-08-20 - why does this always break :face_with_bags_under_eyes:

- Empty commit (no file changes) - CI/timing fix after `962b5b4`.

## `962b5b4` - 2026-08-20 - pathing tasks

- `README.md:1` - docs update (12 lines)
- `src/client/java/com/zephyr/client/commands/PathCommand.java:1` - expanded argument handling (+91/-?; adds sub-commands for path tasks)
- `src/client/java/com/zephyr/client/commands/ZCommand.java:1` - 4 lines (register new path task commands)
- `src/client/java/com/zephyr/client/module/bots/pathing/BlockLocator.java:1` - New 99-line utility for locating target blocks for pathing tasks
- `src/client/java/com/zephyr/client/module/bots/pathing/Pathing.java:1` - +81 lines (task queue handling, refines navigation)
- `src/client/java/com/zephyr/client/module/bots/pathing/TargetRender.java:1` - 8 lines (render tweak)
- **Stat:** 6 files, 278+/17-

## `6dda68b` - 2026-08-19 - destructive flag for pathing

- `README.md:1` - +24 lines (documents `--destructive` / pathing flags)
- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 3 lines (hook for destructive config)
- `src/client/java/com/zephyr/client/commands/PathCommand.java:1` - +59 lines (parse `--destructive` flag)
- `src/client/java/com/zephyr/client/mixin/bots/pathing/KeyboardInputMixin.java:1` - +10 lines
- `src/client/java/com/zephyr/client/module/bots/pathing/AStarPathfinder.java:1` - +232/-? (adds destructive neighbor expansion: can break blocks, cost model change)
- `src/client/java/com/zephyr/client/module/bots/pathing/Pathing.java:1` - +359/-? (flag plumbing, path validation, re-path on block break)
- `src/client/java/com/zephyr/client/module/bots/pathing/TargetRender.java:1` - New 202-line gizmo renderer for path target
- **Stat:** 7 files, 830+/59-

## `bd438b2` - 2026-08-18 - eensie bit of refactoring

- `src/client/java/com/zephyr/client/commands/PathCommand.java:1` - 2 lines (import/arg fix)
- `src/client/java/com/zephyr/client/configplusgui/module/ModuleManager.java:1` - 1 line
- Rename `src/client/java/com/zephyr/client/mixin/movement/Pathing/KeyboardInputMixin.java:1` → `src/client/java/com/zephyr/client/mixin/bots/pathing/KeyboardInputMixin.java:1` (4 lines, package fix)
- Rename `src/client/java/com/zephyr/client/module/movement/AStarPathfinder.java:1` → `src/client/java/com/zephyr/client/module/bots/pathing/AStarPathfinder.java:1`
- Rename `src/client/java/com/zephyr/client/module/movement/Pathing.java:1` → `src/client/java/com/zephyr/client/module/bots/pathing/Pathing.java:1`
- Delete `src/client/java/com/zephyr/client/module/bots/temp.txt` (8 lines)
- `src/client/resources/zephyr.client.mixins.json:1` - 2 lines (mixin path update)
- **Stat:** 7 files, 7+/14-

## `44da4cb` - 2026-08-18 - first look at pathing

- `src/client/java/com/zephyr/client/commands/PathCommand.java:1` - New 78 lines (`.path <x> <y> <z>` command)
- `src/client/java/com/zephyr/client/commands/ZCommand.java:1` - 15 lines (command registration)
- `src/client/java/com/zephyr/client/configplusgui/module/ModuleManager.java:1` - 1 line (register Pathing module)
- `src/client/java/com/zephyr/client/mixin/movement/Pathing/KeyboardInputMixin.java:1` - New 39 lines (input override for auto-walk)
- `src/client/java/com/zephyr/client/module/movement/AStarPathfinder.java:1` - New 180 lines (grid A* over world blocks)
- `src/client/java/com/zephyr/client/module/movement/Pathing.java:1` - New 160 lines (module skeleton, tick-driven movement)
- `src/client/resources/zephyr.client.mixins.json:1` - 1 line
- **Stat:** 7 files, 470+/4-

## `d44a6aa` - 2026-08-15 - couple of annoying keybinding bugs

- `src/client/java/com/zephyr/client/commands/CommandPrefixHandler.java:1` - 8 lines (prefix handling edge)
- `src/client/java/com/zephyr/client/configplusgui/keybind/KeybindManager.java:1` - 7 lines (conflict/debounce fix)
- `src/client/java/com/zephyr/client/configplusgui/screen/KeybindGuiScreen.java:1` - 10 lines (UI fix)
- **Stat:** 3 files, 20+/5-

## `0aaacbf` - 2026-08-11 - new modules plus ui improvements

- `README.md:1` - 14 lines
- `gradle.properties:1` - 2 lines (version bump)
- `src/client/java/com/zephyr/client/configplusgui/hud/NotificationManager.java:1` - +78/-? (richer toasts, stacking)
- `src/client/java/com/zephyr/client/configplusgui/module/ModuleManager.java:1` - 10 lines (register new modules)
- `src/client/java/com/zephyr/client/configplusgui/screen/KeybindGuiScreen.java:1` - 38 lines (layout improvement)
- `src/client/java/com/zephyr/client/mixin/combat/TargetStrafe/KeyboardInputMixin.java:1` - New 61 lines
- `src/client/java/com/zephyr/client/mixin/combat/TotemPopNotifier/ClientPacketListenerMixin.java:1` - New 38 lines
- `src/client/java/com/zephyr/client/mixin/combat/TotemPopNotifier/HudMixin.java:1` - New 32 lines
- `src/client/java/com/zephyr/client/mixin/disable/DisableDamageTilt/GameRendererMixin.java:1` - New 33 lines
- `src/client/java/com/zephyr/client/mixin/movement/AutoWalk/KeyboardInputMixin.java:1` - New 38 lines
- `src/client/java/com/zephyr/client/mixin/movement/Blink/LocalPlayerMixin.java:1` - New 43 lines
- `src/client/java/com/zephyr/client/mixin/movement/IceSpeed/BlockMixin.java:1` - New 43 lines
- `src/client/java/com/zephyr/client/mixin/movement/Jesus/LivingEntityMixin.java:1` - New 61 lines
- `src/client/java/com/zephyr/client/module/combat/AutoCrystal.java:1` - New 261 lines
- `src/client/java/com/zephyr/client/module/combat/SpearDamage.java:1` - New 70 lines
- `src/client/java/com/zephyr/client/module/combat/TargetStrafe.java:1` - New 84 lines
- `src/client/java/com/zephyr/client/module/combat/TotemPopNotifier.java:1` - New 95 lines
- `src/client/java/com/zephyr/client/module/disable/disableDamageTilt.java:1` - New 17 lines
- `src/client/java/com/zephyr/client/module/movement/AutoWalk.java:1` - New 17 lines
- `src/client/java/com/zephyr/client/module/movement/Blink.java:1` - New 17 lines
- `src/client/java/com/zephyr/client/module/movement/IceSpeed.java:1` - New 26 lines
- `src/client/java/com/zephyr/client/module/movement/Jesus.java:1` - New 46 lines
- `src/client/java/com/zephyr/client/module/movement/Scaffold.java:1` - New 90 lines
- `src/client/resources/zephyr.client.mixins.json:1` - 8 lines
- **Stat:** 24 files, 1209+/13-

## `dd20288` - 2026-08-10 - Javadocs plus a bug from 1.0.0 that I forgot to fix :sob:

- `README.md:1` - +183/-? (expanded docs + Javadoc notes)
- `gradle.properties:1` - 2 lines (version)
- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 13 lines (Javadoc/comments)
- `src/client/java/com/zephyr/client/commands/*:1` - ~79 lines Javadoc on `Command`, `CommandManager`, `CommandPrefixHandler`, `SeedcrackerCommand`, `ZCommand`
- `src/client/java/com/zephyr/client/configplusgui/**:1` - Javadoc across `ConfigManager`, `GlobalConfig`, `ProfileManager`, `StealthManager`, `Corner`, `HudMode`, `HudRenderer`, `NotificationManager`, `PartyManager`, `PlaceholderEngine`, `ThemeColor`, `keybind/*`, `module/*`, `screen/*`, `setting/*`
- `src/client/java/com/zephyr/client/mixin/**:1` - Javadoc on ~18 mixin files (bettermovement, combat, disable, etc.)
- Bug fix from 1.0.0
- **Stat:** many files, ~Javadoc-only + bugfix

## `43f3523` - 2026-08-09 - jade/hwyla recreation, inventoryrenderer, shulkerboxtooltips recreation

- `README.md:1` - 19 lines
- `gradle.properties:1` - 2 lines
- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 15 lines (register new modules)
- `src/client/java/com/zephyr/client/configplusgui/module/ModuleManager.java:1` - 4 lines
- `src/client/java/com/zephyr/client/mixin/qol/InventoryRenderer/HudMixin.java:1` - New 20 lines
- `src/client/java/com/zephyr/client/mixin/qol/ShulkerBoxTooltip/*:1` - 4 new mixins (`GuiGraphicsMixin:94`, `ItemContainerContentsMixin:35`, `ItemStackMixin:71`, `ScreenMixin:24`)
- `src/client/java/com/zephyr/client/module/qol/InventoryRenderer.java:1` - New 157 lines
- `src/client/java/com/zephyr/client/module/qol/ShulkerBoxTooltip.java:1` - New 62 lines
- `src/client/java/com/zephyr/client/module/qol/jade/**:1` - ~20 new files (~1.2k lines): `Jade`, `JadeColors`, `JadeRenderer`, `OverlayPosition`, `access/*`, `provider/*`, `provider/vanilla/*`, `ray/RayTracer`, `render/*`, `tooltip/Tooltip`
- `src/client/java/com/zephyr/client/module/qol/shulkerboxtooltip/**:1` - ~25 new files (~1.5k lines): `ColorKey`, `FixedPreviewProviderRegistry`, `Preview*`, `ShulkerBoxPreviewProvider`, `hook/GuiGraphicsExtensions`, `render/*`, `tooltip/*`, `util/*`
- `src/client/resources/assets/zephyr/textures/gui/sprites/shulker_box_tooltip.png:1` + `.mcmeta:11`
- `src/client/resources/zephyr.client.mixins.json:1` - 7 lines
- **Stat:** 68 files, 3699+/2-

## `ebc78b0` - 2026-08-08 - may or may not have broken it

- `src/client/java/com/zephyr/client/mixin/qol/MobESP/EntityGlowMixin.java:1` - Delete 27 lines
- `src/client/resources/zephyr.client.mixins.json:1` - 1 line (remove mixin)
- **Stat:** 2 files, 28 deletions

## `583c480` - 2026-08-08 - me when i have to update the readme

- `README.md:1` - 9 lines (6+/3-)
- **Stat:** 1 file

## `d5ed480` - 2026-08-08 - new modules, list setting, updated modules

- `gradle.properties:1` - 2 lines
- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 2 lines
- `src/client/java/com/zephyr/client/configplusgui/config/ConfigManager.java:1` - 23 lines (persist `ListSetting`)
- `src/client/java/com/zephyr/client/configplusgui/hud/PartyManager.java:1` - remove 23 lines (secret feature prune)
- `src/client/java/com/zephyr/client/configplusgui/module/ModuleManager.java:1` - 5 lines (register new modules)
- `src/client/java/com/zephyr/client/configplusgui/screen/ClickGuiScreen.java:1` - +191 lines (categories, search, list-setting UI)
- `src/client/java/com/zephyr/client/configplusgui/screen/ConfigGuiScreen.java:1` - 13 lines
- `src/client/java/com/zephyr/client/configplusgui/screen/SecretGuiScreen.java:1` - 7 lines
- New `src/client/java/com/zephyr/client/configplusgui/secretsettings/bettermovement/*:1` - 8 files: `Aerodynamics:67`, `BetterMovement:49`, `Dash:147`, `DoubleJump:182`, `FluidCheck:39`, `Glide:125`, `GlideSound:38`, `NoFall:60`, `WaveDash:118`
- `src/client/java/com/zephyr/client/configplusgui/setting/ListSetting.java:1` - New 66 lines
- `src/client/java/com/zephyr/client/mixin/bettermovement/*:1` - 6 new mixins (`DashHudMixin:74`, `DoubleJump*HudMixin`, `GlideCrosshairMixin`, `NoFallConnectionMixin`, `WaveDashCrosshairMixin`)
- `src/client/java/com/zephyr/client/mixin/combat/AbstractArrowInvoker.java:1` - New 12 lines
- `src/client/java/com/zephyr/client/mixin/qol/FreeCam/*:1` - 11 new/updated mixins (camera, entity, renderer rewrites, 300+ lines)
- `src/client/java/com/zephyr/client/mixin/qol/MobESP/EntityGlowMixin.java:1` - New 27 lines
- `src/client/java/com/zephyr/client/module/combat/AnchorAura.java:1` - New 280 lines
- `src/client/java/com/zephyr/client/module/combat/InstaCart.java:1` - New 234 lines
- `src/client/java/com/zephyr/client/module/combat/TriggerBot.java:1` - New 38 lines
- `src/client/java/com/zephyr/client/module/combat/XBowCart.java:1` - New 282 lines
- `src/client/java/com/zephyr/client/module/qol/FreeCam.java:1` - Rewrite +277/-? (freecam refactor)
- `src/client/java/com/zephyr/client/module/qol/Xray.java:1` - +81/-? (list-setting integration)
- `src/client/java/com/zephyr/client/module/qol/freecam/*:1` - 6 new files: `FlightMode`, `FreeCamera:261`, `FreecamPosition:82`, `InteractionMode`, `Motion:59`, `Perspective`
- Assets: 12 dash icons (`assets/zephyr/dash/icon/dash*.png`), 12 double-jump icons, 13 elytra icons, `sounds.json:32`, `sounds/dash.ogg`, `sounds/double_jump.ogg`
- **Stat:** very large

## `1af01f7` - 2026-08-06 - better screen cycling plus a secret <3

- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 2 lines (screen cycling hook)
- `src/client/java/com/zephyr/client/configplusgui/config/GlobalConfig.java:1` - 11 lines
- `src/client/java/com/zephyr/client/configplusgui/hud/NotificationManager.java:1` - 3 lines
- `src/client/java/com/zephyr/client/configplusgui/hud/PartyManager.java:1` - New 140 lines (party/secret feature)
- `src/client/java/com/zephyr/client/configplusgui/keybind/KeybindManager.java:1` - 11 lines
- `src/client/java/com/zephyr/client/configplusgui/screen/ClickGuiScreen.java:1` - 6 lines
- `src/client/java/com/zephyr/client/configplusgui/screen/CreditsGuiScreen.java:1` - New 40 lines
- `src/client/java/com/zephyr/client/configplusgui/screen/SecretGuiScreen.java:1` - New 204 lines
- `src/client/java/com/zephyr/client/configplusgui/screen/ZephyrScreen.java:1` - +135/-? (Z-suffix cycling, secret trigger)
- `src/client/java/com/zephyr/client/mixin/commands/ChatInterceptMixin.java:1` - 16 lines (secret command)
- **Stat:** 10 files, 546+/22-

## `dd9cf40` - 2026-08-06 - seedcracker and appleskin modules, fixed autoplace bug, discord presence is now a global setting

- `README.md:1` - 235 lines
- `build.gradle:1` - 28 lines (dependency / inclusion for seedcracker libs)
- `gradle.properties:1` - 13 lines
- `settings.gradle:1` - 12 lines
- `src/client/java/com/zephyr/client/commands/SeedcrackerCommand.java:1` - New 389 lines (full seed cracker CLI)
- `src/client/java/com/zephyr/client/commands/ZCommand.java:1` - 15 lines
- `src/client/java/com/zephyr/client/configplusgui/config/GlobalConfig.java:1` - 31 lines (discord presence global toggle)
- `src/client/java/com/zephyr/client/configplusgui/module/ModuleManager.java:1` - 84 lines (new modules)
- `src/client/java/com/zephyr/client/configplusgui/screen/ConfigGuiScreen.java:1` - 7 lines
- `src/client/java/com/zephyr/client/configplusgui/setting/*:1` - trim 11 lines (simplify `BooleanSetting`/`EnumSetting`/`NumberSetting`/`Setting`)
- `src/client/java/com/zephyr/client/discord/DiscordPresenceManager.java:1` - 8 lines
- `src/client/java/com/zephyr/client/mixin/combat/AutoPlace/AttackMixin.java:1` - 54 lines (bug fix)
- `src/client/java/com/zephyr/client/mixin/qol/AppleSkin/*:1` - 2 new mixins (`FoodDataAccessor:16`, `HudMixin:40`), plus `ArmorRenderer/HudMixin:20`
- `src/client/java/com/zephyr/client/mixin/qol/Seedcracker/*:1` - 3 new mixins (`ClientLevelMixin:38`, `ClientPacketListenerMixin:60`, `LocalPlayerMixin:17`)
- `src/client/java/com/zephyr/client/module/qol/AppleSkin.java:1` - New 197 lines + helpers `appleskin/FoodHelper:193`, `HudOverlayHandler:301`, `IntPoint:7`, `TextureHelper:59`
- `src/client/java/com/zephyr/client/module/qol/ArmorRenderer.java:1` - New 110 lines
- Delete `src/client/java/com/zephyr/client/module/qol/DiscordPresence.java:1` - 22 lines (moved to GlobalConfig)
- `src/client/java/com/zephyr/client/module/qol/Seedcracker.java:1` - New 193 lines + full `seedcracker/**` package (~70 files, ~4k lines: `cracker/*`, `finder/*`, `finder/decorator/*`, `finder/structure/*`, `render/Cuboid`, `util/*`)
- Asset `assets/zephyr/textures/appleskin/icons.png:480B`
- `src/client/resources/zephyr.client.mixins.json:1` - 8 lines
- **Stat:** 83 files, 7644+/210-

## `1279e7f` - 2026-08-06 - oops i forgot to change the version number

- `gradle.properties:1` - 1 line (version bump)
- **Stat:** 1 file, 1+/1-

## `becf54e` - 2026-08-06 - new modules + refactoring + new commands system

- `README.md:1` - 31 lines
- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 5 lines
- `src/client/java/com/zephyr/client/commands/Command.java:1` - New 27 lines
- `src/client/java/com/zephyr/client/commands/CommandManager.java:1` - New 153 lines
- `src/client/java/com/zephyr/client/commands/CommandPrefixHandler.java:1` - New 57 lines
- `src/client/java/com/zephyr/client/commands/ZCommand.java:1` - New 95 lines
- `src/client/java/com/zephyr/client/configplusgui/keybind/KeybindManager.java:1` - 3 lines
- `src/client/java/com/zephyr/client/configplusgui/module/ModuleManager.java:1` - 25 lines
- `src/client/java/com/zephyr/client/mixin/combat/HitAssist/MinecraftMixin.java:1` - New 77 lines
- Remove `src/client/java/com/zephyr/client/mixin/combat/KillAura/EntityGlowMixin.java:30` + `ForceAttackMixin:14`
- `src/client/java/com/zephyr/client/mixin/combat/Knockback/ClientPacketListenerMixin.java:1` - New 32 lines
- `src/client/java/com/zephyr/client/mixin/commands/ChatInterceptMixin.java:1` - New 19 lines
- `src/client/java/com/zephyr/client/mixin/commands/CommandSuggestionsAccessor.java:1` - New 47 lines
- `src/client/java/com/zephyr/client/mixin/commands/CommandSuggestionsMixin.java:1` - New 69 lines
- `src/client/java/com/zephyr/client/mixin/disable/*:1` - 6 new mixins: `BlockOutline/LevelRendererMixin:21`, `Bossbar/HudMixin:20`, `FirstPersonFire/ScreenEffectRendererMixin:46`, `FluidFog/*:52`, `Scoreboard/HudMixin:20`, `TotemAnimation/ClientPacketListenerMixin:21`
- `src/client/java/com/zephyr/client/mixin/movement/NoSlowdown/*:1` - 3 new mixins: `ItemUseMixin:18`, `WaterMixin:19`, `WebMixin:21`
- `src/client/java/com/zephyr/client/mixin/qol/FreeCam/*:1` - 7 new mixins: `CameraMixin:46`, `ClientInputAccessor:16`, `EntityTurnMixin:21`, `GameRendererMixin:18`, `KeyboardInputMixin:23`, `LocalPlayerMixin:72`
- `src/client/java/com/zephyr/client/mixin/qol/InventoryPackets/LocalPlayerMixin.java:1` - New 34 lines
- Remove `src/client/java/com/zephyr/client/mixin/qol/InventoryPackets/ServerPlayerMixin.java:21`
- `src/client/java/com/zephyr/client/mixin/qol/SafeWalk/PlayerMixin.java:1` - New 19 lines
- `src/client/java/com/zephyr/client/mixin/qol/TimeChanger/ClientClockManagerMixin.java:1` - New 23 lines
- `src/client/java/com/zephyr/client/mixin/qol/Zoom/CameraMixin.java:1` - New 18 lines
- `src/client/java/com/zephyr/client/module/bots/temp.txt:1` - 8 lines (placeholder)
- `src/client/java/com/zephyr/client/module/combat/HitAssist.java:1` - New 16 lines
- `src/client/java/com/zephyr/client/module/combat/KillAura/KillAura.java:1` - ~105 lines refactor
- `src/client/java/com/zephyr/client/module/combat/Knockback.java:1` - New 16 lines
- `src/client/java/com/zephyr/client/module/disable/*:1` - 4 new toggles: `disableBlockOutline`, `disableBossbar`, `disableFirstPersonFire`, `disableFluidFog`, `disableScoreboard`, `disableTotemAnimation`
- `src/client/java/com/zephyr/client/module/movement/NoSlowdown.java:1` - New 13 lines
- `src/client/java/com/zephyr/client/module/qol/FreeCam.java:1` - New 128 lines
- `src/client/java/com/zephyr/client/module/qol/SafeWalk.java:1` - New 11 lines
- `src/client/java/com/zephyr/client/module/qol/TimeChanger.java:1` - New 16 lines
- `src/client/java/com/zephyr/client/module/qol/Tracer.java:1` - New 55 lines
- `src/client/java/com/zephyr/client/module/qol/Zoom.java:1` - New 80 lines
- `src/client/resources/zephyr.client.mixins.json:1` - +30/-? lines
- **Stat:** 53 files, 1651+/130-

## `27cff5c` - 2026-08-05 - organization hell (sorry the configplusgui dir was killing me)

-  Package reorganization: `configplusgui/*` → `configplusgui/{config, hud, keybind, module, screen, setting}/*` and `module/{combat, movement, ...}` import fixes. No logic change, 81 files touched (239+/176-).

## `0418cf4` - 2026-08-05 - global settings update

- `README.md:1` - 20 lines
- `gradle.properties:1` - 2 lines
- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 17 lines (GlobalConfig init)
- `src/client/java/com/zephyr/client/configplusgui/ConfigGuiScreen.java:1` - +264/-? (global settings tab, HUD preview, placeholder engine UI)
- `src/client/java/com/zephyr/client/configplusgui/ConfigManager.java:1` - 4 lines
- `src/client/java/com/zephyr/client/configplusgui/Corner.java:1` - New 8 lines
- `src/client/java/com/zephyr/client/configplusgui/GlobalConfig.java:1` - +239/-? (corner, HUD mode, stealth placeholder, watermark, arraylist, etc.)
- `src/client/java/com/zephyr/client/configplusgui/HudMode.java:1` - New 7 lines
- `src/client/java/com/zephyr/client/configplusgui/HudRenderer.java:1` - New 151 lines
- `src/client/java/com/zephyr/client/configplusgui/Keybind.java:1` - 10 lines (global keybinds)
- `src/client/java/com/zephyr/client/configplusgui/KeybindGuiScreen.java:1` - 10 lines
- `src/client/java/com/zephyr/client/configplusgui/KeybindManager.java:1` - 80 lines
- `src/client/java/com/zephyr/client/configplusgui/ModuleManager.java:1` - 9 lines
- `src/client/java/com/zephyr/client/configplusgui/NotificationManager.java:1` - 49 lines
- `src/client/java/com/zephyr/client/configplusgui/PlaceholderEngine.java:1` - New 60 lines
- `src/client/java/com/zephyr/client/configplusgui/StealthManager.java:1` - New 47 lines
- `src/client/java/com/zephyr/client/configplusgui/StringSetting.java:1` - New 12 lines
- `src/client/java/com/zephyr/client/configplusgui/ZephyrScreen.java:1` - 27 lines
- **Stat:** 18 files, 955+/61-

## `54e8184` - 2026-08-05 - oopsie i forgot to push this

- `src/client/java/com/zephyr/client/configplusgui/ThemeColor.java:1` - New 33 lines (`ThemeColor` enum)
- **Stat:** 1 file, 33+

## `bd0e98b` - 2026-08-05 - theme color global setting

- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 8 lines
- `src/client/java/com/zephyr/client/configplusgui/ClickGuiScreen.java:1` - 16 lines (theme-aware rendering)
- `src/client/java/com/zephyr/client/configplusgui/ConfigGuiScreen.java:1` - 69 lines (color picker)
- `src/client/java/com/zephyr/client/configplusgui/GlobalConfig.java:1`- 26 lines (accent storage)
- `src/client/java/com/zephyr/client/configplusgui/KeybindGuiScreen.java:1` - 4 lines
- `src/client/java/com/zephyr/client/configplusgui/NotificationManager.java:1` - 4 lines
- `src/client/java/com/zephyr/client/configplusgui/ProfileGuiScreen.java:1` - 6 lines
- `src/client/java/com/zephyr/client/configplusgui/ZephyrScreen.java:1` - 23 lines
- **Stat:** 8 files, 117+/39-

## `e78d995` - 2026-08-05 - toast popups, new global config

- `README.md:1` - +133/-? (install/config docs)
- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 19 lines
- `src/client/java/com/zephyr/client/configplusgui/ConfigGuiScreen.java:1` - New 92 lines (first config GUI)
- `src/client/java/com/zephyr/client/configplusgui/GlobalConfig.java:1` - New 79 lines (initial global settings)
- `src/client/java/com/zephyr/client/configplusgui/GuiKeybindHandler.java:1` - 6 lines removed
- `src/client/java/com/zephyr/client/configplusgui/KeybindManager.java:1` - 1 line
- `src/client/java/com/zephyr/client/configplusgui/NotificationManager.java:1` - New 85 lines (toast system)
- `src/client/java/com/zephyr/client/configplusgui/ZephyrScreen.java:1` - 12 lines
- **Stat:** 8 files, 410+/17-

## `86e70cd` - 2026-08-04 - keybinds and profiles + dynamic menu sizing

- `src/client/java/com/zephyr/client/ZephyrClient.java:1` - 4 lines
- `src/client/java/com/zephyr/client/configplusgui/ClickGuiScreen.java:1` - 150 lines (dynamic sizing fix vs 362-line original)
- `src/client/java/com/zephyr/client/configplusgui/ConfigManager.java:1` - 80 lines (profile-aware persist)
- `src/client/java/com/zephyr/client/configplusgui/GlfwKeyNames.java:1` - New 54 lines
- `src/client/java/com/zephyr/client/configplusgui/GuiKeybindHandler.java:1` - 29 lines
- `src/client/java/com/zephyr/client/configplusgui/Keybind.java:1` - New 55 lines
- `src/client/java/com/zephyr/client/configplusgui/KeybindGuiScreen.java:1` - New 240 lines
- `src/client/java/com/zephyr/client/configplusgui/KeybindManager.java:1` - New 230 lines
- `src/client/java/com/zephyr/client/configplusgui/ModuleManager.java:1` - 19 lines
- `src/client/java/com/zephyr/client/configplusgui/ProfileGuiScreen.java:1` - New 272 lines
- `src/client/java/com/zephyr/client/configplusgui/ProfileManager.java:1` - New 173 lines
- `src/client/java/com/zephyr/client/configplusgui/ZephyrScreen.java:1` - New 165 lines (base screen with dynamic sizing)
- `src/client/java/com/zephyr/client/mixin/qol/GuiMove/KeyboardInputMixin.java:1` - 4 lines
- `src/client/java/com/zephyr/client/module/qol/ContainerESP.java:1` - New 80 lines
- **Stat:** 14 files, 1402+/153-

## `ebe2fe1` - 2026-08-04 - all the bug fixes are done

- `src/client/java/com/zephyr/client/configplusgui/ModuleManager.java:1` - 1 line
- `src/client/java/com/zephyr/client/mixin/disable/FogRendering/BackgroundRendererMixin.java:1` - 43 lines (simplify fog disable)
- Delete `src/client/java/com/zephyr/client/mixin/qol/InventoryRendererMixin.java:19`
- `src/client/java/com/zephyr/client/mixin/disable/NauseaOverlay/*:1` - `NauseaOverlayMixin:+18/-?`, `NauseaVignetteMixin:24`
- `src/client/java/com/zephyr/client/mixin/disable/PortalGuiClosing/LocalPlayerMixin.java:1` - 21 lines
- `src/client/java/com/zephyr/client/mixin/qol/InventoryPackets/ServerPlayerMixin.java:1` - New 21 lines
- Remove `src/client/java/com/zephyr/client/module/disable/disableInventoryEffectRendering.java:9`
- `src/client/java/com/zephyr/client/module/disable/disableNauseaOverlay.java:1` - 2 lines
- `src/client/java/com/zephyr/client/module/qol/InventoryPackets.java:1`- New 11 lines
- `src/client/java/com/zephyr/client/module/qol/PotionSaver.java:1` - 2 lines
- `src/client/resources/zephyr.client.mixins.json:1` - 6 lines
- **Stat:** 12 files, 89+/88-

## `a462a2b` - 2026-08-03 - Initial commit

- `.gitattributes:9`, `.github/workflows/build.yml:30`, `.gitignore:40`, `LICENSE:121`, `README.md:9`, `build.gradle:111`, `gradle.properties:20`, `gradle/wrapper/*`, `gradlew:248` + `.bat:82`, `settings.gradle:13`
- `src/client/java/com/zephyr/client/ZephyrClient.java:36` - Mod initializer, tick registration, GUI hooks
- `src/client/java/com/zephyr/client/configplusgui/*:8` files - `BooleanSetting:17`, `Category:25`, `ClickGuiScreen:362`, `ConfigManager:124`, `EnumSetting:52`, `GuiKeybindHandler:32`, `Module:96`, `ModuleManager:117`, `NumberSetting:56`, `Setting:29`
- `src/client/java/com/zephyr/client/discord/DiscordPresenceManager.java:218`
- `src/client/java/com/zephyr/client/mixin/**:36` mixins - combat (AnimeProtagonist, AutoPlace, BreachSwap, DensitySwap, KillAura, LungeSwap, Reach, ShieldBreaker), disable (AxeStripping, BlockBreakingCooldown, BlockBreakingParticles, DeadMobRendering, FogRendering, InventoryRenderer, NauseaOverlay, NetherPortalSound, PortalGuiClosing, RainEffects, ShovelPathing), movement (AntiHunger, HighJump, NoFall, TridentBoost), qol (AutoTool, FullBright, GuiMove, ItemRestock, PickBeforePlace, PlayerESP, PotionSaver, Sneak, SpeedMine)
- `src/client/java/com/zephyr/client/module/**:30+` modules - `combat/*` (AnimeProtagonist, AutoPlace, BreachSwap, Criticals, DensitySwap, KillAura, KillWyvern, LungeSwap, Reach, ShieldBreaker), `disable/*` (10 toggles), `movement/*` (Aerodynamics, AirJump, AntiHunger, ElytraBoost, Flight, HighJump, NoFall, Sprint, Step, TridentBoost), `qol/*` (AutoTool, ContainerESP placeholder, DiscordPresence, DurabilitySwap, FastAttack, FastUse, FullBright, GuiMove, HoldAttack, HoldUse, InventoryPackets, ItemRestock, PeriodicAttack, PeriodicUse, PickBeforePlace, PlayerESP, PotionSaver, RenderInvisibility, Sneak, SpeedMine, Xray)
- **Stat:** initial scaffolding, ~2.5k+ lines


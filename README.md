# Zephyr Module Notes

## Disclaimer

This project does not include anti-cheat bypasses. Most features here are direct client-side edits, simple packet edits, or small vanilla-behavior replays. They make the client do different things; they do not try to hide that well.

## Runtime wiring

- `ZephyrClient` registers most always-on tick handlers from `ClientTickEvents.END_CLIENT_TICK`.
- `ZephyrConfig` is the saved-state source of truth. It loads `config/zephyr.json`, pushes values into static booleans/settings, then writes the normalized state back out again.
- `ZephyrKeybindManager` owns the modifier-based key system. Each action is a chord: one shared modifier key plus one specific key.
- `ZephyrMenuScreen` is the in-game toggle UI. Most booleans can be flipped there even if the backing code lives in a mixin.

## Main hooks

- `ClientConnectionMixin` is the outgoing packet hook. `AntiHunger`, `NoFall`, `SpeedMine`, and `Blink` all pass through it.
- `ClientPlayerInteractionManagerMixin` is the item and combat hook. It drives `Criticals`, `PearlBoost`, `ElytraBoost`, `ItemRestock`, and dead-mob interaction blocking.
- `DeathScreenMixin`, `LivingEntityJumpMixin`, `BlockCollisionShapeMixin`, `TridentItemMixin`, `FireworkRocketItemMixin`, `CameraMixin`, `MouseMixin`, `GuiMoveMixin`, and the renderer mixins handle the rest of the behavior-specific changes.

## Modules

### Aerodynamics

- Tick hook: `Aerodynamics.tick`.
- Runs only while enabled, with a player present, and while the player is airborne.
- `AllowElytra` is hardcoded `true`, so elytra flight is allowed even though the class still has the old branch for disallowing it.
- It only adds velocity while sprinting, or while elytra-flying with the sprint key held.
- The acceleration direction is `player.getRotationVector().normalize()`.
- Every tick it calls `player.addVelocity(direction * acceleration)` and sets `player.velocityModified = true`.
- Acceleration is clamped to `0.005D .. 0.15D`. Default is `0.04D`.

### AirJump

- Tick hook: `AirJump.tick`.
- Hard dependency: if `NoFall.enabled` is false, `AirJump` resets its edge-detection state and does nothing.
- Only runs while airborne. Touching ground clears `wasJumpPressed`.
- On a fresh jump press, it directly calls `player.jump()`.
- The `level` field tracks Y level and can be decremented by sneak, but the current code does not actually use that field to enforce a limit.

### AntiHunger

- Tick hook: `AntiHunger.onTick`.
- Packet hook: `AntiHunger.onSendPacket`.
- Cancels outgoing `START_SPRINTING` command packets.
- For outgoing `PlayerMoveC2SPacket`s, it rewrites `onGround` to `false` when the player is on ground, not falling, and not currently breaking a block.
- The first move packet after landing is intentionally skipped through `ignoreNextMovePacket`.
- It does nothing while mounted, touching water, or submerged.

### AutoRespawn

- Screen hook: `AutoRespawn.onScreenOpen` through `DeathScreenMixin`.
- If enabled and a `DeathScreen` is about to open, it immediately calls `requestRespawn()`.
- `DeathScreenMixin` then cancels the screen open, so the death UI never appears.

### AutoTool

- Block-break hook: `AttackBlockMixin`.
- On the first `attackBlock` call, it scans hotbar slots `0..8`.
- It compares `ItemStack.getMiningSpeedMultiplier(state)` and picks the fastest tool above the baseline `1.0f`.
- If it finds one, it directly rewrites `inventory.selectedSlot`.

### Blink

- Active use action is separate from the toggle. `Blink.CanUseKeybind` only decides whether the blink hotkey is allowed.
- Packet hook: `Blink.onSendPacket`.
- Tick hook: `Blink.tick`.
- It buffers only `PlayerMoveC2SPacket`s. Everything else still sends normally.
- Duplicate movement packets are dropped by comparing position, yaw, pitch, and `onGround` against the last buffered packet.
- On enable it stores `startPos`, clears the packet buffer, and can spawn a fake local clone if `RENDER_ORIGINAL` stays `true`.
- The fake player mirrors pose, rotation, selected slot, and full inventory contents.
- If the real player gets more than `7` blocks away from the fake player, the module disables and flushes the packet buffer.
- Normal disable replays buffered movement packets in order.
- `cancel()` drops the buffered path, teleports the player back to `startPos`, and zeros velocity.
- `DELAY_TICKS` exists but is `0`, so timed resend is currently inactive.

### Criticals

- Attack hook: `Criticals.onAttack` before the real attack packet send.
- If the player is on ground, not mounted, not climbing, and not in water or lava, it sends two spoofed move packets:
  - `(x, y + spoofHeight, z, false)`
  - `(x, y, z, false)`
- The goal is to make the next hit look airborne.
- `spoofHeight` is clamped to `>= 0.0`. Default is `2.0`.
- `forceCrit()` exists but nothing in the current code calls it.

### DurabilitySwap

- Tick hook: `DurabilitySwap.onTick`.
- Checks the currently held main-hand item only.
- If that item is damageable and has `1` durability left, it scans the hotbar for the first non-empty replacement item.
- Damageable replacements are skipped if they also only have `1` durability left.
- If a replacement is found, it rewrites `selectedSlot` and sends an action bar message.
- Highkey I should have done a percentage, but this exists...

### ElytraBoost

- Item hook: `ClientPlayerInteractionManagerMixin.interactItem`.
- Block-use hook: `FireworkRocketItemMixin.useOnBlock`.
- Tick hook: `ElytraBoost.tick`.
- If the held stack is a firework rocket and the module is intercepting it, normal rocket use is replaced.
- `canStartBoost(...)` requires a rocket, `dontConsumeFirework == true`, no screen open, and active elytra flight.
- Starting a boost does not consume a rocket. It only starts a local timer using a vanilla-like lifetime formula based on `fireworkLevel`.
- While the timer is active, the module applies a vanilla-like firework acceleration blend every tick and spawns particles every other tick.
- Optional launch sound is played locally once at boost start.
- If interception is active but boost conditions fail, the use hook returns `ActionResult.FAIL`.
- `useOnBlock` is also blocked while rocket interception is active, so rockets cannot be placed on blocks in that state.

### ElytraSwap

- Tick hook: `ElytraSwap.onTick`.
- Checks only the block directly below the player.
- If that block is air and the chest slot is not an elytra, it scans inventory for an elytra and equips it.
- If that block is not air and the chest slot is an elytra, it scans for a chestplate and equips it.
- The actual swap is three `clickSlot(..., PICKUP, ...)` calls using armor slot `6`.
- Chestplate detection is limited to the vanilla armor tiers hardcoded in `isChestplate(...)`.

### F5Tweaks

- Camera hooks: `GameOptionsMixin`, `MouseMixin`, `CameraMixin`, `CameraClipMixin`.
- Only applies in `Perspective.THIRD_PERSON_BACK`.
- When active, mouse movement rotates stored `cameraYaw` and `cameraPitch` instead of rotating the player.
- Camera position is still anchored to the player, then moved back a fixed distance.
- `clipToSpace` is bypassed, so the camera does not get pushed inward by blocks while free-looking.
- `GameOptionsMixin` also skips `THIRD_PERSON_FRONT`, so perspective cycling becomes first-person <-> back-view only while enabled.

### FastAttack

- Tick hook: `FastAttack.tick`.
- Repeats `simulateAttack(...)` `TimesPerTick` times every client tick.
- Entity targets call `attackEntity(...)`.
- Block targets call `attackBlock(...)`.
- Misses still swing the main hand.
- `TimesPerTick` is clamped to `1 .. 20`. Default is `10`.

### FastUse

- Tick hook: `FastUse.tick`.
- Repeats `simulateUse(...)` `TimesPerTick` times every client tick.
- If the crosshair is on a block, it calls `interactBlock(...)`; otherwise it calls `interactItem(...)`.
- It always swings the main hand after each simulated use.
- `TimesPerTick` is clamped to `1 .. 20`. Default is `10`.

### Flight

- Tick hook: `Flight.tick`.
- Enable and disable paths: `onEnable()` and `onDisable()`.
- Only mode left is `ABILITIES`.
- On enable and every tick, it forces `allowFlying = true`, `flying = true`, and fly speed `0.1f`.
- On disable, it clears `flying`, clears `allowFlying` for non-creative players, and restores fly speed to `0.05f`.
- It does not spoof anything. It just rewrites vanilla ability flags.
- OKAY SO I WAS REWRITING IT WHEN IT HAD DIFFERENT MODES I JUST DIDNT CHANGE THE ENUM STUFF :sob:

### FreeCam

- Tick hook: `FreeCam.tick`.
- Camera hooks: `CameraMixin`, `MouseMixin`, `CameraClipMixin`.
- Input hook: `KeyboardInputMixin`.
- On first activation it snapshots the current camera position and player yaw/pitch.
- After that, camera movement is handled entirely through local `x/y/z/yaw/pitch` fields.
- Movement uses forward, right, and vertical vectors built from the stored camera rotation, not the player rotation.
- Sprint multiplies speed by `2.5`.
- While enabled, `KeyboardInputMixin` zeros movement input so the player body stops walking around.
- The player stays where they are; only the camera moves.
- Freecam genuinely just sucks. please don't use it.

### GhostHand

- Use hook: `DoItemUseMixin`.
- Only runs while use is held and the player is not sneaking.
- Performs a raycast, then walks forward along the view direction in small steps.
- For each unique block position, if the block has a block entity, it synthesizes a centered `BlockHitResult` and tries `interactBlock(...)` with both hands.
- On the first accepted interaction, it swings that hand and exits.
- This is effectively a scan for chest-like blocks behind the normal front block.

### GuiMove

- Input hook: `GuiMoveMixin`.
- While any screen is open and the module is enabled, it polls the physical movement, jump, and sprint keys directly from the window handle.
- It pushes those states into the normal keybindings so movement continues with GUIs open.
- When the screen closes, it restores control to vanilla key handling.
- It does not cover attack or use.

### HighJump

- Jump hook: `LivingEntityJumpMixin`.
- Multiplies the stored jump velocity local inside `LivingEntity.jump`.
- It changes the actual vanilla jump impulse rather than adding velocity later.
- Multiplier is clamped to `1.0 .. 5.0`. Default is `2.0`.

### HoldAttack

- Tick hook: `HoldAttack.onTick`.
- The current implementation sets `mc.options.useKey.setPressed(true)`.
- That means this class is functionally another hold-use toggle right now, not a hold-attack toggle.

### HoldUse

- Tick hook: `HoldUse.onTick`.
- Sets `mc.options.useKey.setPressed(true)` every tick while enabled.
- This is the expected continuous-right-click behavior.

### HotbarRowSwap

- Tick hook: `HotbarRowSwap.tick`.
- HUD hook: `HotbarRowSwapHudMixin`.
- Mouse hook: `HotbarRowSwapMouseMixin`.
- While the configured action key is held, the overlay opens and scroll wheel changes `selectedRow`.
- Releasing the key triggers a swap between the hotbar and the selected main-inventory row.
- The swap is done with nine `SlotActionType.SWAP` clicks, one for each column.
- If a GUI is open, the player is missing, or the cursor stack is not empty, the selection is cleared or the swap is skipped.

### ItemRestock

- Use hooks: `ClientPlayerInteractionManagerMixin` on item use and block use.
- Totem-pop hook: `ClientPlayNetworkHandlerMixin`.
- Tick hook: `ItemRestock.tick`.
- It tracks pending use separately for `MAIN_HAND` and `OFF_HAND`.
- On an accepted use, it stores a copy of the consumed stack as a restock template and keeps watching that hand for up to `40` ticks.
- If the hand empties or the count drops while the same item is still there, it refills that hand from inventory.
- Matching is strict: `ItemStack.areItemsAndComponentsEqual(...copyWithCount(1), ...copyWithCount(1))`.
- Source selection picks the matching inventory slot with the largest stack count, excluding the target slot and the opposite hand slot.
- The refill itself is three pickup clicks: source -> target -> source.
- Totem handling is separate: after local status byte `35`, it checks for up to `5` ticks whether a totem disappeared from either hand and then restocks it.

### Jesus

- Tick hook: `Jesus.tick`.
- Collision hook: `BlockCollisionShapeMixin`.
- If the controlled entity is touching water, or in lava while not sneaking, it zeros Y velocity.
- If the block directly below is liquid, it also zeros Y velocity.
- If mounted, the vehicle is used as the controlled entity instead of the player.
- The collision hook returns `VoxelShapes.fullCube()` for liquid blocks below the player when approaching from above and not already submerged.
- This is a small surface spoof, not a broad liquid-walk implementation.

### LongJump

- Tick hook: `LongJump.tick`.
- On a fresh jump press while on ground, it writes horizontal velocity based on look direction.
- Y velocity is preserved from the current velocity; it is not boosted here.
- If the look vector is too small, it falls back to a yaw-only horizontal direction.
- It also forces sprinting and sets `velocityModified = true`.
- Momentum is clamped to `0.1 .. 2.0`. Default is `0.6`.

### NoFall

- Packet hook: `NoFall.onSendPacket`, only for move packets.
- If enabled, it rewrites outgoing move packets to `onGround = true` once the fall should count as landed.
- It skips creative, spectator, and ability-flight players.
- Normal path: if `fallDistance > 2.5F`, the next move packet is marked grounded.
- Elytra path: it only spoofs grounding when the player is descending and the bounding box shifted down by `0.6` is no longer empty.

### PearlBoost

- Item hook: `ClientPlayerInteractionManagerMixin.interactItem`.
- Tick hook: `PearlBoost.tick`.
- State machine: `IDLE -> BOOSTING -> THROWING`.
- Initial pearl use is intercepted if the held item is an ender pearl.
- It stores the hand, current velocity, and sprinting state, then returns `ActionResult.SUCCESS` before vanilla throw logic runs.
- Next tick it sets sprinting, writes `direction * boostVelocity` into player velocity, and manually sends movement packets.
- Following tick it replays the original item use, restores the old velocity and sprinting state, sends movement packets again, and resets.
- `boostVelocity` is clamped to `0.5D .. 20.0D`. Default is `10.35D`.

### PeriodicAttack

- Tick hook: `PeriodicAttack.tick`.
- Counts client ticks and runs one simulated attack every `delayTicks`.
- Attack logic is the same shape as `FastAttack`: entity attack, block attack, or miss swing.
- `delayTicks` is clamped to `1 .. 200`. Default is `20`.

### PeriodicUse

- Tick hook: `PeriodicUse.tick`.
- Counts client ticks and runs one simulated use every `delayTicks`.
- Use logic is the same shape as `FastUse`: block interact if targeting a block, otherwise item interact.
- `delayTicks` is clamped to `1 .. 200`. Default is `20`.

### PickBeforePlace

- Block-place hook: `BlockPlaceMixin`.
- Only affects main-hand `interactBlock(...)`.
- Before the real placement call, it triggers Minecraft's middle-click pick-block action through `BlockPickInvoker`.
- It then immediately re-calls `interactBlock(...)` with the newly picked block.
- `isPicking` prevents infinite recursion inside the mixin.

### RenderInvisibility

- Render hook: `InvisibilityRenderMixin`.
- Redirects `Entity.isInvisibleTo(...)` so the internal `isInvisible()` call returns `false` while enabled.
- In practice this makes invisible entities render as visible to the client.

### Sneak

- Tick hook: `Sneak.onTick`.
- Forces `mc.options.sneakKey.setPressed(true)` every tick while enabled.

### SpeedMine

- Tick hook: `SpeedMine.tick`.
- Packet hook: `SpeedMine.onSendPacket`.
- Modes: `OFF`, `HASTE`, `DAMAGE`.
- `HASTE` gives the local player an infinite hidden Haste II effect whenever the current effect is missing or weaker.
- `DAMAGE` tracks the current breaking position and progress through `CurrentBreakingPosAccessor`.
- If `currentBreakingProgress + calcBlockBreakingDelta(...) >= 0.7f` and it has not already fired, it sends an early `STOP_DESTROY_BLOCK`.
- That stop packet uses a valid sequence number from `ClientWorld.getPendingUpdateManager()` through `ClientWorldAccessor`.
- `START_DESTROY_BLOCK`, `ABORT_DESTROY_BLOCK`, and `STOP_DESTROY_BLOCK` packets update local tracking state so the early stop only fires once per break cycle.

### Sprint

- Tick hook: `Sprint.onTick`.
- If enabled and the player is not touching water, it checks whether forward or sideways input is non-zero.
- If so, it directly calls `setSprinting(true)`. Otherwise it clears sprinting.
- There is no hunger logic or extra spoofing beyond vanilla behavior after the state change.

### Step

- Toggle path: `Step.setEnabled(...)`.
- Tick hook: `Step.tick`.
- Rewrites the player's `EntityAttributes.GENERIC_STEP_HEIGHT` base value.
- On enable, it stores the old base value and writes `stepHeight`.
- While enabled, `tick()` reapplies the custom value every tick in case something else overwrites it.
- On disable or `reset()`, it restores the saved base value.
- Default custom step height is `1.25`.

### TridentBoost

- Use hook: `TridentItemMixin.use`.
- Release hook: `TridentItemMixin.onStoppedUsing`.
- Allows Riptide tridents to start charging outside water by consuming the use action locally and setting the active hand.
- Release handling only continues if the trident was charged for at least `10` ticks.
- It computes a direction vector from yaw and pitch, scales it by Riptide strength, and adds that velocity to the player.
- It then calls `player.useRiptide(...)`, optionally bumps the player upward if they were on ground, and plays the matching Riptide sound.
- The dry-use path is blocked if the trident is about to break.

### ArmorRenderer

- `ArmorRenderer` currently has no code in it.
- It is a placeholder, not an active feature.

## Disables

### Disable Portal GUI Closing

- Hook: `ClientPlayerEntityMixin.tickNausea`.
- It rewrites the `MinecraftClient.currentScreen` read to `null`.
- That makes vanilla portal nausea logic think no screen is open, so the current GUI is not forcibly closed by portal effect handling.

### Disable Dead Mob Interaction

- Hooks: `ClientPlayerInteractionManagerMixin.attackEntity`, `interactEntity`, and `interactEntityAtLocation`.
- If the target is a `LivingEntity` that is not alive, already removed, or has `deathTime > 0`, the interaction is canceled or failed.

### Disable Axe Stripping

- Hook: `AxeItemMixin.useOnBlock`.
- If enabled, the mixin returns `ActionResult.PASS` at method head.
- That prevents the axe from running its normal strip/scrape/wax-off logic through this path.

### Disable Shovel Pathing

- Hook: `ShovelItemMixin.useOnBlock`.

### Disable Block Break Cooldown

- Hook: `BlockBreakingCooldownMixin.tick`.
- Every client tick, if enabled, it writes `blockBreakingCooldown = 0` directly inside `ClientPlayerInteractionManager`.

### Disable Block Break Particles

- Hook: `BlockParticleMixin.addBlockBreakParticles`.
- If enabled, it cancels the client-side block break particle spawn entirely.

### Disable Inventory Effect Rendering

- Hook: `InventoryRendererMixin.drawStatusEffects`.
- If enabled, it cancels the inventory-side potion/effect panel rendering.

### Disable Nausea Overlay

- Hooks: `NauseaOverlayMixin.renderNausea` and `NauseaVignetteMixin.renderVignetteOverlay`.
- If enabled, both the wobble overlay and the vignette path are canceled.
- The class comment notes that distortion effects may also need to be turned down in settings for the result to fully match expectations.

### Disable Portal Sound

- Hook: `SoundManagerMixin.play(SoundInstance)`.
- If the sound id is exactly `SoundEvents.BLOCK_PORTAL_AMBIENT`, the sound is canceled while the toggle is enabled.

### Disable Fog

- Hook: `BackgroundRendererMixin.applyFog`.
- If enabled, the fog application method is canceled at the head.

### Disable First Person Effect Particles

- Hook: `LivingEntityRendererMixin.tickStatusEffects`.
- Redirects the status-effect particle spawn call.
- If the entity is the local player and the camera is in first person, the particle spawn is skipped.
- Other entities still spawn their effect particles normally.

### Disable Rain Effects

- Render hook: `NoRainRenderMixin.renderWeather`.
- If enabled, weather rendering is canceled.

### Disable Dead Mob Rendering

- Hook: `DeadMobRenderingMixin.render`.
- If enabled, dead or dying `LivingEntity` renders are canceled at method head.
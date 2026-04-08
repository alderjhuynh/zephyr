# Zephyr Module Notes

## Disclaimer

This project does **not** contain anti-cheat bypasses. The modules here are meant for singleplayer use and permissive anarchy-style servers only. Most of them are straightforward client-side state edits, packet flag edits, or vanilla-behavior replays; they are not built to evade server-side checks.

## Runtime wiring

- `ZephyrClient` registers most modules on `ClientTickEvents.END_CLIENT_TICK`.
- `ClientConnectionMixin` is the main outbound packet hook. It lets `AntiHunger`, `NoFall`, `SpeedMine`, and `Blink` inspect or modify packets before send.
- `ClientPlayerInteractionManagerMixin` is the main item/attack hook. It triggers `Criticals`, `PearlBoost`, `ElytraBoost`, and `ItemRestock`.
- `DeathScreenMixin`, `BlockCollisionShapeMixin`, `LivingEntityJumpMixin`, `TridentItemMixin`, `FireworkRocketItemMixin`, and `ClientPlayNetworkHandlerMixin` provide the remaining behavior-specific hooks.

## Modules

### Aerodynamics

- Tick hook: `Aerodynamics.tick`.
- Active only when `enabled == true`, the player exists, the player is not on ground, and the player is not elytra-flying.
- Reads `player.getRotationVector()`, normalizes it, and uses that exact look vector as acceleration direction.
- Adds velocity every tick with `player.addVelocity(direction * acceleration)`.
- Marks `player.velocityModified = true` so the movement change is applied immediately.
- Acceleration is clamped to `0.005D .. 0.15D`. Default is `0.04D`.

### AirJump

- Tick hook: `AirJump.tick`.
- Does nothing unless `enabled == true`.
- Hard dependency on `NoFall.enabled`; if `NoFall` is off, `AirJump` resets its edge-detection state and exits.
- Only runs while the player is airborne. On ground, it clears `wasJumpPressed`.
- Detects a fresh jump press with `isJumpPressed && !wasJumpPressed`, then calls `player.jump()` directly.
- Tracks a `level` field using `player.getBlockPos().getY()`, but that value is not currently used to enforce a jump limit. Sneak decrements `level`, but the current implementation never reads that decrement back for gating.

### AntiHunger

- Tick hook: `AntiHunger.onTick`.
- Packet hook: `AntiHunger.onSendPacket` from `ClientConnectionMixin`.
- When the client lands (`onGround` changed from `false` to `true`), it sets `ignoreNextMovePacket = true` so the first landing packet is left untouched.
- Cancels outgoing `ClientCommandC2SPacket.Mode.START_SPRINTING` packets by returning `false` from the packet hook.
- For outgoing `PlayerMoveC2SPacket`s, if the player is on ground, has `fallDistance <= 0`, and is not currently breaking a block, it mutates the packet’s private `onGround` field to `false` through `PlayerMoveC2SPacketAccessor`.
- It skips all of this while mounted, touching water, or submerged.

### AutoRespawn

- Screen hook: `AutoRespawn.onScreenOpen` from `DeathScreenMixin`.
- When a `DeathScreen` is about to open and the module is enabled, it immediately calls `client.player.requestRespawn()`.
- `DeathScreenMixin` then cancels `MinecraftClient.setScreen(...)` for `DeathScreen`, so the death UI never appears.

### Blink

- Tick hook: `Blink.tick`.
- Packet hook: `Blink.onSendPacket` from `ClientConnectionMixin`.
- Only intercepts `PlayerMoveC2SPacket`. Non-movement packets are not buffered.
- While enabled, movement packets are stored in `packets` instead of being sent immediately. `ClientConnectionMixin` cancels the send when `Blink.onSendPacket(...)` returns `true`.
- Duplicate movement packets are dropped by comparing `onGround`, yaw, pitch, and XYZ against the last buffered packet.
- On enable, it stores `startPos`, clears the packet buffer, and optionally spawns an `OtherClientPlayerEntity` clone (`fakePlayer`) at the real player position.
- The fake player’s position, angles, pose, on-ground flag, inventory contents, and selected hotbar slot are mirrored from the real player.
- If the real player moves more than `7` blocks from the fake player, `Blink.tick` force-disables the module and flushes the buffer.
- On normal disable, `dumpPackets(true)` replays buffered movement packets in order through `mc.getNetworkHandler().sendPacket(...)`.
- On `cancel()`, it disables, drops the buffered path instead of sending it, then teleports the local player back to `startPos` and zeroes velocity.
- `DELAY_TICKS` exists but is `0`, so timed auto-refresh is effectively disabled in the current code.

### Criticals

- Attack hook: `Criticals.onAttack` from `ClientPlayerInteractionManagerMixin` just before the attack packet send.
- If enabled and the player is on ground, not mounted, not climbing, and not in water/lava, it sends two extra movement packets:
  - `PositionAndOnGround(x, y + spoofHeight, z, false)`
  - `PositionAndOnGround(x, y, z, false)`
- The intent is to make the next hit look airborne to the server.
- `spoofHeight` defaults to `2.0` and is clamped to `>= 0.0`.
- `forceCrit()` exists and always sends the spoof pair when enabled, but nothing in the current code calls it. (I lowkey forgot I made this)

### Elytra Boost

- Item hook: `ClientPlayerInteractionManagerMixin.interactItem`.
- Block-placement hook: `FireworkRocketItemMixin.useOnBlock`.
- Tick hook: `ElytraBoost.tick`.
- If the held stack is a firework rocket and `enabled == true`, normal rocket use is intercepted.
- `canStartBoost(...)` requires:
  - firework rocket in hand,
  - `dontConsumeFirework == true`,
  - player/world present,
  - no open screen,
  - player currently `isFallFlying()`.
- When those checks pass, `startBoost(...)` does not consume a rocket. It only starts a local timer `remainingBoostTicks` using a vanilla-style lifetime formula based on `fireworkLevel`.
- Each tick while active, it applies a vanilla-like firework acceleration blend toward the look vector and spawns firework particles every other tick.
- Optional launch sound is played once when the boost starts.
- If the player is not actually in valid elytra-flight state, the timer is cleared.
- If a rocket item is used while the module is blocking rockets but boost conditions fail, the interaction hook returns `ActionResult.FAIL`.
- `FireworkRocketItemMixin` also blocks `useOnBlock` so rockets cannot be placed as blocks while interception is active.

### Flight

- Tick hook: `Flight.tick`.
- Enable path: `Flight.onEnable`.
- Disable path: `Flight.onDisable`.
- This is ability-flight only. The only mode left in the enum is `ABILITIES`.
- On enable and every tick, if the player is not a spectator, it forces:
  - `allowFlying = true`
  - `flying = true`
  - fly speed to `0.1f`
- On disable, it clears `flying`; if the player is not in creative mode it also clears `allowFlying`, and then restores fly speed to `0.05f`.
- It does not spoof packets or hide the ability state; it just toggles vanilla client ability flags.

### High Jump

- Jump hook: `LivingEntityJumpMixin` modifies the stored jump velocity local in `LivingEntity.jump`.
- `HighJump.modifyJumpVelocity(float original)` multiplies the vanilla jump impulse by `multiplier` when enabled.
- Multiplier range is `1.0 .. 5.0`. Default is `2.0`.
- This affects the actual jump velocity calculation directly rather than adding velocity later in the tick.

### Item Restock

- Item/block use hooks: `ClientPlayerInteractionManagerMixin` captures use attempts at method head and tracks accepted results at method return.
- Totem hook: `ClientPlayNetworkHandlerMixin.onEntityStatus` watches for status byte `35` on the local player.
- Tick hook: `ItemRestock.tick`.
- The module keeps two `PendingUse` trackers, one for `MAIN_HAND` and one for `OFF_HAND`.
- On an accepted item or block interaction, it stores a copy of the used stack as the restock template and watches that hand for up to `40` ticks.
- If the tracked hand becomes empty, or the stack count drops while still matching the same item/components, it restocks immediately.
- Restocking is done with three `clickSlot(...)` calls:
  - pick up source stack,
  - place into target hand slot,
  - place any remainder back into the source slot.
- Matching is strict: `ItemStack.areItemsAndComponentsEqual(candidate.copyWithCount(1), template.copyWithCount(1))`.
- The source selection picks the matching inventory slot with the highest stack count, excluding the target slot and the opposite hand slot.
- Main hand maps to the selected hotbar slot. Offhand maps to screen slot `45`.
- Totem restock is separate from normal use tracking:
  - the module snapshots previous main-hand and offhand stacks each tick,
  - after a local totem-pop packet, it checks for up to `5` ticks whether a totem disappeared from either hand,
  - if so, it runs the same `restockHand(...)` logic using the previous totem stack as the template.

### Jesus

- Tick hook: `Jesus.tick`.
- Collision hook: `BlockCollisionShapeMixin` delegates `AbstractBlock.getCollisionShape(...)` to `Jesus.getCollisionShape(...)`.
- The tick path is very simple: it zeros vertical velocity in several liquid-adjacent states.
- If the controlled entity is touching water, it sets Y velocity to `0`.
- If the controlled entity is in lava and not sneaking, it also sets Y velocity to `0`.
- If the block directly below the entity is water or lava, it also sets Y velocity to `0`.
- If the player is mounted, the vehicle is used as the controlled entity instead of the player.
- The collision hook returns `VoxelShapes.fullCube()` for liquid blocks below the player when:
  - the module is enabled,
  - the context entity is a non-spectator player,
  - the liquid block’s Y is below the player’s Y,
  - the player is not already touching water or lava.
- That means the module fakes a solid collision surface over liquids only in the specific approach case handled by the mixin.

### Long Jump

- Tick hook: `LongJump.tick`.
- On a fresh jump press while on ground, it calls `applyForwardMomentum(...)`.
- That method normalizes `player.getRotationVector()` and writes horizontal velocity with:
  - `x = direction.x * momentum`
  - `z = direction.z * momentum`
  - `y = currentVelocity.y`
- Because only `x` and `z` are written, vertical velocity is not boosted here.
- It also forces `player.setSprinting(true)` and sets `player.velocityModified = true`.
- If the rotation vector is effectively zero, it falls back to a yaw-only horizontal vector.
- Momentum range is `0.1 .. 2.0`. Default is `0.6`.

### NoFall

- Packet hook: `NoFall.onSendPacket` from `ClientConnectionMixin`, only for `PlayerMoveC2SPacket`.
- If enabled, it mutates the packet’s `onGround` field to `true` through `PlayerMoveC2SPacketAccessor` when the fall should be “landed” early.
- It does nothing for creative, spectator, or ability-flying players.
- Normal fall path: if `player.fallDistance > 2.5F`, the next outgoing move packet is marked on-ground.
- Elytra path: if the player is `isFallFlying()`, it only spoofs landing when:
  - Y velocity is negative, and
  - the player’s bounding box offset downward by `0.6` is no longer space-empty.
- If those elytra checks fail, the packet is left alone.

### Pearl Boost

- Item hook: `ClientPlayerInteractionManagerMixin.interactItem`.
- Tick hook: `PearlBoost.tick`.
- Uses a two-step state machine: `IDLE -> BOOSTING -> THROWING`.
- On the initial pearl use, `queueThrow(...)`:
  - checks that the module is enabled,
  - rejects re-entry during replay,
  - requires an ender pearl in the hand,
  - stores `pendingHand`,
  - snapshots the player’s current velocity and sprinting state,
  - returns `ActionResult.SUCCESS` so the original interact call is intercepted immediately.
- On the next tick in `BOOSTING`, it:
  - computes the normalized look vector,
  - forces sprinting,
  - sets velocity to `direction * boostVelocity`,
  - calls the private `ClientPlayerEntity.sendMovementPackets()` through `ClientPlayerEntityInvoker`,
  - advances to `THROWING`.
- On the following tick in `THROWING`, it:
  - sets `replayingUse = true`,
  - manually calls `mc.interactionManager.interactItem(player, pendingHand)` to throw the pearl after the boosted movement packets were already sent,
  - restores the original velocity and sprinting state,
  - sends movement packets again,
  - resets internal state.
- Default boost velocity is `10.35D`, clamped to `0.5D .. 20.0D`.

### Speed Mine

- Tick hook: `SpeedMine.tick`.
- Packet hook: `SpeedMine.onSendPacket` from `ClientConnectionMixin`.
- Mode machine: `OFF`, `HASTE`, `DAMAGE`.
- `HASTE` mode continuously gives the local player a hidden infinite Haste II effect (`amplifier == 1`) if the current Haste effect is missing or weaker.
- `DAMAGE` mode reads `currentBreakingPos` and `currentBreakingProgress` from `ClientPlayerInteractionManager` through `CurrentBreakingPosAccessor`.
- For the tracked block, it also computes `state.calcBlockBreakingDelta(...)`.
- If `currentBreakingProgress + delta >= 0.7f` and a stop packet has not already been triggered, it sends an early `STOP_DESTROY_BLOCK` packet.
- That stop packet is constructed with a valid client sequence from `ClientWorld.getPendingUpdateManager()` via `ClientWorldAccessor`.
- Packet tracking behavior in `DAMAGE` mode:
  - `START_DESTROY_BLOCK` stores the breaking position and face and clears the trigger flag.
  - `ABORT_DESTROY_BLOCK` on the tracked position resets state.
  - `STOP_DESTROY_BLOCK` marks the trigger as already used.
- If the block turns to air or progress resets, local tracking state is cleared.

### Sprint

- Tick hook: `Sprint.onTick`.
- If enabled, the player exists, and the player is not touching water, it checks whether `movementForward != 0` or `movementSideways != 0`.
- It then directly calls `mc.player.setSprinting(isMoving)`.
- There is no hunger check, cooldown handling, or packet spoofing beyond whatever vanilla does after the sprint state changes.

### Step

- Tick hook: `Step.tick`.
- Toggle path: `Step.setEnabled(boolean)`.
- Uses the player attribute `EntityAttributes.GENERIC_STEP_HEIGHT`.
- When enabled, it stores the current base attribute value in `previousStepHeight` and writes `stepHeight` into the attribute.
- While enabled, `tick()` reapplies `stepHeight` every tick in case something else overwrites the attribute.
- When disabled, it restores the previously stored base value.
- `reset()` also restores the previous base value and clears `enabled`.
- Default custom step height is `1.25`.

### Trident Boost

- Use hook: `TridentItemMixin.use`.
- Release hook: `TridentItemMixin.onStoppedUsing`.
- The use hook allows a Riptide trident to begin charging outside water by returning `TypedActionResult.consume(stack)` and setting the active hand manually.
- `canUseOutsideWater(...)` requires:
  - module enabled,
  - held item is a trident,
  - the trident has Riptide (`getTridentSpinAttackStrength(...) > 0`),
  - player is not touching water or rain,
  - the trident is not one durability from breaking.
- On release, `handleDryRiptide(...)` only proceeds if the trident was charged for at least `10` ticks.
- It computes a normalized movement vector from player yaw/pitch, scales it by Riptide strength, and adds that velocity to the player.
- It then calls `player.useRiptide(20, 8.0F, stack)` to apply the spin-attack state.
- If the player is on ground, it immediately moves them upward by `1.1999999F`.
- Finally it plays the appropriate Riptide sound variant and cancels the vanilla `onStoppedUsing` flow.

package com.zephyr.client.module.combat;

import com.zephyr.client.TickScheduler;
import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.mixin.combat.AbstractArrowInvoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Automatically places a rail, TNT minecart, and fire to catch the local player's own
 * crossbow arrows: the arrow's trajectory is predicted, a rail and minecart are placed at
 * the landing block, and fire is placed behind the landing position so the arrow ignites it
 * and detonates the cart.
 *
 * <p>With the {@code Legit} setting enabled, the module instead intercepts the player's
 * crossbow fire attempt, spreads rail, cart, and fire placement across separate ticks with
 * randomized humanlike jitter, and only then fires the crossbow programmatically.</p>
 */
public final class XBowCart extends Module {
    public static final XBowCart INSTANCE = new XBowCart();

    /** Number of ticks of trajectory simulation used to predict where an arrow will land. */
    private static final int LOOKAHEAD_TICKS = 8;

    /** Maximum ticks of pre-fire trajectory simulation used in legit mode. */
    private static final int MAX_PREDICTION_TICKS = 300;

    private static final double ARROW_GRAVITY = 0.05;
    private static final double ARROW_DRAG = 0.99;
    private static final double CROSSBOW_POWER = 3.15;

    private final Map<Integer, BlockPos> predictions = new HashMap<>();
    private final Set<Integer> processed = new HashSet<>();
    private final Random random = new Random();

    /** True while a legit placement sequence is scheduled and not yet finished. */
    private boolean sequenceActive;

    /** Set while programmatically firing so the mixin lets the fire through. */
    private boolean suppressIntercept;

    /** The selected slot held before the legit sequence started; restored on abort/finish. */
    private int sequenceSlot;

    private XBowCart() {
        super("XBowCart", "Places a rail, TNT minecart, and fire to catch your own crossbow arrows", Category.COMBAT);
        addSetting(legit);
        addSetting(placementDelay);
        addSetting(jitter);
        addSetting(safety);
        addSetting(accurateDamage);
        addSetting(onlyIfLethal);
    }

    /** When enabled, cancels the crossbow fire and places rail, cart, then fire first. */
    public final BooleanSetting legit = new BooleanSetting("Legit", false);

    /** Base ticks between each placement step in legit mode. */
    public final NumberSetting placementDelay = new NumberSetting("Placement Delay", 2.0, 0.0, 10.0, 1.0);

    /** Max random extra ticks added to each legit placement step for humanlike jitter. */
    public final NumberSetting jitter = new NumberSetting("Jitter", 2.0, 0.0, 6.0, 1.0);

    /** When enabled, places the most blast resistant block between the player and the cart as a shield. */
    public final BooleanSetting safety = new BooleanSetting("Safety", false);

    /** When enabled, uses exposure-accurate explosion damage (raytraces + armor reduction) for lethal checks. */
    public final BooleanSetting accurateDamage = new BooleanSetting("Accurate Damage", false);

    /** When enabled with Safety, only places the shield if the cart explosion would kill the player. */
    public final BooleanSetting onlyIfLethal = new BooleanSetting("Only If Lethal", false);

    /** Tracks the predicted landing position for each in-flight arrow and places fire, rail, and minecart. */
    @Override
    public void tick(Minecraft client) {
        if (client.player == null
                || client.level == null
                || client.gameMode == null) {
            return;
        }

        if (legit.get()) {
            cleanup(client);
            return;
        }

        if (!hasValidCrossbow(client))
            return;

        LocalPlayer player = client.player;
        double rangeSq = player.blockInteractionRange();
        rangeSq *= rangeSq;

        cleanup(client);

        for (Entity entity : client.level.entitiesForRendering()) {

            if (!(entity instanceof Arrow arrow))
                continue;

            if (arrow.getOwner() != player)
                continue;

            if (((AbstractArrowInvoker) arrow).zephyr$isInGround())
                continue;

            int id = arrow.getId();

            if (processed.contains(id))
                continue;

            if (player.distanceToSqr(arrow) > rangeSq)
                continue;

            predictions.computeIfAbsent(id, ignored -> predict(client, arrow));
        }

        Iterator<Map.Entry<Integer, BlockPos>> iterator = predictions.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<Integer, BlockPos> entry = iterator.next();

            Entity entity = client.level.getEntity(entry.getKey());

            if (!(entity instanceof Arrow arrow)) {
                iterator.remove();
                continue;
            }

            if (((AbstractArrowInvoker) arrow).zephyr$isInGround()) {
                iterator.remove();
                processed.add(arrow.getId());
                continue;
            }

            if (place(client, arrow, entry.getValue())) {
                iterator.remove();
                processed.add(arrow.getId());
            }
        }
    }

    /**
     * Called from the {@code MultiPlayerGameMode#useItemOn} mixin when the local player
     * places a rail while legit mode is active. Checks for a loaded crossbow in the
     * hotbar, derives the rail position from the hit result, and schedules the
     * remaining cart, fire, and crossbow steps. The rail placement itself is not
     * cancelled.
     */
    public void onRailPlace(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (!isEnabled() || !legit.get())
            return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || client.level == null
                || client.gameMode == null)
            return;

        if (player != client.player)
            return;

        if (hand != InteractionHand.MAIN_HAND)
            return;

        if (player.isUsingItem())
            return;

        if (sequenceActive)
            return;

        ItemStack stack = player.getItemInHand(hand);
        if (!isRail(stack))
            return;

        if (findChargedCrossbowSlot(player) == -1)
            return;

        BlockPos railPos = hit.getBlockPos().relative(hit.getDirection());
        if (railPos == null)
            return;

        Vec3 velocity = player.getLookAngle()
                .multiply(CROSSBOW_POWER, CROSSBOW_POWER, CROSSBOW_POWER);

        BlockPos firePos = findFirePos(client, railPos, velocity);

        if (firePos == null)
            return;

        if ((findMinecartSlot(player) == -1 && !player.getOffhandItem().is(Items.TNT_MINECART))
                || findFlintAndSteelSlot(player) == -1)
            return;

        sequenceActive = true;
        startSequence(player, hand, railPos, firePos, velocity);
    }

    /** Kept for compatibility; legit mode is now triggered by rail placement. */
    public boolean onUseItemFireAttempt(Player player, InteractionHand hand) {
        return false;
    }

    /** Schedules the cart, fire, and crossbow steps across ticks with random jitter.
     *  The rail is assumed to have been placed by the player and is not scheduled. */
    private void startSequence(Player player, InteractionHand hand,
                               BlockPos railPos, BlockPos firePos) {
        startSequence(player, hand, railPos, firePos, player.getLookAngle().multiply(CROSSBOW_POWER, CROSSBOW_POWER, CROSSBOW_POWER));
    }

    private void startSequence(Player player, InteractionHand hand,
                               BlockPos railPos, BlockPos firePos, Vec3 velocity) {
        sequenceSlot = player.getInventory().getSelectedSlot();
        int step = placementDelay.get().intValue();
        int jit = jitter.get().intValue();

        int cartDelay = 1 + random.nextInt(jit + 1);
        int fireDelay = step + random.nextInt(jit + 1);
        int shootDelay = step + random.nextInt(jit + 1);

        // Determine if we need a shield in legit sequence. Evaluate at schedule time.
        BlockPos shieldPos = null;
        boolean doShield = false;
        int shieldDelay = 0;
        if (safety.get()) {
            boolean lethalCheck = !onlyIfLethal.get() || wouldKill(Minecraft.getInstance(), railPos, velocity);
            if (lethalCheck) {
                BlockPos candidate = findShieldPos(Minecraft.getInstance(), railPos, firePos);
                int bestSlot = findBestShieldSlot(player);
                if (candidate != null && bestSlot != -1) {
                    shieldPos = candidate;
                    doShield = true;
                    shieldDelay = step + random.nextInt(jit + 1);
                }
            }
        }

        TickScheduler.schedule(cartDelay, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (!isSequenceRunning(mc)) return;
            if (!placeCart(mc, railPos, true, true)) abortSequence(mc);
        });

        if (doShield) {
            BlockPos finalShieldPos = shieldPos;
            TickScheduler.schedule(cartDelay + shieldDelay, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (!isSequenceRunning(mc)) return;
                // Re-check lethal at execution time if needed; not aborting if not lethal anymore but we already decided.
                // Place shield; failure does not abort the whole sequence (shield is best-effort).
                placeShield(mc, finalShieldPos, true, true);
            });
            TickScheduler.schedule(cartDelay + shieldDelay + fireDelay, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (!isSequenceRunning(mc)) return;
                if (!placeFire(mc, firePos, true, true)) abortSequence(mc);
            });
            TickScheduler.schedule(cartDelay + shieldDelay + fireDelay + shootDelay, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (!isSequenceRunning(mc)) return;
                fireCrossbow(mc, hand);
            });
            TickScheduler.schedule(cartDelay + shieldDelay + fireDelay + shootDelay + 1, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.getInventory().setSelectedSlot(sequenceSlot);
                }
                sequenceActive = false;
            });
        } else {
            TickScheduler.schedule(cartDelay + fireDelay, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (!isSequenceRunning(mc)) return;
                if (!placeFire(mc, firePos, true, true)) abortSequence(mc);
            });

            TickScheduler.schedule(cartDelay + fireDelay + shootDelay, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (!isSequenceRunning(mc)) return;
                fireCrossbow(mc, hand);
            });

            TickScheduler.schedule(cartDelay + fireDelay + shootDelay + 1, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.getInventory().setSelectedSlot(sequenceSlot);
                }
                sequenceActive = false;
            });
        }
    }

    private boolean isSequenceRunning(Minecraft mc) {
        return isEnabled() && sequenceActive
                && mc.player != null && mc.level != null && mc.gameMode != null;
    }

    /** Restores the pre-sequence slot and stops the sequence without firing. */
    private void abortSequence(Minecraft mc) {
        if (mc.player != null) {
            mc.player.getInventory().setSelectedSlot(sequenceSlot);
        }
        sequenceActive = false;
    }

    /** Programmatically fires a loaded crossbow from the hotbar. */
    private void fireCrossbow(Minecraft mc, InteractionHand hand) {
        LocalPlayer player = mc.player;

        if (player == null || mc.gameMode == null)
            return;

        int crossbowSlot = findChargedCrossbowSlot(player);
        if (crossbowSlot == -1)
            return;

        player.getInventory().setSelectedSlot(crossbowSlot);

        if (!CrossbowItem.isCharged(player.getItemInHand(hand)))
            return;

        suppressIntercept = true;
        try {
            player.swing(hand, SwingAnimation.DEFAULT, false);
            mc.gameMode.useItem(player, hand);
        } finally {
            suppressIntercept = false;
        }
    }

    private static int findChargedCrossbowSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.CROSSBOW) && CrossbowItem.isCharged(stack)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasValidCrossbow(Minecraft client) {
        return client.player.getMainHandItem().is(Items.CROSSBOW);
    }

    private static BlockPos predict(Minecraft client, Arrow arrow) {

        Vec3 pos = arrow.position();
        Vec3 velocity = arrow.getDeltaMovement();

        for (int i = 0; i < LOOKAHEAD_TICKS; i++) {

            Vec3 next = pos.add(velocity);

            BlockHitResult hit = arrow.level().clip(new ClipContext(
                    pos,
                    next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    arrow
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit.getBlockPos().relative(hit.getDirection());
            }

            pos = next;
        }

        return null;
    }

    /** Predicts the landing block of a crossbow arrow fired from the player's current aim. */
    private static BlockPos predictFromAim(Minecraft client) {

        LocalPlayer player = client.player;
        Vec3 pos = player.getEyePosition();
        Vec3 velocity = player.getLookAngle()
                .multiply(CROSSBOW_POWER, CROSSBOW_POWER, CROSSBOW_POWER);

        for (int i = 0; i < MAX_PREDICTION_TICKS; i++) {

            Vec3 next = pos.add(velocity);

            BlockHitResult hit = client.level.clip(new ClipContext(
                    pos,
                    next,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit.getBlockPos().relative(hit.getDirection());
            }

            pos = next;

            velocity = velocity.multiply(ARROW_DRAG, ARROW_DRAG, ARROW_DRAG);
            velocity = velocity.add(0.0, -ARROW_GRAVITY, 0.0);
        }

        return null;
    }

    private static boolean place(Minecraft client, Arrow arrow, BlockPos railPos) {

        if (railPos == null)
            return false;

        LocalPlayer player = client.player;

        int fireSlot = findFlintAndSteelSlot(player);
        int railSlot = findRailSlot(player);
        int cartSlot = findMinecartSlot(player);
        boolean hasCartInOffhand = player.getOffhandItem().is(Items.TNT_MINECART);

        if (fireSlot == -1
                || railSlot == -1
                || (cartSlot == -1 && !hasCartInOffhand))
            return false;

        BlockPos firePos = findFirePos(client, railPos, arrow.getDeltaMovement());

        if (firePos == null)
            return false;

        int previousSlot = player.getInventory().getSelectedSlot();

        player.getInventory().setSelectedSlot(fireSlot);
        placeFire(client, firePos, false, false);

        player.getInventory().setSelectedSlot(railSlot);
        placeRail(client, railPos, false, false);

        // Safety shield: place most blast resistant block between player and cart,
        // close to the player, never overwriting firePos.
        if (INSTANCE.safety.get()) {
            boolean shouldShield = !INSTANCE.onlyIfLethal.get() || wouldKill(client, railPos, arrow.getDeltaMovement());
            if (shouldShield) {
                BlockPos shieldPos = findShieldPos(client, railPos, firePos);
                if (shieldPos != null) {
                    // Use a temporary slot switch for shield; placeShield handles its own slot logic
                    // Save current slot to restore afterwards
                    int tmp = player.getInventory().getSelectedSlot();
                    boolean placed = placeShield(client, shieldPos, false, false);
                    // placeShield already restores if holdSlot=false, but we ensure we return to tmp
                    player.getInventory().setSelectedSlot(tmp);
                    // shield failure is non-fatal; continue to cart
                }
            }
        }

        if (cartSlot != -1) {
            player.getInventory().setSelectedSlot(cartSlot);
        }
        placeCart(client, railPos, false, false);

        player.getInventory().setSelectedSlot(previousSlot);

        return true;
    }

    private static boolean placeRail(Minecraft client, BlockPos placePos, boolean swingHand, boolean holdSlot) {

        if (placePos == null)
            return false;

        LocalPlayer player = client.player;

        int railSlot = findRailSlot(player);

        if (railSlot == -1)
            return false;

        int previousSlot = player.getInventory().getSelectedSlot();

        player.getInventory().setSelectedSlot(railSlot);

        if (swingHand)
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

        client.gameMode.useItemOn(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(placePos),
                        Direction.UP,
                        placePos.below(),
                        false
                )
        );

        if (!holdSlot)
            player.getInventory().setSelectedSlot(previousSlot);

        return true;
    }

    private static boolean placeCart(Minecraft client, BlockPos placePos, boolean swingHand, boolean holdSlot) {

        if (placePos == null)
            return false;

        LocalPlayer player = client.player;

        int cartSlot = findMinecartSlot(player);

        if (cartSlot != -1) {
            int previousSlot = player.getInventory().getSelectedSlot();

            player.getInventory().setSelectedSlot(cartSlot);

            if (swingHand)
                player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

            client.gameMode.useItemOn(
                    player,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(
                            Vec3.atCenterOf(placePos),
                            Direction.UP,
                            placePos,
                            false
                    )
            );

            if (!holdSlot)
                player.getInventory().setSelectedSlot(previousSlot);

            return true;
        }

        if (player.getOffhandItem().is(Items.TNT_MINECART)) {
            if (swingHand)
                player.swing(InteractionHand.OFF_HAND, SwingAnimation.DEFAULT, false);

            client.gameMode.useItemOn(
                    player,
                    InteractionHand.OFF_HAND,
                    new BlockHitResult(
                            Vec3.atCenterOf(placePos),
                            Direction.UP,
                            placePos,
                            false
                    )
            );

            return true;
        }

        return false;
    }

    private static boolean placeFire(Minecraft client, BlockPos firePos, boolean swingHand, boolean holdSlot) {

        if (firePos == null)
            return false;

        LocalPlayer player = client.player;

        int fireSlot = findFlintAndSteelSlot(player);

        if (fireSlot == -1)
            return false;

        int previousSlot = player.getInventory().getSelectedSlot();

        player.getInventory().setSelectedSlot(fireSlot);

        if (swingHand)
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

        BlockPos support = firePos.below();

        client.gameMode.useItemOn(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(support),
                        Direction.UP,
                        support,
                        false
                )
        );

        if (!holdSlot)
            player.getInventory().setSelectedSlot(previousSlot);

        return true;
    }

    /**
     * Places the most blast resistant block available in the hotbar at the given shield position.
     * Returns true if a block was placed.
     */
    private static boolean placeShield(Minecraft client, BlockPos shieldPos, boolean swingHand, boolean holdSlot) {
        if (shieldPos == null)
            return false;
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null)
            return false;

        int shieldSlot = findBestShieldSlot(player);
        if (shieldSlot == -1) {
            // Try offhand if it holds a placeable resistant block
            ItemStack off = player.getOffhandItem();
            if (!off.isEmpty()) {
                Block offBlock = Block.byItem(off.getItem());
                if (offBlock != Blocks.AIR && offBlock.getExplosionResistance() > 0) {
                    BlockHitResult hit = findSupport(client, shieldPos);
                    if (hit == null) return false;
                    if (swingHand) player.swing(InteractionHand.OFF_HAND, SwingAnimation.DEFAULT, false);
                    client.gameMode.useItemOn(player, InteractionHand.OFF_HAND, hit);
                    return true;
                }
            }
            return false;
        }

        int previousSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(shieldSlot);
        BlockHitResult hit = findSupport(client, shieldPos);
        if (hit == null) {
            if (!holdSlot) player.getInventory().setSelectedSlot(previousSlot);
            return false;
        }
        if (swingHand) player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        if (!holdSlot) player.getInventory().setSelectedSlot(previousSlot);
        return true;
    }

    private static BlockPos findFirePos(Minecraft client, BlockPos railPos, Vec3 velocity) {

        Direction back = Direction.getApproximateNearest(velocity.x, velocity.y, velocity.z).getOpposite();

        if (back.getAxis().isHorizontal()) {
            BlockPos candidate = railPos.relative(back);
            if (canPlaceFire(client, candidate)) {
                return candidate;
            }
        }

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = railPos.relative(direction);
            if (canPlaceFire(client, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean canPlaceFire(Minecraft client, BlockPos pos) {
        return BaseFireBlock.canBePlacedAt(client.level, pos, Direction.NORTH);
    }

    // ==================== Safety / Shield helpers ====================

    /**
     * Finds the best shield position: closest to the player but still between player and cart,
     * never overwriting firePos or railPos. Checks for replaceable air and a solid support.
     */
    private static BlockPos findShieldPos(Minecraft client, BlockPos railPos, BlockPos firePos) {
        if (client.player == null || client.level == null || railPos == null)
            return null;
        LocalPlayer player = client.player;
        Vec3 cartCenter = Vec3.atCenterOf(railPos);
        double px = player.getX();
        double pz = player.getZ();
        double dx = px - cartCenter.x;
        double dz = pz - cartCenter.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        if (horizDist < 2.0) return null;
        double ux = dx / horizDist;
        double uz = dz / horizDist;
        BlockPos playerPos = player.blockPosition();
        double range = player.blockInteractionRange() + 2.0;
        double rangeSq = range * range;

        // Prefer positions nearest the player (1 block away, then 2, etc.)
        for (double distFromPlayer = 1.0; distFromPlayer < horizDist - 0.5; distFromPlayer += 1.0) {
            double cx = px - ux * distFromPlayer;
            double cz = pz - uz * distFromPlayer;
            BlockPos candidate = BlockPos.containing(cx, railPos.getY(), cz);
            if (candidate.equals(railPos) || candidate.equals(firePos) || candidate.equals(playerPos)) continue;
            BlockState state = client.level.getBlockState(candidate);
            if (!state.isAir() && !state.canBeReplaced()) continue;
            if (findSupport(client, candidate) == null) continue;
            if (player.distanceToSqr(Vec3.atCenterOf(candidate)) > rangeSq) continue;
            return candidate;
        }
        // Try alternative Y levels if rail Y is blocked
        for (double distFromPlayer = 1.0; distFromPlayer < horizDist - 0.5; distFromPlayer += 1.0) {
            double cx = px - ux * distFromPlayer;
            double cz = pz - uz * distFromPlayer;
            for (int yOff = -1; yOff <= 1; yOff++) {
                if (yOff == 0) continue;
                BlockPos candidate = BlockPos.containing(cx, railPos.getY() + yOff, cz);
                if (candidate.equals(railPos) || candidate.equals(firePos) || candidate.equals(playerPos)) continue;
                BlockState state = client.level.getBlockState(candidate);
                if (!state.isAir() && !state.canBeReplaced()) continue;
                if (findSupport(client, candidate) == null) continue;
                if (player.distanceToSqr(Vec3.atCenterOf(candidate)) > rangeSq) continue;
                return candidate;
            }
        }
        return null;
    }

    private static BlockHitResult findSupport(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()) return null;
        for (Direction dir : Direction.values()) {
            BlockPos supportPos = pos.relative(dir);
            BlockState support = client.level.getBlockState(supportPos);
            if (support.isAir() || !support.getFluidState().isEmpty()) continue;
            return new BlockHitResult(Vec3.atCenterOf(supportPos), dir.getOpposite(), supportPos, false);
        }
        return null;
    }

    private static int findBestShieldSlot(Player player) {
        int bestSlot = -1;
        float bestRes = -1.0F;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            if (isRail(stack)) continue;
            if (stack.is(Items.TNT_MINECART) || stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.CROSSBOW)) continue;
            Block block = Block.byItem(stack.getItem());
            if (block == Blocks.AIR) continue;
            float res = block.getExplosionResistance();
            // Skip blocks with negligible resistance (e.g., air is 0, but we already filtered)
            // Prefer higher resistance
            if (res > bestRes) {
                bestRes = res;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    // ==================== Accurate damage simulation ====================

    /**
     * Returns true if the TNT minecart at railPos would kill the player from explosion damage.
     * Uses optional accurate simulation with exposure raytracing and armor reduction.
     */
    private static boolean wouldKill(Minecraft client, BlockPos railPos, Vec3 velocity) {
        if (client.player == null || client.level == null || railPos == null) return false;
        LocalPlayer player = client.player;
        float radius = getCartExplosionRadius(velocity);
        Vec3 center = Vec3.atCenterOf(railPos);
        // Center a bit higher for minecart (entity height). Use +0.5y to approximate cart center.
        center = new Vec3(center.x, center.y + 0.5, center.z);
        boolean accurate = INSTANCE.accurateDamage.get();
        float damage = accurate ? calculateAccurateDamage(client, center, player, radius) : calculateSimpleDamage(center, player, radius);
        float health = player.getHealth() + player.getAbsorptionAmount();
        return damage >= health;
    }

    private static float getCartExplosionRadius(Vec3 velocity) {
        if (velocity == null) return 4.0F;
        double speed = velocity.length();
        double capped = Math.min(speed, 5.0);
        // Max radius: base 4 + random*1.5*capped, use max for lethal check (conservative)
        return 4.0F + (float)(1.5 * capped);
    }

    private static float calculateSimpleDamage(Vec3 center, LivingEntity target, float radius) {
        double dist = Math.sqrt(target.distanceToSqr(center));
        float diameter = radius * 2.0F;
        if (dist > diameter) return 0.0F;
        double factor = (1.0 - dist / diameter);
        return (float)((factor * factor + factor) / 2.0 * 7.0 * diameter + 1.0);
    }

    private static float calculateAccurateDamage(Minecraft client, Vec3 center, LivingEntity target, float radius) {
        double dist = Math.sqrt(target.distanceToSqr(center));
        float diameter = radius * 2.0F;
        if (dist > diameter) return 0.0F;
        float exposure = getSeenPercent(center, target);
        double factor = (1.0 - dist / diameter) * exposure;
        float base = (float)((factor * factor + factor) / 2.0 * 7.0 * diameter + 1.0);
        if (base <= 0) return 0;

        // Armor reduction
        float armor = target.getArmorValue();
        float toughness = 0.0F;
        try {
            toughness = (float) target.getAttributes().getValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        } catch (Exception ignored) {}

        var damageSource = client.level != null ? client.level.damageSources().explosion(null) : target.damageSources().explosion(null);
        // Fallback generic if explosion source is null
        if (damageSource == null) damageSource = target.damageSources().generic();

        float afterArmor = CombatRules.getDamageAfterAbsorb(target, base, damageSource, armor, toughness);

        // Enchantment protection (blast protection weighted)
        float prot = getProtectionAmount(client, target);
        float afterMagic = CombatRules.getDamageAfterMagicAbsorb(afterArmor, prot);

        // Resistance effect
        var resEffect = target.getEffect(MobEffects.RESISTANCE);
        if (resEffect != null) {
            int amp = resEffect.getAmplifier();
            afterMagic *= (1.0F - 0.2F * (amp + 1));
        }
        return Math.max(0, afterMagic);
    }

    private static float getProtectionAmount(Minecraft client, LivingEntity target) {
        if (client.level == null) return 0;
        try {
            var registry = client.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var protHolder = registry.getOrThrow(Enchantments.PROTECTION);
            var blastHolder = registry.getOrThrow(Enchantments.BLAST_PROTECTION);
            float total = 0;
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                if (!slot.isArmor()) continue;
                ItemStack armorStack = target.getItemBySlot(slot);
                if (armorStack.isEmpty()) continue;
                int protLvl = armorStack.getEnchantments().getLevel(protHolder);
                int blastLvl = armorStack.getEnchantments().getLevel(blastHolder);
                total += protLvl + blastLvl * 2.0F;
            }
            return Mth.clamp(total, 0.0F, 20.0F);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Vanilla exposure calculation: fraction of entity bounding box points with line-of-sight to explosion.
     * Mirrors {@link net.minecraft.world.level.ServerExplosion#getSeenPercent}.
     */
    private static float getSeenPercent(Vec3 explosionPos, Entity entity) {
        AABB box = entity.getBoundingBox();
        double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double h = (1.0 - Math.floor(1.0 / f) * f) / 2.0;
        if (d < 0 || e < 0 || f < 0) return 0.0F;
        int seen = 0;
        int total = 0;
        for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
                for (double m = 0.0; m <= 1.0; m += f) {
                    double n = Mth.lerp(k, box.minX, box.maxX);
                    double o = Mth.lerp(l, box.minY, box.maxY);
                    double p = Mth.lerp(m, box.minZ, box.maxZ);
                    Vec3 point = new Vec3(n + g, o, p + h);
                    BlockHitResult hit = entity.level().clip(new ClipContext(point, explosionPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
                    if (hit.getType() == HitResult.Type.MISS) seen++;
                    total++;
                }
            }
        }
        return (float) seen / (float) total;
    }

    private static boolean isRail(ItemStack stack) {
        return stack.is(Items.RAIL)
                || stack.is(Items.POWERED_RAIL)
                || stack.is(Items.DETECTOR_RAIL)
                || stack.is(Items.ACTIVATOR_RAIL);
    }

    private static int findFlintAndSteelSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.FLINT_AND_STEEL)) {
                return i;
            }
        }
        return -1;
    }

    private static int findRailSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            if (isRail(player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int findMinecartSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.TNT_MINECART)) {
                return i;
            }
        }
        return -1;
    }

    private void cleanup(Minecraft client) {

        predictions.entrySet().removeIf(entry -> {
            Entity entity = client.level.getEntity(entry.getKey());
            return !(entity instanceof Arrow);
        });

        processed.removeIf(id -> {
            Entity entity = client.level.getEntity(id);
            return !(entity instanceof Arrow);
        });
    }

    @Override
    protected void onDisable() {
        predictions.clear();
        processed.clear();
        sequenceActive = false;
        suppressIntercept = false;
    }
}

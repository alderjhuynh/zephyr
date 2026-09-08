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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
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
    }

    /** When enabled, cancels the crossbow fire and places rail, cart, then fire first. */
    public final BooleanSetting legit = new BooleanSetting("Legit", false);

    /** Base ticks between each placement step in legit mode. */
    public final NumberSetting placementDelay = new NumberSetting("Placement Delay", 2.0, 0.0, 10.0, 1.0);

    /** Max random extra ticks added to each legit placement step for humanlike jitter. */
    public final NumberSetting jitter = new NumberSetting("Jitter", 2.0, 0.0, 6.0, 1.0);

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
        startSequence(player, hand, railPos, firePos);
    }

    /** Kept for compatibility; legit mode is now triggered by rail placement. */
    public boolean onUseItemFireAttempt(Player player, InteractionHand hand) {
        return false;
    }

    /** Schedules the cart, fire, and crossbow steps across ticks with random jitter.
     *  The rail is assumed to have been placed by the player and is not scheduled. */
    private void startSequence(Player player, InteractionHand hand,
                               BlockPos railPos, BlockPos firePos) {
        sequenceSlot = player.getInventory().getSelectedSlot();
        int step = placementDelay.get().intValue();
        int jit = jitter.get().intValue();

        int cartDelay = 1 + random.nextInt(jit + 1);
        int fireDelay = step + random.nextInt(jit + 1);
        int shootDelay = step + random.nextInt(jit + 1);

        TickScheduler.schedule(cartDelay, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (!isSequenceRunning(mc)) return;
            if (!placeCart(mc, railPos, true, true)) abortSequence(mc);
        });

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
            player.swing(hand);
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
            player.swing(InteractionHand.MAIN_HAND);

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
                player.swing(InteractionHand.MAIN_HAND);

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
                player.swing(InteractionHand.OFF_HAND);

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
            player.swing(InteractionHand.MAIN_HAND);

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

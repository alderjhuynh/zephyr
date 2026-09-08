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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Automatically places a rail and TNT minecart to catch the local player's own flaming bow
 * arrows. Predicts each in-flight arrow's trajectory, then places a rail and minecart at the
 * predicted landing block so the arrow ignites the cart for a free explosion.
 */
public final class InstaCart extends Module {
    public static final InstaCart INSTANCE = new InstaCart();

    /** Number of ticks of trajectory simulation used to predict where an arrow will land. */
    private static final int LOOKAHEAD_TICKS = 8;

    /** Extended lookahead used in legit mode so the two-tick placement starts early enough. */
    private static final int LEGIT_LOOKAHEAD_TICKS = 12;

    private static final double ARROW_GRAVITY = 0.05;
    private static final double ARROW_DRAG = 0.99;

    private final Map<Integer, BlockPos> predictions = new HashMap<>();
    private final Set<Integer> processed = new HashSet<>();
    private final Set<Integer> cartPending = new HashSet<>();

    private InstaCart() {
        super("InstaCart", "Automatically places a rail and TNT minecart to catch your own flaming arrows", Category.COMBAT);
        addSetting(legit);
        addSetting(placementDelay);
    }

    /** When enabled, spreads the rail and cart placements over two separate ticks (vanilla-plausible). */
    public final BooleanSetting legit = new BooleanSetting("Legit", false);

    /** Ticks to wait between placing the rail and placing the minecart in legit mode. */
    public final NumberSetting placementDelay = new NumberSetting("Placement Delay", 1.0, 0.0, 20.0, 1.0);

    /** Tracks the predicted landing position for each in-flight arrow and places the rail + minecart. */
    @Override
    public void tick(Minecraft client) {
        if (client.player == null
                || client.level == null
                || client.gameMode == null) {
            return;
        }

        if (!hasValidBow(client))
            return;

        LocalPlayer player = client.player;
        double rangeSq = player.blockInteractionRange();
        rangeSq *= rangeSq;

        cleanup(client);

        int lookahead = legit.get() ? LEGIT_LOOKAHEAD_TICKS : LOOKAHEAD_TICKS;

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

            predictions.computeIfAbsent(id, ignored -> predict(client, arrow, lookahead));
        }

        Iterator<Map.Entry<Integer, BlockPos>> iterator = predictions.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<Integer, BlockPos> entry = iterator.next();

            Entity entity = client.level.getEntity(entry.getKey());

            if (!(entity instanceof Arrow arrow)) {
                cartPending.remove(entry.getKey());
                iterator.remove();
                continue;
            }

            if (((AbstractArrowInvoker) arrow).zephyr$isInGround()) {
                cartPending.remove(entry.getKey());
                iterator.remove();
                processed.add(arrow.getId());
                continue;
            }

            int id = arrow.getId();

            if (legit.get()) {

                if (!cartPending.contains(id)) {
                    int originalSlot = player.getInventory().getSelectedSlot();
                    if (placeRail(client, entry.getValue(), true, true)) {
                        cartPending.add(id);
                        scheduleCart(id, entry.getValue(), originalSlot);
                    }
                }

            } else if (place(client, entry.getValue())) {
                processed.add(arrow.getId());
                iterator.remove();
            }
        }
    }

    private static boolean hasValidBow(Minecraft client) {
        ItemStack stack = client.player.getMainHandItem();

        if (!stack.is(Items.BOW))
            return false;

        var flame = client.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FLAME);

        return stack.getEnchantments().getLevel(flame) >= 1;
    }

    private static BlockPos predict(Minecraft client, Arrow arrow, int lookaheadTicks) {

        Vec3 pos = arrow.position();
        Vec3 vel = arrow.getDeltaMovement();

        for (int i = 0; i < lookaheadTicks; i++) {

            Vec3 next = pos.add(vel);

            BlockHitResult hit = client.level.clip(new ClipContext(
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

            vel = vel.multiply(ARROW_DRAG, ARROW_DRAG, ARROW_DRAG);
            vel = vel.add(0.0, -ARROW_GRAVITY, 0.0);
        }

        return null;
    }

    private static boolean place(Minecraft client, BlockPos placePos) {
        return placeRail(client, placePos, false, false) && placeCart(client, placePos, false, false);
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

    private void scheduleCart(int id, BlockPos placePos, int originalSlot) {
        int delay = placementDelay.get().intValue();

        TickScheduler.schedule(delay, () -> {
            if (!isEnabled())
                return;

            Minecraft client = Minecraft.getInstance();

            if (client.player == null
                    || client.level == null
                    || client.gameMode == null)
                return;

            Entity entity = client.level.getEntity(id);

            if (!(entity instanceof Arrow arrow))
                return;

            if (((AbstractArrowInvoker) arrow).zephyr$isInGround())
                return;

            if (placeCart(client, placePos, true, true)) {
                processed.add(id);
                cartPending.remove(id);
                predictions.remove(id);
            }
        });

        TickScheduler.schedule(delay * 2, () -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null)
                return;
            client.player.getInventory().setSelectedSlot(originalSlot);
        });
    }

    private static boolean isRail(ItemStack stack) {
        return stack.is(Items.RAIL)
                || stack.is(Items.POWERED_RAIL)
                || stack.is(Items.DETECTOR_RAIL)
                || stack.is(Items.ACTIVATOR_RAIL);
    }

    private static int findRailSlot(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            if (isRail(player.getInventory().getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int findMinecartSlot(LocalPlayer player) {
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

        cartPending.removeIf(id -> {
            Entity entity = client.level.getEntity(id);
            return !(entity instanceof Arrow);
        });
    }

    @Override
    protected void onDisable() {
        predictions.clear();
        processed.clear();
        cartPending.clear();
    }
}

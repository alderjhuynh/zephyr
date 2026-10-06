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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
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
 * Automatically places a respawn anchor, charges it with glowstone, shields behind
 * glowstone, and detonates it to catch the local player's own crossbow arrows:
 * the arrow's trajectory is predicted, an anchor is placed at the landing block,
 * charged, shielded, and detonated.
 *
 * <p>With the {@code Legit} setting enabled, the module instead intercepts the player's
 * anchor placement, spreads the charge, shield, and detonate steps across separate
 * ticks with randomized humanlike jitter. Sequence: place respawn anchor (detected,
 * not scheduled) -&gt; interact while holding glowstone -&gt; place shield (glowstone)
 * -&gt; interact without holding glowstone to detonate.</p>
 */
public final class AnchorHelper extends Module {
    public static final AnchorHelper INSTANCE = new AnchorHelper();

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

    /** The selected slot held before the legit sequence started; restored on abort/finish. */
    private int sequenceSlot;

    private AnchorHelper() {
        super("AnchorHelper", "Places a respawn anchor, charges it with glowstone, shields with glowstone, and detonates to catch your own crossbow arrows", Category.COMBAT, false, false, true);
        addSetting(legit);
        addSetting(placementDelay);
        addSetting(jitter);
        addSetting(safety);
        addSetting(accurateDamage);
        addSetting(onlyIfLethal);
    }

    /** When enabled, reacts to manual anchor placement and spreads charge, shield, then detonate across ticks. */
    public final BooleanSetting legit = new BooleanSetting("Legit", false);

    /** Base ticks between each placement step in legit mode. */
    public final NumberSetting placementDelay = new NumberSetting("Placement Delay", 2.0, 0.0, 10.0, 1.0);

    /** Max random extra ticks added to each legit placement step for humanlike jitter. */
    public final NumberSetting jitter = new NumberSetting("Jitter", 2.0, 0.0, 6.0, 1.0);

    /** When enabled, places a glowstone block between the player and the anchor as a shield. */
    public final BooleanSetting safety = new BooleanSetting("Safety", false);

    /** When enabled, uses exposure-accurate explosion damage (raytraces + armor reduction) for lethal checks. */
    public final BooleanSetting accurateDamage = new BooleanSetting("Accurate Damage", false);

    /** When enabled with Safety, only places the shield if the anchor explosion would kill the player. */
    public final BooleanSetting onlyIfLethal = new BooleanSetting("Only If Lethal", false);

    /** Tracks the predicted landing position for each in-flight arrow and places/charges/shields/detonates the anchor. */
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
     * places a respawn anchor while legit mode is active. Schedules the remaining
     * charge, shield, and detonate steps. The anchor placement itself is not
     * cancelled.
     */
    public void onAnchorPlace(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
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
        if (!stack.is(Items.RESPAWN_ANCHOR))
            return;

        BlockPos anchorPos = hit.getBlockPos().relative(hit.getDirection());
        if (anchorPos == null)
            return;

        Vec3 velocity = player.getLookAngle()
                .multiply(CROSSBOW_POWER, CROSSBOW_POWER, CROSSBOW_POWER);

        int glowSlot = findGlowstoneSlot(player, 1);
        if (glowSlot == -1)
            return;

        if (findDetonateSlot(player, glowSlot) == -1)
            return;

        sequenceActive = true;
        startSequence(player, hand, anchorPos, velocity);
    }

    /** Kept for compatibility; legit mode is now triggered by anchor placement. */
    public boolean onUseItemFireAttempt(Player player, InteractionHand hand) {
        return false;
    }

    /** Schedules the charge, shield, and detonate steps across ticks with random jitter.
     *  The anchor is assumed to have been placed by the player and is not scheduled. */
    private void startSequence(Player player, InteractionHand hand,
                               BlockPos anchorPos, Vec3 velocity) {
        sequenceSlot = player.getInventory().getSelectedSlot();
        int step = placementDelay.get().intValue();
        int jit = jitter.get().intValue();

        int chargeDelay = 1 + random.nextInt(jit + 1);
        int shieldDelay = step + random.nextInt(jit + 1);
        int detonateDelay = step + random.nextInt(jit + 1);

        // Determine if we need a shield in legit sequence. Evaluate at schedule time.
        // The shield block is always glowstone, so require a second glowstone when shielding.
        BlockPos shieldPos = null;
        boolean doShield = false;
        if (safety.get()) {
            boolean lethalCheck = !onlyIfLethal.get() || wouldKill(Minecraft.getInstance(), anchorPos, velocity);
            if (lethalCheck) {
                BlockPos candidate = findShieldPos(Minecraft.getInstance(), anchorPos);
                int glowForShield = findGlowstoneSlot(player, 2);
                if (candidate != null && glowForShield != -1) {
                    shieldPos = candidate;
                    doShield = true;
                }
            }
        }

        TickScheduler.schedule(chargeDelay, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (!isSequenceRunning(mc)) return;
            if (!chargeAnchor(mc, anchorPos, true, true)) abortSequence(mc);
        });

        if (doShield) {
            BlockPos finalShieldPos = shieldPos;
            TickScheduler.schedule(chargeDelay + shieldDelay, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (!isSequenceRunning(mc)) return;
                // Place shield; failure does not abort the whole sequence (shield is best-effort).
                placeShield(mc, finalShieldPos, true, true);
            });
            TickScheduler.schedule(chargeDelay + shieldDelay + detonateDelay, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (!isSequenceRunning(mc)) return;
                if (!detonateAnchor(mc, anchorPos, true, true)) abortSequence(mc);
            });
            TickScheduler.schedule(chargeDelay + shieldDelay + detonateDelay + 1, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.getInventory().setSelectedSlot(sequenceSlot);
                }
                sequenceActive = false;
            });
        } else {
            TickScheduler.schedule(chargeDelay + detonateDelay, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (!isSequenceRunning(mc)) return;
                if (!detonateAnchor(mc, anchorPos, true, true)) abortSequence(mc);
            });

            TickScheduler.schedule(chargeDelay + detonateDelay + 1, () -> {
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

    /** Restores the pre-sequence slot and stops the sequence without detonating. */
    private void abortSequence(Minecraft mc) {
        if (mc.player != null) {
            mc.player.getInventory().setSelectedSlot(sequenceSlot);
        }
        sequenceActive = false;
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

    private static boolean place(Minecraft client, Arrow arrow, BlockPos anchorPos) {

        if (anchorPos == null)
            return false;

        LocalPlayer player = client.player;

        int anchorSlot = findAnchorSlot(player);
        int glowSlot = findGlowstoneSlot(player, 1);

        if (anchorSlot == -1 || glowSlot == -1)
            return false;

        if (findDetonateSlot(player, glowSlot) == -1)
            return false;

        int previousSlot = player.getInventory().getSelectedSlot();

        player.getInventory().setSelectedSlot(anchorSlot);
        if (!placeAnchor(client, anchorPos, false, false)) {
            player.getInventory().setSelectedSlot(previousSlot);
            return false;
        }

        if (!chargeAnchor(client, anchorPos, false, false)) {
            player.getInventory().setSelectedSlot(previousSlot);
            return false;
        }

        // Safety shield: place a glowstone block between player and anchor,
        // never overwriting the anchor itself.
        if (INSTANCE.safety.get()) {
            boolean shouldShield = !INSTANCE.onlyIfLethal.get() || wouldKill(client, anchorPos, arrow.getDeltaMovement());
            if (shouldShield) {
                BlockPos shieldPos = findShieldPos(client, anchorPos);
                if (shieldPos != null) {
                    int tmp = player.getInventory().getSelectedSlot();
                    placeShield(client, shieldPos, false, false);
                    // placeShield already restores if holdSlot=false, but we ensure we return to tmp
                    player.getInventory().setSelectedSlot(tmp);
                    // shield failure is non-fatal; continue to detonate
                }
            }
        }

        detonateAnchor(client, anchorPos, false, false);

        player.getInventory().setSelectedSlot(previousSlot);

        return true;
    }

    private static boolean placeAnchor(Minecraft client, BlockPos placePos, boolean swingHand, boolean holdSlot) {

        if (placePos == null)
            return false;

        LocalPlayer player = client.player;

        int anchorSlot = findAnchorSlot(player);

        if (anchorSlot == -1)
            return false;

        BlockHitResult hit = findSupport(client, placePos);
        if (hit == null)
            return false;

        int previousSlot = player.getInventory().getSelectedSlot();

        player.getInventory().setSelectedSlot(anchorSlot);

        if (swingHand)
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);

        if (!holdSlot)
            player.getInventory().setSelectedSlot(previousSlot);

        return true;
    }

    /** Interacts with the anchor while holding glowstone to charge it. */
    private static boolean chargeAnchor(Minecraft client, BlockPos anchorPos, boolean swingHand, boolean holdSlot) {

        if (anchorPos == null)
            return false;

        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null)
            return false;

        int glowSlot = findGlowstoneSlot(player, 1);

        if (glowSlot == -1)
            return false;

        int previousSlot = player.getInventory().getSelectedSlot();

        player.getInventory().setSelectedSlot(glowSlot);

        if (swingHand)
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

        client.gameMode.useItemOn(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(anchorPos),
                        Direction.UP,
                        anchorPos,
                        false
                )
        );

        if (!holdSlot)
            player.getInventory().setSelectedSlot(previousSlot);

        return true;
    }

    /** Interacts with the charged anchor without holding glowstone to detonate it. */
    private static boolean detonateAnchor(Minecraft client, BlockPos anchorPos, boolean swingHand, boolean holdSlot) {

        if (anchorPos == null)
            return false;

        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null)
            return false;

        int glowSlot = findGlowstoneSlot(player, 1);
        int detonateSlot = findDetonateSlot(player, glowSlot);

        if (detonateSlot == -1)
            return false;

        int previousSlot = player.getInventory().getSelectedSlot();

        player.getInventory().setSelectedSlot(detonateSlot);

        if (swingHand)
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);

        client.gameMode.useItemOn(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(anchorPos),
                        Direction.UP,
                        anchorPos,
                        false
                )
        );

        if (!holdSlot)
            player.getInventory().setSelectedSlot(previousSlot);

        return true;
    }

    /**
     * Places a glowstone block at the given shield position.
     * The shield block is always glowstone. Returns true if a block was placed.
     */
    private static boolean placeShield(Minecraft client, BlockPos shieldPos, boolean swingHand, boolean holdSlot) {
        if (shieldPos == null)
            return false;
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null)
            return false;

        int shieldSlot = findGlowstoneSlot(player, 1);
        if (shieldSlot == -1) {
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

    // ==================== Safety / Shield helpers ====================

    /**
     * Finds the best shield position: closest to the player but still between player and anchor,
     * never overwriting the anchor itself. Checks for replaceable air and a solid support.
     */
    private static BlockPos findShieldPos(Minecraft client, BlockPos anchorPos) {
        if (client.player == null || client.level == null || anchorPos == null)
            return null;
        LocalPlayer player = client.player;
        Vec3 anchorCenter = Vec3.atCenterOf(anchorPos);
        double px = player.getX();
        double pz = player.getZ();
        double dx = px - anchorCenter.x;
        double dz = pz - anchorCenter.z;
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
            BlockPos candidate = BlockPos.containing(cx, anchorPos.getY(), cz);
            if (candidate.equals(anchorPos) || candidate.equals(playerPos)) continue;
            BlockState state = client.level.getBlockState(candidate);
            if (!state.isAir() && !state.canBeReplaced()) continue;
            if (findSupport(client, candidate) == null) continue;
            if (player.distanceToSqr(Vec3.atCenterOf(candidate)) > rangeSq) continue;
            return candidate;
        }
        // Try alternative Y levels if anchor Y is blocked
        for (double distFromPlayer = 1.0; distFromPlayer < horizDist - 0.5; distFromPlayer += 1.0) {
            double cx = px - ux * distFromPlayer;
            double cz = pz - uz * distFromPlayer;
            for (int yOff = -1; yOff <= 1; yOff++) {
                if (yOff == 0) continue;
                BlockPos candidate = BlockPos.containing(cx, anchorPos.getY() + yOff, cz);
                if (candidate.equals(anchorPos) || candidate.equals(playerPos)) continue;
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

    // ==================== Accurate damage simulation ====================

    /**
     * Returns true if the respawn anchor at anchorPos would kill the player from explosion damage.
     * Uses optional accurate simulation with exposure raytracing and armor reduction.
     */
    private static boolean wouldKill(Minecraft client, BlockPos anchorPos, Vec3 velocity) {
        if (client.player == null || client.level == null || anchorPos == null) return false;
        LocalPlayer player = client.player;
        float radius = getAnchorExplosionRadius();
        Vec3 center = Vec3.atCenterOf(anchorPos);
        center = new Vec3(center.x, center.y + 0.5, center.z);
        boolean accurate = INSTANCE.accurateDamage.get();
        float damage = accurate ? calculateAccurateDamage(client, center, player, radius) : calculateSimpleDamage(center, player, radius);
        float health = player.getHealth() + player.getAbsorptionAmount();
        return damage >= health;
    }

    private static float getAnchorExplosionRadius() {
        return 5.0F;
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

    private static int findAnchorSlot(Player player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.RESPAWN_ANCHOR)) {
                return i;
            }
        }
        return -1;
    }

    private static int findGlowstoneSlot(Player player, int minCount) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.GLOWSTONE) && stack.getCount() >= minCount) {
                return i;
            }
        }
        return -1;
    }

    /** Finds a hotbar slot that does not hold glowstone for detonating (empty preferred). */
    private static int findDetonateSlot(Player player, int glowSlot) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).isEmpty()) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (i == glowSlot) continue;
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(Items.GLOWSTONE)) return i;
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
    }
}

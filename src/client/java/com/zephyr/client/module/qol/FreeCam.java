package com.zephyr.client.module.qol;

import com.zephyr.client.configplusgui.module.Category;
import com.zephyr.client.configplusgui.module.Module;
import com.zephyr.client.configplusgui.setting.BooleanSetting;
import com.zephyr.client.configplusgui.setting.EnumSetting;
import com.zephyr.client.configplusgui.setting.NumberSetting;
import com.zephyr.client.module.qol.freecam.FlightMode;
import com.zephyr.client.module.qol.freecam.FreeCamera;
import com.zephyr.client.module.qol.freecam.InteractionMode;
import com.zephyr.client.module.qol.freecam.Perspective;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.level.block.Block;

public final class FreeCam extends Module {
    public static final FreeCam INSTANCE = new FreeCam();
    public static final Minecraft MC = Minecraft.getInstance();

    private final EnumSetting<FlightMode> flightMode = new EnumSetting<>("Flight Mode", FlightMode.DEFAULT);
    private final NumberSetting horizontalSpeed = new NumberSetting("Horizontal Speed", 1.0, 0.5, 30.0, 0.5);
    private final NumberSetting verticalSpeed = new NumberSetting("Vertical Speed", 1.0, 0.5, 30.0, 0.5);
    private final EnumSetting<Perspective> initialPerspective = new EnumSetting<>("Initial Perspective", Perspective.INSIDE);
    private final BooleanSetting showPlayer = new BooleanSetting("Show Player", true);
    private final BooleanSetting showHand = new BooleanSetting("Show Hand", false);
    private final BooleanSetting fullBright = new BooleanSetting("Full Bright", false);
    private final BooleanSetting showSubmersionFog = new BooleanSetting("Show Submersion Fog", false);
    private final BooleanSetting outlinePlayer = new BooleanSetting("Outline Player", false);
    private final BooleanSetting freezePlayer = new BooleanSetting("Freeze Player", false);
    private final BooleanSetting disableOnDamage = new BooleanSetting("Disable on Damage", true);
    private final BooleanSetting allowInteract = new BooleanSetting("Allow Interactions", false);
    private final EnumSetting<InteractionMode> interactionMode = new EnumSetting<>("Interaction Mode", InteractionMode.CAMERA);
    private final BooleanSetting ignoreCollision = new BooleanSetting("Ignore Collision", true);
    private final BooleanSetting checkInitialCollision = new BooleanSetting("Check Initial Collision", false);

    private boolean suppressPerspectiveGuard;
    private boolean disableNextTick;

    private FreeCamera freeCamera;
    private CameraType rememberedF5;

    private FreeCam() {
        super("FreeCam", "Detaches the camera to fly freely while your player stays in place", Category.QOL);
        addSetting(flightMode);
        addSetting(horizontalSpeed);
        addSetting(verticalSpeed);
        addSetting(initialPerspective);
        addSetting(showPlayer);
        addSetting(showHand);
        addSetting(fullBright);
        addSetting(showSubmersionFog);
        addSetting(outlinePlayer);
        addSetting(freezePlayer);
        addSetting(disableOnDamage);
        addSetting(allowInteract);
        addSetting(interactionMode);
        addSetting(ignoreCollision);
        addSetting(checkInitialCollision);
    }

    @Override
    protected void onEnable() {
        MC.smartCull = false;
        rememberedF5 = MC.options.getCameraType();
        // The Options mixin prevents perspective changes while enabled, so this flag
        // lets us force first-person (needed when freecam is toggled from third person).
        suppressPerspectiveGuard = true;
        try {
            if (MC.gameRenderer.mainCamera().isDetached()) {
                MC.options.setCameraType(CameraType.FIRST_PERSON);
            }
        } finally {
            suppressPerspectiveGuard = false;
        }
        createCamera();
    }

    @Override
    protected void onDisable() {
        MC.smartCull = true;
        if (freeCamera != null) {
            if (MC.player != null) {
                MC.setCameraEntity(MC.player);
            }
            freeCamera.despawn();
            freeCamera.input = new ClientInput();
            freeCamera = null;
        }
        if (MC.player != null) {
            MC.player.input = new KeyboardInput(MC.options);
        }
        if (rememberedF5 != null) {
            MC.options.setCameraType(rememberedF5);
            rememberedF5 = null;
        }
    }

    @Override
    public void tick(Minecraft client) {
        if (disableNextTick) {
            disableNextTick = false;
            setEnabled(false);
            return;
        }
        // Covers the case where freecam is enabled before a world (and therefore a
        // player) exists - the camera entity is created as soon as one is available.
        createCamera();
    }

    private void createCamera() {
        if (freeCamera != null || MC.player == null || MC.level == null) return;
        freeCamera = new FreeCamera(-420);
        freeCamera.copyPosition(MC.player);
        freeCamera.applyPerspective(initialPerspective.get(), shouldCheckInitialCollision());
        freeCamera.spawn();
        MC.setCameraEntity(freeCamera);
    }

    /** True while the camera entity exists and is being used. */
    public static boolean isActive() {
        return INSTANCE.isEnabled() && getFreeCamera() != null;
    }

    public static FreeCamera getFreeCamera() {
        return INSTANCE.freeCamera;
    }

    public static boolean isPlayerControlEnabled() {
        return false;
    }

    public static FlightMode getFlightMode() {
        return INSTANCE.flightMode.get();
    }

    public static double getHorizontalSpeed() {
        return INSTANCE.horizontalSpeed.get();
    }

    public static double getVerticalSpeed() {
        return INSTANCE.verticalSpeed.get();
    }

    public static Perspective getInitialPerspective() {
        return INSTANCE.initialPerspective.get();
    }

    public static boolean shouldShowPlayer() {
        return INSTANCE.showPlayer.get();
    }

    public static boolean shouldHidePlayer() {
        return !INSTANCE.showPlayer.get();
    }

    public static boolean shouldShowHand() {
        return INSTANCE.showHand.get();
    }

    public static boolean shouldHideHand() {
        return !INSTANCE.showHand.get();
    }

    public static boolean isFullBrightEnabled() {
        return INSTANCE.fullBright.get();
    }

    public static boolean shouldShowSubmersionFog() {
        return INSTANCE.showSubmersionFog.get();
    }

    public static boolean shouldHideSubmersionFog() {
        return !INSTANCE.showSubmersionFog.get();
    }

    public static boolean isOutlineEnabled() {
        return INSTANCE.outlinePlayer.get();
    }

    public static boolean shouldFreezePlayer() {
        return INSTANCE.freezePlayer.get();
    }

    public static boolean shouldDisableOnDamage() {
        return INSTANCE.disableOnDamage.get();
    }

    public static boolean shouldPreventInteractions() {
        return !INSTANCE.allowInteract.get();
    }

    public static boolean allowInteractionsFromPlayer() {
        return INSTANCE.allowInteract.get() && INSTANCE.interactionMode.get() == InteractionMode.PLAYER;
    }

    public static boolean allowInteractionsFromCamera() {
        return INSTANCE.allowInteract.get() && INSTANCE.interactionMode.get() == InteractionMode.CAMERA;
    }

    public static boolean ignoreCollision() {
        return INSTANCE.ignoreCollision.get();
    }

    public static boolean ignoreCollisionWith(Block block) {
        return INSTANCE.ignoreCollision.get();
    }

    public static boolean shouldCheckInitialCollision() {
        return INSTANCE.checkInitialCollision.get() || !INSTANCE.ignoreCollision.get();
    }

    /** True only while {@link #onEnable()} is forcing first-person. */
    public static boolean isSuppressingPerspectiveGuard() {
        return INSTANCE.suppressPerspectiveGuard;
    }

    public static void disableNextTick() {
        INSTANCE.disableNextTick = true;
    }

    /** Disables freecam if the player disconnects or respawns. */
    public static void onDisconnect() {
        if (INSTANCE.isEnabled()) {
            INSTANCE.setEnabled(false);
        }
    }
}

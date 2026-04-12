package com.zephyr.client.keybind;

import com.zephyr.client.ZephyrConfig;
import com.zephyr.client.disable.*;
import com.zephyr.client.gui.ZephyrKeybindsScreen;
import com.zephyr.client.gui.ZephyrMenuScreen;
import com.zephyr.client.module.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ZephyrKeybindManager {
    public static final int UNBOUND_KEY = GLFW.GLFW_KEY_UNKNOWN;

    private static final EnumMap<Action, InternalKeybind> KEYBINDS = new EnumMap<>(Action.class);

    private static int modifierKeyCode = GLFW.GLFW_KEY_L;

    static {
        for (Action action : Action.values()) {
            KEYBINDS.put(action, new InternalKeybind(action, action.defaultSpecificKeyCode));
        }
    }

    private ZephyrKeybindManager() {
    }

    public static void tick(MinecraftClient client) {
        if (client == null || client.getWindow() == null) {
            return;
        }

        long windowHandle = client.getWindow().getHandle();
        boolean modifierPressed = isKeyPressed(windowHandle, modifierKeyCode);

        for (InternalKeybind keybind : KEYBINDS.values()) {
            keybind.tick(client, windowHandle, modifierPressed);
        }
    }

    public static int getModifierKeyCode() {
        return modifierKeyCode;
    }

    public static void setModifierKeyCode(int keyCode) {
        modifierKeyCode = keyCode;
    }

    public static int getSpecificKeyCode(Action action) {
        return KEYBINDS.get(action).specificKeyCode;
    }

    public static void setSpecificKeyCode(Action action, int keyCode) {
        KEYBINDS.get(action).specificKeyCode = keyCode;
    }

    public static Map<String, Integer> getSpecificKeyCodes() {
        Map<String, Integer> keyCodes = new LinkedHashMap<>();

        for (Action action : Action.values()) {
            keyCodes.put(action.name(), getSpecificKeyCode(action));
        }

        return keyCodes;
    }

    public static void loadSpecificKeyCodes(Map<String, Integer> savedKeyCodes) {
        for (Action action : Action.values()) {
            setSpecificKeyCode(action, action.defaultSpecificKeyCode);
        }

        if (savedKeyCodes == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry : savedKeyCodes.entrySet()) {
            try {
                Action action = Action.valueOf(entry.getKey());
                setSpecificKeyCode(action, entry.getValue() == null ? UNBOUND_KEY : entry.getValue());
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public static Text getModifierButtonText() {
        return Text.literal("Modifier: " + getKeyName(modifierKeyCode));
    }

    public static Text getActionButtonText(Action action) {
        return Text.literal(action.displayName + ": " + getKeyName(modifierKeyCode) + " + " + getKeyName(getSpecificKeyCode(action)));
    }

    public static boolean isActionHeld(MinecraftClient client, Action action) {
        if (client == null || client.getWindow() == null) {
            return false;
        }

        long windowHandle = client.getWindow().getHandle();
        return isKeyPressed(windowHandle, modifierKeyCode) && isKeyPressed(windowHandle, getSpecificKeyCode(action));
    }

    private static boolean isKeyPressed(long windowHandle, int keyCode) {
        return keyCode != UNBOUND_KEY && InputUtil.isKeyPressed(windowHandle, keyCode);
    }

    private static String getKeyName(int keyCode) {
        if (keyCode == UNBOUND_KEY) {
            return "None";
        }

        return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
    }

    public enum Action {
        OPEN_MENU("Open Menu", GLFW.GLFW_KEY_ENTER),
        BLINK("Use Blink", GLFW.GLFW_KEY_B),
        AUTO_RESPAWN("AutoRespawn", UNBOUND_KEY),
        STEP("Step", UNBOUND_KEY),
        SPRINT("Sprint", UNBOUND_KEY),
        ANTI_HUNGER("AntiHunger", UNBOUND_KEY),
        ELYTRA_BOOST("Elytra Boost", UNBOUND_KEY),
        ELYTRA_SWAP("Elytra Swap", UNBOUND_KEY),
        NO_FALL("No Fall", UNBOUND_KEY),
        ITEM_RESTOCK("Item Restock", UNBOUND_KEY),
        DURABILITY_SWAP("Durability Swap", UNBOUND_KEY),
        HOTBAR_ROW_SWAP("Use Hotbar Row Swap", UNBOUND_KEY),
        TRIDENT_BOOST("Trident Boost", UNBOUND_KEY),
        BLINK_ENABLED("Blink Enabled", UNBOUND_KEY),
        HOTBAR_ROW_SWAP_ENABLED("Hotbar Row Swap", UNBOUND_KEY),
        AIR_JUMP("AirJump", UNBOUND_KEY),
        FLIGHT("Flight", UNBOUND_KEY),
        FREE_CAM("FreeCam", UNBOUND_KEY),
        F5_TWEAKS("F5 Tweaks", UNBOUND_KEY),
        GUI_MOVE("GUI Move", UNBOUND_KEY),
        CRITICALS("Criticals", UNBOUND_KEY),
        JESUS("Jesus", UNBOUND_KEY),
        AUTO_TOOL("AutoTool", UNBOUND_KEY),
        FAST_ATTACK("Fast Attack", UNBOUND_KEY),
        FAST_USE("Fast Use", UNBOUND_KEY),
        GHOST_HAND("GhostHand", UNBOUND_KEY),
        HOLD_ATTACK("Hold Attack", UNBOUND_KEY),
        HOLD_USE("Hold Use", UNBOUND_KEY),
        PERIODIC_ATTACK("Periodic Attack", UNBOUND_KEY),
        PERIODIC_USE("Periodic Use", UNBOUND_KEY),
        PICK_BEFORE_PLACE("Pick Before Place", UNBOUND_KEY),
        RENDER_INVISIBILITY("Render Invisibility", UNBOUND_KEY),
        SNEAK("Sneak", UNBOUND_KEY),
        PORTAL_GUI_CLOSING("Disable Portal GUI Closing", UNBOUND_KEY),
        DEAD_MOB_INTERACTION("Disable Dead Mob Interaction", UNBOUND_KEY),
        AXE_STRIPPING("Disable Axe Stripping", UNBOUND_KEY),
        SHOVEL_PATHING("Disable Shovel Pathing", UNBOUND_KEY),
        BREAK_COOLDOWN("Disable Block Break Cooldown", UNBOUND_KEY),
        BREAK_PARTICLES("Disable Block Break Particles", UNBOUND_KEY),
        INVENTORY_EFFECTS("Disable Inventory Effect Rendering", UNBOUND_KEY),
        NAUSEA_OVERLAY("Disable Nausea Overlay", UNBOUND_KEY),
        PORTAL_SOUND("Disable Portal Sound", UNBOUND_KEY),
        FOG("Disable Fog", UNBOUND_KEY),
        FIRST_PERSON_PARTICLES("Disable First Person Effect Particles", UNBOUND_KEY),
        RAIN("Disable Rain Effects", UNBOUND_KEY),
        DEAD_MOB_RENDERING("Disable Dead Mob Rendering", UNBOUND_KEY),
        PEARL_BOOST("Pearl Boost", UNBOUND_KEY),
        HIGH_JUMP("High Jump", UNBOUND_KEY),
        LONG_JUMP("Long Jump", UNBOUND_KEY),
        AERODYNAMICS("Aerodynamics", UNBOUND_KEY);

        private final String displayName;
        private final int defaultSpecificKeyCode;

        Action(String displayName, int defaultSpecificKeyCode) {
            this.displayName = displayName;
            this.defaultSpecificKeyCode = defaultSpecificKeyCode;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static final class InternalKeybind {
        private final Action action;
        private int specificKeyCode;
        private boolean chordHeld;

        private InternalKeybind(Action action, int specificKeyCode) {
            this.action = action;
            this.specificKeyCode = specificKeyCode;
        }

        private void tick(MinecraftClient client, long windowHandle, boolean modifierPressed) {
            boolean chordPressed = modifierPressed && isKeyPressed(windowHandle, specificKeyCode);

            if (chordPressed && !chordHeld) {
                trigger(client, action);
            }

            chordHeld = chordPressed;
        }
    }

    private static void trigger(MinecraftClient client, Action action) {
        Screen currentScreen = client.currentScreen;

        if (currentScreen instanceof ZephyrKeybindsScreen) {
            return;
        }

        switch (action) {
            case OPEN_MENU -> {
                if (currentScreen instanceof ZephyrMenuScreen) {
                    return;
                }

                client.setScreen(new ZephyrMenuScreen(currentScreen));
            }
            case BLINK -> {
                if (!Blink.CanUseKeybind) {
                    return;
                }

                if (currentScreen instanceof ZephyrMenuScreen) {
                    return;
                }

                Blink.toggle();
            }
            case AUTO_RESPAWN -> toggleAndSave(() -> AutoRespawn.enabled = !AutoRespawn.enabled);
            case STEP -> toggleAndSave(() -> Step.setEnabled(!Step.isEnabled()));
            case SPRINT -> toggleAndSave(() -> Sprint.enabled = !Sprint.enabled);
            case ANTI_HUNGER -> toggleAndSave(() -> AntiHunger.enabled = !AntiHunger.enabled);
            case ELYTRA_BOOST -> toggleAndSave(() -> ElytraBoost.enabled = !ElytraBoost.enabled);
            case ELYTRA_SWAP -> toggleAndSave(() -> ElytraSwap.enabled = !ElytraSwap.enabled);
            case NO_FALL -> toggleAndSave(() -> NoFall.enabled = !NoFall.enabled);
            case ITEM_RESTOCK -> toggleAndSave(() -> ItemRestock.enabled = !ItemRestock.enabled);
            case DURABILITY_SWAP -> toggleAndSave(() -> DurabilitySwap.enabled = !DurabilitySwap.enabled);
            case HOTBAR_ROW_SWAP -> {
            }
            case TRIDENT_BOOST -> toggleAndSave(() -> TridentBoost.enabled = !TridentBoost.enabled);
            case BLINK_ENABLED -> toggleAndSave(() -> Blink.CanUseKeybind = !Blink.CanUseKeybind);
            case HOTBAR_ROW_SWAP_ENABLED -> toggleAndSave(() -> HotbarRowSwap.enabled = !HotbarRowSwap.enabled);
            case AIR_JUMP -> toggleAndSave(() -> AirJump.setEnabled(!AirJump.enabled));
            case FLIGHT -> toggleAndSave(() -> Flight.setEnabled(!Flight.enabled));
            case FREE_CAM -> toggleAndSave(() -> FreeCam.setEnabled(!FreeCam.enabled));
            case F5_TWEAKS -> toggleAndSave(() -> F5Tweaks.enabled = !F5Tweaks.enabled);
            case GUI_MOVE -> toggleAndSave(() -> GuiMove.enabled = !GuiMove.enabled);
            case CRITICALS -> toggleAndSave(() -> Criticals.enabled = !Criticals.enabled);
            case JESUS -> toggleAndSave(() -> Jesus.enabled = !Jesus.enabled);
            case AUTO_TOOL -> toggleAndSave(() -> AutoTool.enabled = !AutoTool.enabled);
            case FAST_ATTACK -> toggleAndSave(() -> FastAttack.enabled = !FastAttack.enabled);
            case FAST_USE -> toggleAndSave(() -> FastUse.enabled = !FastUse.enabled);
            case GHOST_HAND -> toggleAndSave(() -> GhostHand.enabled = !GhostHand.enabled);
            case HOLD_ATTACK -> toggleAndSave(() -> HoldAttack.enabled = !HoldAttack.enabled);
            case HOLD_USE -> toggleAndSave(() -> HoldUse.enabled = !HoldUse.enabled);
            case PERIODIC_ATTACK -> toggleAndSave(() -> PeriodicAttack.enabled = !PeriodicAttack.enabled);
            case PERIODIC_USE -> toggleAndSave(() -> PeriodicUse.enabled = !PeriodicUse.enabled);
            case PICK_BEFORE_PLACE -> toggleAndSave(() -> PickBeforePlace.enabled = !PickBeforePlace.enabled);
            case RENDER_INVISIBILITY -> toggleAndSave(() -> RenderInvisibility.enabled = !RenderInvisibility.enabled);
            case SNEAK -> toggleAndSave(() -> Sneak.enabled = !Sneak.enabled);
            case PORTAL_GUI_CLOSING -> toggleAndSave(() -> disablePortalGuiClosing.enabled = !disablePortalGuiClosing.enabled);
            case DEAD_MOB_INTERACTION -> toggleAndSave(() -> disableDeadMobInteraction.enabled = !disableDeadMobInteraction.enabled);
            case AXE_STRIPPING -> toggleAndSave(() -> disableAxeStripping.enabled = !disableAxeStripping.enabled);
            case SHOVEL_PATHING -> toggleAndSave(() -> disableShovelPathing.enabled = !disableShovelPathing.enabled);
            case BREAK_COOLDOWN -> toggleAndSave(() -> disableBlockBreakingCooldown.enabled = !disableBlockBreakingCooldown.enabled);
            case BREAK_PARTICLES -> toggleAndSave(() -> disableBlockBreakingParticles.enabled = !disableBlockBreakingParticles.enabled);
            case INVENTORY_EFFECTS -> toggleAndSave(() -> disableInventoryEffectRendering.enabled = !disableInventoryEffectRendering.enabled);
            case NAUSEA_OVERLAY -> toggleAndSave(() -> disableNauseaOverlay.enabled = !disableNauseaOverlay.enabled);
            case PORTAL_SOUND -> toggleAndSave(() -> disableNetherPortalSound.enabled = !disableNetherPortalSound.enabled);
            case FOG -> toggleAndSave(() -> disableFogRendering.enabled = !disableFogRendering.enabled);
            case FIRST_PERSON_PARTICLES -> toggleAndSave(() -> disableFirstPersonEffectParticles.enabled = !disableFirstPersonEffectParticles.enabled);
            case RAIN -> toggleAndSave(() -> disableRainEffects.enabled = !disableRainEffects.enabled);
            case DEAD_MOB_RENDERING -> toggleAndSave(() -> disableDeadMobRendering.enabled = !disableDeadMobRendering.enabled);
            case PEARL_BOOST -> toggleAndSave(() -> PearlBoost.enabled = !PearlBoost.enabled);
            case HIGH_JUMP -> toggleAndSave(() -> HighJump.enabled = !HighJump.enabled);
            case LONG_JUMP -> toggleAndSave(() -> LongJump.setEnabled(!LongJump.enabled));
            case AERODYNAMICS -> toggleAndSave(() -> Aerodynamics.enabled = !Aerodynamics.enabled);
        }
    }

    private static void toggleAndSave(Runnable toggle) {
        toggle.run();
        ZephyrConfig.saveCurrentState();
    }
}

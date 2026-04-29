package com.zephyr.client.keybind;

import com.zephyr.client.ZephyrConfig;
import com.zephyr.client.disable.*;
import com.zephyr.client.gui.ZephyrKeybindsScreen;
import com.zephyr.client.gui.ZephyrMenuScreen;
import com.zephyr.client.module.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
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

        Window window = client.getWindow();
        boolean modifierPressed = isKeyPressed(window, modifierKeyCode);

        for (InternalKeybind keybind : KEYBINDS.values()) {
            keybind.tick(client, window, modifierPressed);
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

        Window window = client.getWindow();
        return isKeyPressed(window, modifierKeyCode) && isKeyPressed(window, getSpecificKeyCode(action));
    }

    private static boolean isKeyPressed(Window window, int keyCode) {
        return keyCode != UNBOUND_KEY && InputUtil.isKeyPressed(window, keyCode);
    }

    private static String getKeyName(int keyCode) {
        if (keyCode == UNBOUND_KEY) {
            return "None";
        }

        return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
    }

    public enum Action {
        OPEN_MENU("Open Menu", GLFW.GLFW_KEY_ENTER),
        BLINK("Use Blink", UNBOUND_KEY),
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
        SHIELD_BREAKER("Shield Breaker", UNBOUND_KEY),
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
        AERODYNAMICS("Aerodynamics", UNBOUND_KEY),
        BREACHSWAP("Breach Swap", UNBOUND_KEY),
        PEARLCATCH("Pearl Catch", UNBOUND_KEY);


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

        private void tick(MinecraftClient client, Window window, boolean modifierPressed) {
            boolean chordPressed = modifierPressed && isKeyPressed(window, specificKeyCode);

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
        
            case AUTO_RESPAWN -> toggleAndSave(action.getDisplayName(), () -> AutoRespawn.enabled = !AutoRespawn.enabled, () -> AutoRespawn.enabled);
            case STEP -> toggleAndSave(action.getDisplayName(), () -> Step.setEnabled(!Step.isEnabled()), () -> Step.enabled);
            case SPRINT -> toggleAndSave(action.getDisplayName(), () -> Sprint.enabled = !Sprint.enabled, () -> Sprint.enabled);
            case ANTI_HUNGER -> toggleAndSave(action.getDisplayName(), () -> AntiHunger.enabled = !AntiHunger.enabled, () -> AntiHunger.enabled);
            case ELYTRA_BOOST -> toggleAndSave(action.getDisplayName(), () -> ElytraBoost.enabled = !ElytraBoost.enabled, () -> ElytraBoost.enabled);
            case ELYTRA_SWAP -> toggleAndSave(action.getDisplayName(), () -> ElytraSwap.enabled = !ElytraSwap.enabled, () -> ElytraSwap.enabled);
            case NO_FALL -> toggleAndSave(action.getDisplayName(), () -> NoFall.enabled = !NoFall.enabled, () -> NoFall.enabled);
            case ITEM_RESTOCK -> toggleAndSave(action.getDisplayName(), () -> ItemRestock.enabled = !ItemRestock.enabled, () -> ItemRestock.enabled);
            case DURABILITY_SWAP -> toggleAndSave(action.getDisplayName(), () -> DurabilitySwap.enabled = !DurabilitySwap.enabled, () -> DurabilitySwap.enabled);
            case HOTBAR_ROW_SWAP -> {
            }
            case TRIDENT_BOOST -> toggleAndSave(action.getDisplayName(), () -> TridentBoost.enabled = !TridentBoost.enabled,() -> TridentBoost.enabled);
            case BLINK_ENABLED -> toggleAndSave(action.getDisplayName(), () -> Blink.CanUseKeybind = !Blink.CanUseKeybind, () -> Blink.CanUseKeybind);
            case HOTBAR_ROW_SWAP_ENABLED -> toggleAndSave(action.getDisplayName(), () -> HotbarRowSwap.enabled = !HotbarRowSwap.enabled, () -> HotbarRowSwap.enabled);
            case AIR_JUMP -> toggleAndSave(action.getDisplayName(), () -> AirJump.setEnabled(!AirJump.enabled), () -> AirJump.enabled);
            case FLIGHT -> toggleAndSave(action.getDisplayName(), () -> Flight.setEnabled(!Flight.enabled), () -> Flight.enabled);
            case FREE_CAM -> toggleAndSave(action.getDisplayName(), () -> FreeCam.setEnabled(!FreeCam.enabled), () -> FreeCam.enabled);
            case F5_TWEAKS -> toggleAndSave(action.getDisplayName(), () -> F5Tweaks.enabled = !F5Tweaks.enabled, () -> F5Tweaks.enabled);
            case GUI_MOVE -> toggleAndSave(action.getDisplayName(), () -> GuiMove.enabled = !GuiMove.enabled, () -> GuiMove.enabled);
            case CRITICALS -> toggleAndSave(action.getDisplayName(), () -> Criticals.enabled = !Criticals.enabled, () -> Criticals.enabled);
            case JESUS -> toggleAndSave(action.getDisplayName(), () -> Jesus.enabled = !Jesus.enabled, () -> Jesus.enabled);
            case AUTO_TOOL -> toggleAndSave(action.getDisplayName(), () -> AutoTool.enabled = !AutoTool.enabled, () -> AutoTool.enabled);
            case FAST_ATTACK -> toggleAndSave(action.getDisplayName(), () -> FastAttack.enabled = !FastAttack.enabled, () -> FastAttack.enabled);
            case SHIELD_BREAKER -> toggleAndSave(action.getDisplayName(), () -> ShieldBreaker.enabled = !ShieldBreaker.enabled, () -> ShieldBreaker.enabled);
            case FAST_USE -> toggleAndSave(action.getDisplayName(), () -> FastUse.enabled = !FastUse.enabled, () -> FastUse.enabled);
            case GHOST_HAND -> toggleAndSave(action.getDisplayName(), () -> GhostHand.enabled = !GhostHand.enabled, () -> GhostHand.enabled);
            case HOLD_ATTACK -> toggleAndSave(action.getDisplayName(), () -> HoldAttack.enabled = !HoldAttack.enabled, () -> HoldAttack.enabled);
            case HOLD_USE -> toggleAndSave(action.getDisplayName(), () -> HoldUse.enabled = !HoldUse.enabled, () -> HoldUse.enabled);
            case PERIODIC_ATTACK -> toggleAndSave(action.getDisplayName(), () -> PeriodicAttack.enabled = !PeriodicAttack.enabled, () -> PeriodicAttack.enabled);
            case PERIODIC_USE -> toggleAndSave(action.getDisplayName(), () -> PeriodicUse.enabled = !PeriodicUse.enabled, () -> PeriodicUse.enabled);
            case PICK_BEFORE_PLACE -> toggleAndSave(action.getDisplayName(), () -> PickBeforePlace.enabled = !PickBeforePlace.enabled, () -> PickBeforePlace.enabled);
            case RENDER_INVISIBILITY -> toggleAndSave(action.getDisplayName(), () -> RenderInvisibility.enabled = !RenderInvisibility.enabled, () -> RenderInvisibility.enabled);
            case SNEAK -> toggleAndSave(action.getDisplayName(), () -> Sneak.enabled = !Sneak.enabled, () -> Sneak.enabled);
            case PORTAL_GUI_CLOSING -> toggleAndSave(action.getDisplayName(), () -> disablePortalGuiClosing.enabled = !disablePortalGuiClosing.enabled, () -> disablePortalGuiClosing.enabled);
            case DEAD_MOB_INTERACTION -> toggleAndSave(action.getDisplayName(), () -> disableDeadMobInteraction.enabled = !disableDeadMobInteraction.enabled, () -> disableDeadMobInteraction.enabled);
            case AXE_STRIPPING -> toggleAndSave(action.getDisplayName(), () -> disableAxeStripping.enabled = !disableAxeStripping.enabled, () -> disableAxeStripping.enabled);
            case SHOVEL_PATHING -> toggleAndSave(action.getDisplayName(), () -> disableShovelPathing.enabled = !disableShovelPathing.enabled, () -> disableShovelPathing.enabled);
            case BREAK_COOLDOWN -> toggleAndSave(action.getDisplayName(), () -> disableBlockBreakingCooldown.enabled = !disableBlockBreakingCooldown.enabled, () -> disableBlockBreakingCooldown.enabled);
            case BREAK_PARTICLES -> toggleAndSave(action.getDisplayName(), () -> disableBlockBreakingParticles.enabled = !disableBlockBreakingParticles.enabled, () -> disableBlockBreakingParticles.enabled);
            case INVENTORY_EFFECTS -> toggleAndSave(action.getDisplayName(), () -> disableInventoryEffectRendering.enabled = !disableInventoryEffectRendering.enabled, () -> disableInventoryEffectRendering.enabled);
            case NAUSEA_OVERLAY -> toggleAndSave(action.getDisplayName(), () -> disableNauseaOverlay.enabled = !disableNauseaOverlay.enabled, () -> disableNauseaOverlay.enabled);
            case PORTAL_SOUND -> toggleAndSave(action.getDisplayName(), () -> disableNetherPortalSound.enabled = !disableNetherPortalSound.enabled, () -> disableNetherPortalSound.enabled);
            case FOG -> toggleAndSave(action.getDisplayName(), () -> disableFogRendering.enabled = !disableFogRendering.enabled, () -> disableFogRendering.enabled);
            case FIRST_PERSON_PARTICLES -> toggleAndSave(action.getDisplayName(), () -> disableFirstPersonEffectParticles.enabled = !disableFirstPersonEffectParticles.enabled, () -> disableFirstPersonEffectParticles.enabled);
            case RAIN -> toggleAndSave(action.getDisplayName(), () -> disableRainEffects.enabled = !disableRainEffects.enabled, () -> disableRainEffects.enabled);
            case DEAD_MOB_RENDERING -> toggleAndSave(action.getDisplayName(), () -> disableDeadMobRendering.enabled = !disableDeadMobRendering.enabled, () -> disableDeadMobRendering.enabled);
            case PEARL_BOOST -> toggleAndSave(action.getDisplayName(), () -> PearlBoost.enabled = !PearlBoost.enabled, () -> PearlBoost.enabled);
            case HIGH_JUMP -> toggleAndSave(action.getDisplayName(), () -> HighJump.enabled = !HighJump.enabled, () -> HighJump.enabled);
            case LONG_JUMP -> toggleAndSave(action.getDisplayName(), () -> LongJump.setEnabled(!LongJump.enabled), () -> LongJump.enabled);
            case AERODYNAMICS -> toggleAndSave(action.getDisplayName(), () -> Aerodynamics.enabled = !Aerodynamics.enabled, () -> Aerodynamics.enabled);
            case BREACHSWAP -> toggleAndSave(action.getDisplayName(), () -> BreachSwap.enabled = !BreachSwap.enabled, () -> BreachSwap.enabled);
            case PEARLCATCH -> toggleAndSave(action.getDisplayName(), () -> PearlCatch.enabled = !PearlCatch.enabled, () -> PearlCatch.enabled);
        }
    }

    private static void toggleAndSave(String name, Runnable toggle, java.util.function.Supplier<?> stateSupplier) {
        toggle.run();
        ZephyrConfig.saveCurrentState();

        Object state = stateSupplier != null ? stateSupplier.get() : "";
        sendOverlay(name, state);
}

    private static void sendOverlay(String name, Object state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String message;

        if (state instanceof Boolean b) {
            message = "Toggled " + name + " " + (b ? "§aON" : "§cOFF");
        } else if (state != null && !state.toString().isEmpty()) {
            message = "toggled " + name + " " + state.toString().toUpperCase();
        } else {
            message = "toggled " + name;
        }

        client.player.sendMessage(Text.literal(message), true);
    }
}

package com.zephyr.client.commands;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;

import java.util.List;
import java.util.Locale;

/**
 * Port of {@code clook}.
 */
public final class LookCommand extends Command {
    public static final LookCommand INSTANCE = new LookCommand();

    private LookCommand() {
        super("look", "Look at block/angles: .z look <block <x> <y> <z>|angles <yaw> <pitch>|cardinal <north|south|east|west|up|down>> (alias: clook)");
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) return List.of("block", "angles", "cardinal");
        if (args.length == 2 && args[0].equalsIgnoreCase("cardinal")) return List.of("up","down","north","south","east","west");
        return List.of();
    }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { CommandManager.sendMessage("You must be in a world"); return; }
        if (args.length == 0) { CommandManager.sendMessage("Usage: .z look <block <x> <y> <z>|angles <yaw> <pitch>|cardinal ...>"); return; }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "block" -> {
                if (args.length < 4) { CommandManager.sendMessage("Usage: .z look block <x> <y> <z>"); return; }
                try {
                    BlockPos pos = new BlockPos(Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
                    double dx = (pos.getX()+0.5)-mc.player.getX();
                    double dy = (pos.getY()+0.5)-(mc.player.getY()+mc.player.getEyeHeight());
                    double dz = (pos.getZ()+0.5)-mc.player.getZ();
                    double dh = Math.sqrt(dx*dx+dz*dz);
                    float yaw = (float)Math.toDegrees(Math.atan2(dz,dx))-90;
                    float pitch = (float)-Math.toDegrees(Math.atan2(dy,dh));
                    mc.player.moveOrInterpolateTo(mc.player.position(), yaw, pitch);
                    CommandManager.sendMessage("Looking at " + pos.toShortString());
                } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid coordinates"); }
            }
            case "angles" -> {
                if (args.length < 3) { CommandManager.sendMessage("Usage: .z look angles <yaw> <pitch>"); return; }
                try {
                    float yaw = Float.parseFloat(args[1]);
                    float pitch = Float.parseFloat(args[2]);
                    mc.player.moveOrInterpolateTo(mc.player.position(), yaw, pitch);
                    CommandManager.sendMessage("Look yaw=" + yaw + " pitch=" + pitch);
                } catch (NumberFormatException e) { CommandManager.sendMessage("Invalid angles"); }
            }
            case "cardinal" -> {
                if (args.length < 2) { CommandManager.sendMessage("Usage: .z look cardinal <north|south|east|west|up|down>"); return; }
                String dir = args[1].toLowerCase(Locale.ROOT);
                float yaw = mc.player.getYRot();
                float pitch = mc.player.getXRot();
                switch (dir) {
                    case "up" -> pitch = -90;
                    case "down" -> pitch = 90;
                    case "north" -> { yaw = 180; pitch = 0; }
                    case "south" -> { yaw = 0; pitch = 0; }
                    case "east" -> { yaw = -90; pitch = 0; }
                    case "west" -> { yaw = 90; pitch = 0; }
                    default -> { CommandManager.sendMessage("Unknown direction: " + dir); return; }
                }
                mc.player.moveOrInterpolateTo(mc.player.position(), yaw, pitch);
                CommandManager.sendMessage("Looking " + dir);
            }
            default -> CommandManager.sendMessage("Unknown subcommand: " + sub);
        }
    }
}

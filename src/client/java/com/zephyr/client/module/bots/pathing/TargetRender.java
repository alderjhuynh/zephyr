package com.zephyr.client.module.bots.pathing;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Backport of 26.3's TargetRender. 1.21.1 differences: HudRenderCallback
// instead of HudElementRegistry, GuiGraphics instead of GuiGraphicsExtractor,
// Camera comes from gameRenderer.getMainCamera(), and FOV via
// gameRenderer.getFov(camera, tickDelta, true) (Camera has no getFov here).
public final class TargetRender {

	private static final int MARKER_FILL = 0x40FF3333;
	private static final int MARKER_EDGE = 0xFFFF2222;
	private static final int LABEL = 0xFFFFDDDD;
	private static final int EDGE = 0xFFFFCC00;
	private static final int PATH_DOT = 0xFF00DDFF;
	private static final int PATH_DOT_ACTIVE = 0xFFFFDD00;
	private static final int PATH_DONE = 0xFF22CC22;
	private static final int MINE_DOT = 0xFFFF4444;
	private static final int PLACE_DOT = 0xFF4488FF;
	private static final int MAX_PATH_POINTS = 96;
	private static final int MIN_PIXEL_SPACING = 3;

	private TargetRender() {}

	public static void init() {
		HudRenderCallback.EVENT.register((graphics, tickCounter) -> render(graphics));
	}

	private static void render(GuiGraphics graphics) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) return;
		if (!Pathing.INSTANCE.isEnabled()) return;

		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();

		List<BlockPos> path = Pathing.INSTANCE.getPath();
		drawPath(graphics, client, path, Pathing.INSTANCE.getPathIndex(), width, height);
		Set<BlockPos> mine = new HashSet<>(Pathing.INSTANCE.getBlocksToMine());
		BlockPos taskTarget = Pathing.INSTANCE.getTaskTarget();
		if (taskTarget != null) {
			mine.add(taskTarget);
		}
		drawBlocks(graphics, client, mine, MINE_DOT);
		drawBlocks(graphics, client, Pathing.INSTANCE.getBlocksToPlace(), PLACE_DOT);
		drawTarget(graphics, client, Pathing.INSTANCE.getTarget(), width, height);
	}

	private static float fovFactor(Minecraft client) {
		// 1.21.1 Camera exposes no FOV and GameRenderer.getFov is private;
		// the base FOV option is close enough for projecting markers.
		double fov = client.options.fov().get();
		return (float) (1.0 / Math.tan(Math.toRadians(fov) / 2.0));
	}

	private static void drawPath(GuiGraphics graphics, Minecraft client, List<BlockPos> path, int pathIndex, int width, int height) {
		if (path == null || path.size() < 2) return;

		float f = fovFactor(client);

		// Downsample long routes so the loop stays bounded: at most MAX_PATH_POINTS
		// waypoints are projected per frame regardless of A* path length.
		int step = Math.max(1, (path.size() + MAX_PATH_POINTS - 1) / MAX_PATH_POINTS);

		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < path.size(); i += step) {
			indices.add(i);
		}
		if (indices.get(indices.size() - 1) != path.size() - 1) {
			indices.add(path.size() - 1);
		}

		int dotX = Integer.MIN_VALUE;
		int dotY = 0;
		for (int idx = 0; idx < indices.size(); idx++) {
			int i = indices.get(idx);
			BlockPos pos = path.get(i);
			Vector3f rel = relToCamera(client, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			if (rel.z >= 0) {
				dotX = Integer.MIN_VALUE;
				continue;
			}
			int sx = (int) Math.round((rel.x / -rel.z * f * 0.5 + 0.5) * width);
			int sy = (int) Math.round((1.0 - (rel.y / -rel.z * f * 0.5 + 0.5)) * height);

			// Skip waypoints that pile up on the same pixel.
			if (dotX != Integer.MIN_VALUE
					&& Math.abs(sx - dotX) < MIN_PIXEL_SPACING
					&& Math.abs(sy - dotY) < MIN_PIXEL_SPACING) {
				continue;
			}
			dotX = sx;
			dotY = sy;

			int col = i < pathIndex ? PATH_DONE : PATH_DOT;
			graphics.fill(sx - 2, sy - 2, sx + 2, sy + 2, col);
		}

		// Always pin the waypoint the bot is currently heading toward, even if the
		// downsampling above skipped it.
		if (pathIndex >= 0 && pathIndex < path.size()) {
			BlockPos active = path.get(pathIndex);
			Vector3f rel = relToCamera(client, active.getX() + 0.5, active.getY() + 0.5, active.getZ() + 0.5);
			if (rel.z < 0) {
				int sx = (int) Math.round((rel.x / -rel.z * f * 0.5 + 0.5) * width);
				int sy = (int) Math.round((1.0 - (rel.y / -rel.z * f * 0.5 + 0.5)) * height);
				graphics.fill(sx - 3, sy - 3, sx + 3, sy + 3, PATH_DOT_ACTIVE);
			}
		}
	}

	private static void drawBlocks(GuiGraphics graphics, Minecraft client, Set<BlockPos> blocks, int color) {
		if (blocks == null || blocks.isEmpty()) return;

		float f = fovFactor(client);
		int width = client.getWindow().getGuiScaledWidth();
		int height = client.getWindow().getGuiScaledHeight();

		for (BlockPos pos : blocks) {
			Vector3f rel = relToCamera(client, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
			if (rel.z >= 0) continue;
			int sx = (int) Math.round((rel.x / -rel.z * f * 0.5 + 0.5) * width);
			int sy = (int) Math.round((1.0 - (rel.y / -rel.z * f * 0.5 + 0.5)) * height);
			graphics.fill(sx - 2, sy - 2, sx + 2, sy + 2, color);
		}
	}

	private static void drawTarget(GuiGraphics graphics, Minecraft client, BlockPos goal, int width, int height) {
		if (goal == null) return;

		Vector3f rel = relToCamera(client, goal.getX() + 0.5, goal.getY() + 1.0, goal.getZ() + 0.5);

		// mirror behind-camera points to the front hemisphere so they still project
		if (rel.z >= 0) {
			rel.mul(-1.0f);
		}

		float f = fovFactor(client);
		float nx = rel.x / -rel.z * f;
		float ny = rel.y / -rel.z * f;
		int sx = (int) Math.round((nx * 0.5 + 0.5) * width);
		int sy = (int) Math.round((1.0 - (ny * 0.5 + 0.5)) * height);

		int margin = 22;
		if (sx < margin || sx > width - margin || sy < margin || sy > height - margin) {
			drawEdge(graphics, clamp(sx, margin, width - margin), clamp(sy, margin, height - margin), width, height);
		} else {
			drawMarker(graphics, client, sx, sy, goal);
		}
	}

	private static void drawMarker(GuiGraphics graphics, Minecraft client, int sx, int sy, BlockPos goal) {
		int size = 7;
		int x0 = sx - size;
		int y0 = sy - size;
		int x1 = sx + size;
		int y1 = sy + size;
		int edge = 1;

		graphics.fill(x0, y0, x1, y1, MARKER_FILL);
		graphics.fill(x0, y0, x1, y0 + edge, MARKER_EDGE);
		graphics.fill(x0, y1 - edge, x1, y1, MARKER_EDGE);
		graphics.fill(x0, y0, x0 + edge, y1, MARKER_EDGE);
		graphics.fill(x1 - edge, y0, x1, y1, MARKER_EDGE);

		double dist = Math.sqrt(client.gameRenderer.getMainCamera().getPosition().distanceToSqr(Vec3.atCenterOf(goal)));
		String label = goal.toShortString() + "  " + String.format("%.1fm", dist);
		graphics.drawString(client.font, label, x1 + 5, y1 - 4, LABEL, false);
	}

	private static void drawEdge(GuiGraphics graphics, int sx, int sy, int width, int height) {
		int size = 4;
		graphics.fill(sx - size, sy - size, sx + size, sy + size, EDGE);
		graphics.fill(sx - size, sy - size, sx + size, sy - size + 1, 0xFFFFFFFF);
		graphics.fill(sx - size, sy + size - 1, sx + size, sy + size, 0xFFFFFFFF);

		String arrow = sx < width / 2 ? "<-" : "->";
		graphics.drawString(Minecraft.getInstance().font, arrow, sx - size - 4, sy - 4, EDGE, false);
	}

	private static Vector3f relToCamera(Minecraft client, double wx, double wy, double wz) {
		Vec3 camPos = client.gameRenderer.getMainCamera().getPosition();
		Vector3f rel = new Vector3f(
				(float) (wx - camPos.x),
				(float) (wy - camPos.y),
				(float) (wz - camPos.z)
		);
		Quaternionf rotation = new Quaternionf(client.gameRenderer.getMainCamera().rotation()).conjugate();
		rel.rotate(rotation);
		return rel;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}

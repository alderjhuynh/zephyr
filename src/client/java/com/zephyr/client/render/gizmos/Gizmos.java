package com.zephyr.client.render.gizmos;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Drop-in replacement for the newer-version {@code net.minecraft.gizmos} rendering API
 * used by this client's ESP modules. Gizmos are queued during the client tick (guarded
 * by {@link #collect()}) and drawn once per frame from a world render event via
 * {@link #render(Camera, Matrix4f)}.
 */
public final class Gizmos {
    private static final List<Gizmo> QUEUE = new ArrayList<>();

    private Gizmos() {
    }

    public static GizmoSession collect() {
        return () -> {
        };
    }

    @FunctionalInterface
    public interface GizmoSession extends AutoCloseable {
        @Override
        void close();
    }

    public static LineGizmo line(Vec3 start, Vec3 end, int argb, float width) {
        LineGizmo gizmo = new LineGizmo(start, end, argb);
        QUEUE.add(gizmo);
        return gizmo;
    }

    public static CuboidGizmo cuboid(BlockPos pos, GizmoStyle style) {
        return cuboid(new AABB(pos), style);
    }

    public static CuboidGizmo cuboid(AABB box, GizmoStyle style) {
        CuboidGizmo gizmo = new CuboidGizmo(box, style.color());
        QUEUE.add(gizmo);
        return gizmo;
    }

    /** Renders and clears every queued gizmo. Called from a world render event. */
    public static void render(Camera camera, Matrix4f positionMatrix) {
        if (QUEUE.isEmpty()) return;

        Vec3 cameraPos = camera.getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);

        Tesselator tesselator = Tesselator.getInstance();
        for (Gizmo gizmo : QUEUE) {
            if (gizmo.alwaysOnTop) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }
            gizmo.render(tesselator, positionMatrix, cameraPos);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        QUEUE.clear();
    }

    public abstract static class Gizmo {
        protected boolean alwaysOnTop;

        public void setAlwaysOnTop() {
            this.alwaysOnTop = true;
        }

        abstract void render(Tesselator tesselator, Matrix4f matrix, Vec3 cameraPos);
    }

    public static final class LineGizmo extends Gizmo {
        private final Vec3 start;
        private final Vec3 end;
        private final int argb;

        private LineGizmo(Vec3 start, Vec3 end, int argb) {
            this.start = start;
            this.end = end;
            this.argb = argb;
        }

        @Override
        void render(Tesselator tesselator, Matrix4f matrix, Vec3 cameraPos) {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            addVertex(buffer, matrix, cameraPos, start, argb);
            addVertex(buffer, matrix, cameraPos, end, argb);
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
    }

    public static final class CuboidGizmo extends Gizmo {
        private final AABB box;
        private final int argb;

        private CuboidGizmo(AABB box, int argb) {
            this.box = box;
            this.argb = argb;
        }

        @Override
        void render(Tesselator tesselator, Matrix4f matrix, Vec3 cameraPos) {
            BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

            Vec3 min = new Vec3(box.minX, box.minY, box.minZ);
            Vec3 max = new Vec3(box.maxX, box.maxY, box.maxZ);

            Vec3[] corners = {
                    new Vec3(min.x, min.y, min.z), new Vec3(max.x, min.y, min.z),
                    new Vec3(max.x, min.y, max.z), new Vec3(min.x, min.y, max.z),
                    new Vec3(min.x, max.y, min.z), new Vec3(max.x, max.y, min.z),
                    new Vec3(max.x, max.y, max.z), new Vec3(min.x, max.y, max.z)
            };

            int[][] edges = {
                    {0, 1}, {1, 2}, {2, 3}, {3, 0},
                    {4, 5}, {5, 6}, {6, 7}, {7, 4},
                    {0, 4}, {1, 5}, {2, 6}, {3, 7}
            };

            for (int[] edge : edges) {
                addVertex(buffer, matrix, cameraPos, corners[edge[0]], argb);
                addVertex(buffer, matrix, cameraPos, corners[edge[1]], argb);
            }
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }
    }

    private static void addVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 cameraPos, Vec3 pos, int argb) {
        float r = (argb >> 16 & 0xFF) / 255.0F;
        float g = (argb >> 8 & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        float a = (argb >> 24 & 0xFF) / 255.0F;
        buffer.addVertex(matrix, (float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z))
                .setColor(r, g, b, a)
                .setNormal(1.0F, 0.0F, 0.0F);
    }
}

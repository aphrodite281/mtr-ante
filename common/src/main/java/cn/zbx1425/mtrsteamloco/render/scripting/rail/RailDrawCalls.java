package cn.zbx1425.mtrsteamloco.render.scripting.rail;

import cn.zbx1425.sowcerext.reuse.DrawScheduler;
import cn.zbx1425.mtrsteamloco.render.scripting.AbstractDrawCalls;
import cn.zbx1425.sowcer.math.Matrix4f;
import net.minecraft.world.level.Level;
import cn.zbx1425.sowcer.math.Vector3f;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.mtrsteamloco.render.scripting.util.DynamicModelHolder;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.AABB;

public interface RailDrawCalls {
    public static interface RailDrawCall {
        void commit(DrawScheduler drawScheduler, Matrix4f world, Frustum frustum, Vector3f cameraPos, int maxRailDistance);
    }

    public static class SimpleRailDrawCall implements RailDrawCall {
        private final Matrix4f matrix;
        private final ModelCluster model;
        private final DynamicModelHolder holder;
        private final Vector3f local;
        private final BlockPos pos;

        public SimpleRailDrawCall(ModelCluster model, Matrix4f matrix) {
            this.matrix = matrix.copy();
            this.model = model;
            this.holder = null;
            local = matrix.transform(new Vector3f(0, 0, 0));
            pos = local.toBlockPos();
        }

        public SimpleRailDrawCall(DynamicModelHolder holder, Matrix4f matrix) {
            this.matrix = matrix.copy();
            this.model = null;
            this.holder = holder;
            local = matrix.transform(new Vector3f(0, 0, 0));
            pos = local.toBlockPos();
        }

        @Override
        public void commit(DrawScheduler drawScheduler, Matrix4f world, Frustum frustum, Vector3f cameraPos, int maxRailDistance) {
            if (getDistanceFrom(cameraPos) <= maxRailDistance) {
                commit(drawScheduler, world);
            }
        }

        private void commit(DrawScheduler drawScheduler, Matrix4f world) {
            Matrix4f mat = world.copy();
            mat.mul(matrix);
            int light = LevelRenderer.getLightColor(Minecraft.getInstance().level, pos);
            drawScheduler.enqueue(model == null? holder.getUploadedModel() : model, mat, light);
        }

        private float getDistanceFrom(Vector3f src) {
            return src.distance(local);
        }
    }
}
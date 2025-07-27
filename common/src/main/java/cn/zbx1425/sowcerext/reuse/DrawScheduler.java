package cn.zbx1425.sowcerext.reuse;

import cn.zbx1425.sowcer.batch.BatchManager;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.shader.ShaderManager;
import cn.zbx1425.sowcer.util.GlStateTracker;
import cn.zbx1425.sowcer.util.DrawContext;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.model.integration.BufferSourceProxy;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class DrawScheduler {

    public final BatchManager batchManager = new BatchManager();
    public final ShaderManager shaderManager = new ShaderManager();

    private final List<ClusterDrawCall> clusterDrawCalls = new LinkedList<>();
    private final List<BlazeDrawCall> blazeDrawCalls = new LinkedList<>();

    public void reloadShaders(ResourceManager resourceManager) throws IOException {
        shaderManager.reloadShaders(resourceManager);
    }

    public void enqueue(ModelCluster model, Matrix4f pose, int light) {
        clusterDrawCalls.add(new ClusterDrawCall(model, pose, light));
    }

    public void enqueue(ModelCluster model, Matrix4f pose, int light, int overlay) {
        clusterDrawCalls.add(new ClusterDrawCall(model, pose, light, overlay));
    }

    public void enqueue(RawModel model, Matrix4f pose, int light) {
        blazeDrawCalls.add(new BlazeDrawCall(model, pose, light));
    }

    public void enqueue(RawModel model, Matrix4f pose, int light, int overlay) {
        blazeDrawCalls.add(new BlazeDrawCall(model, pose, light, overlay));
    }

    public void commit(BufferSourceProxy vertexConsumers, DrawContext drawContext) {
        if (!drawContext.drawWithBlaze && !shaderManager.isReady()) return;
        if (clusterDrawCalls.isEmpty()) return;
        if (drawContext.drawWithBlaze) {
            for (ClusterDrawCall drawCall : clusterDrawCalls)
                drawCall.model.enqueueOpaqueBlaze(vertexConsumers, drawCall.pose, drawCall.light, drawCall.overlay, drawContext);
        } else {
            for (ClusterDrawCall drawCall : clusterDrawCalls) {
                drawCall.model.enqueueOpaqueGl(batchManager, drawCall.pose, drawCall.light, drawCall.overlay, drawContext);
            }
        }
        if (drawContext.drawWithBlaze || drawContext.sortTranslucentFaces) {
            for (ClusterDrawCall drawCall : clusterDrawCalls)
                drawCall.model.enqueueTranslucentBlaze(vertexConsumers, drawCall.pose, drawCall.light, drawCall.overlay, drawContext);
        } else {
            for (ClusterDrawCall drawCall : clusterDrawCalls) {
                drawCall.model.enqueueTranslucentGl(batchManager, drawCall.pose, drawCall.light, drawCall.overlay, drawContext);
            }
        }
        for (BlazeDrawCall drawCall : blazeDrawCalls) {
            drawCall.model.writeBlazeBuffer(vertexConsumers, drawCall.pose, drawCall.light, drawCall.overlay, drawContext);
        }
        if (!drawContext.drawWithBlaze) {
            GlStateTracker.capture();
            commitRaw(drawContext);
            GlStateTracker.restore();
        }
        clusterDrawCalls.clear();
    }

    public void commitRaw(DrawContext drawContext) {
        batchManager.drawAll(shaderManager, drawContext);
    }

    public void drawAlone(ModelCluster model, Matrix4f pose, int light, BufferSourceProxy vertexConsumers, DrawContext drawContext) {
        drawAlone(model, pose, light, OverlayTexture.NO_OVERLAY, vertexConsumers, drawContext);
    }

    public void drawAlone(ModelCluster model, Matrix4f pose, int light, int overlay, BufferSourceProxy vertexConsumers, DrawContext drawContext) {
        BatchManager batchManager = new BatchManager();
        if (!drawContext.drawWithBlaze && !shaderManager.isReady()) return;
        if (drawContext.drawWithBlaze) {
            model.enqueueOpaqueBlaze(vertexConsumers, pose, light, overlay, drawContext);
        } else {
            model.enqueueOpaqueGl(batchManager, pose, light, overlay, drawContext);
        }
        if (drawContext.drawWithBlaze || drawContext.sortTranslucentFaces) {
            model.enqueueTranslucentBlaze(vertexConsumers, pose, light, overlay, drawContext);
        } else {
            model.enqueueTranslucentGl(batchManager, pose, light, overlay, drawContext);
        }
        if (!drawContext.drawWithBlaze) {
            GlStateTracker.capture();
            batchManager.drawAll(shaderManager, drawContext);
            GlStateTracker.restore();
        }
        vertexConsumers.commit();
    }

    private static class ClusterDrawCall {
        public ModelCluster model;
        public Matrix4f pose;
        public int light;
        public int overlay;

        public ClusterDrawCall(ModelCluster model, Matrix4f pose, int light) {
            this.model = model;
            this.pose = pose;
            this.light = light;
            this.overlay = OverlayTexture.NO_OVERLAY;
        }

        public ClusterDrawCall(ModelCluster model, Matrix4f pose, int light, int overlay) {
            this.model = model;
            this.pose = pose;
            this.light = light;
            this.overlay = overlay;
        }
    }

    private static class BlazeDrawCall {
        public RawModel model;
        public Matrix4f pose;
        public int light;
        public int overlay;

        public BlazeDrawCall(RawModel model, Matrix4f pose, int light) {
            this.model = model;
            this.pose = pose;
            this.light = light;
            this.overlay = OverlayTexture.NO_OVERLAY;
        }

        public BlazeDrawCall(RawModel model, Matrix4f pose, int light, int overlay) {
            this.model = model;
            this.pose = pose;
            this.light = light;
            this.overlay = overlay;
        }
    }
}

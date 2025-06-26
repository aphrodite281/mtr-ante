package cn.zbx1425.mtrsteamloco.scripting.util.client;

import cn.zbx1425.sowcer.util.GlStateTracker;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.reuse.ModelManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.RenderCall;

public class DynamicModelHolder {

    private ModelCluster uploadedModel = null;
    
    private RenderCall uploadCall = null;
    public void uploadLater(RawModel rawModel) {
        RawModel finalRawModel = rawModel.copyForMaterialChanges();
        finalRawModel.sourceLocation = null;
        uploadCall = () -> upload(rawModel);
        RenderSystem.recordRenderCall(() -> {
            if (uploadCall != null) {
                uploadCall.execute();
                uploadCall = null;
            }
        });
    }

    public void uploadNow(RawModel rawModel) {
        uploadCall = null;
        RawModel finalRawModel = rawModel.copyForMaterialChanges();
        finalRawModel.sourceLocation = null;
        upload(rawModel);
    }

    private void upload(RawModel finalRawModel) {
        assert RenderSystem.isOnRenderThreadOrInit() == true;
        boolean needProtection = !GlStateTracker.isStateProtected;
        if (needProtection) GlStateTracker.capture();
        ModelCluster lastUploadedModel = uploadedModel;
        ModelCluster newOne = new ModelCluster(finalRawModel, ModelManager.DEFAULT_MAPPING);
        this.uploadedModel = newOne;
        if (lastUploadedModel != null) lastUploadedModel.close();
        if (needProtection) GlStateTracker.restore();
    }

    public ModelCluster getUploadedModel() {
        return uploadedModel;
    }

    public void close() {
        RenderSystem.recordRenderCall(() -> {
            if (uploadedModel != null) {
                uploadedModel.close();
                uploadedModel = null;
            }
        });
    }
}

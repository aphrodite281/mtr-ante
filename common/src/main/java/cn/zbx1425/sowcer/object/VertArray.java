package cn.zbx1425.sowcer.object;

import cn.zbx1425.sowcer.batch.MaterialProp;
import cn.zbx1425.sowcer.model.Mesh;
import cn.zbx1425.sowcer.util.GlStateTracker;
import cn.zbx1425.sowcer.vertex.VertAttrMapping;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL33;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VertArray implements Closeable {

    private static final ScheduledExecutorService EXECTOR = Executors.newScheduledThreadPool(1);

    public int id;
    public MaterialProp materialProp;
    public IndexBuf indexBuf;
    public InstanceBuf instanceBuf;
    public VertAttrMapping mapping;

    public VertArray() {
        id = GL33.glGenVertexArrays();
    }

    private VertArray(VertArray other) {
        this.id = other.id;
        this.materialProp = other.materialProp;
        this.indexBuf = other.indexBuf;
        this.instanceBuf = other.instanceBuf;
        this.mapping = other.mapping;
    }

    public void create(Mesh mesh, VertAttrMapping mapping, InstanceBuf instanceBuf) {
        this.materialProp = mesh.materialProp;
        this.indexBuf = mesh.indexBuf;
        this.instanceBuf = instanceBuf;
        this.mapping = mapping;
        GL33.glBindVertexArray(id);
        mapping.setupAttrsToVao(mesh.vertBuf, instanceBuf);
        mesh.indexBuf.bind(GL33.GL_ELEMENT_ARRAY_BUFFER);
        unbind();
    }

    public void bind() {
        GlStateTracker.assertProtected();
        GL33.glBindVertexArray(id);
    }

    public static void unbind() {
        GlStateTracker.assertProtected();
        GL33.glBindVertexArray(0);
    }

    public void draw() {
        if (indexBuf == null || id == 0 || indexBuf.vertexCount == 0) return;
        if (instanceBuf == null) {
            GL33.glDrawElements(GL33.GL_TRIANGLES, indexBuf.vertexCount, indexBuf.indexType, 0L);
        } else {
            if (instanceBuf.size < 1) return;
            GL33.glDrawElementsInstanced(GL33.GL_TRIANGLES, indexBuf.vertexCount, indexBuf.indexType, 0L, instanceBuf.size);
        }
    }

    public int getFaceCount() {
        return indexBuf.faceCount * (instanceBuf == null ? 1 : instanceBuf.size);
    }

    public VertArray copyForMaterialChanges() {
        VertArray result = new VertArray(this);
        result.materialProp = result.materialProp.copy();
        return result;
    }

    @Override
    public void close() {
        EXECTOR.schedule(() -> _close(id), 10, TimeUnit.SECONDS);
        id = 0;
    }

    private void _close(int id) {
        if (RenderSystem.isOnRenderThread()) {
            GL33.glDeleteVertexArrays(id);
        } else {
            RenderSystem.recordRenderCall(() -> _close(id));
        }
    }
}

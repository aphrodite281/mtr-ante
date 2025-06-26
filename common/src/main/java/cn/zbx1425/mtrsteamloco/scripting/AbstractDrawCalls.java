package cn.zbx1425.mtrsteamloco.scripting;

import cn.zbx1425.mtrsteamloco.scripting.util.client.DynamicModelHolder;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.reuse.DrawScheduler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.*;

public abstract class AbstractDrawCalls {

    @FunctionalInterface
    public static interface DrawCall {
        public void commit(DrawScheduler drawScheduler, Matrix4f basePose, Matrix4f worldPose, int light);
    }

    public static abstract class DrawCallBase implements DrawCall {
        public ModelCluster model;
        public DynamicModelHolder modelHolder;
        public Matrix4f pose;

        public DrawCallBase(ModelCluster model, Matrix4f pose) {
            this.model = model;
            this.pose = pose;
        }

        public DrawCallBase(DynamicModelHolder model, Matrix4f pose) {
            this.modelHolder = model;
            this.pose = pose;
        }

        @Override
        public void commit(DrawScheduler drawScheduler, Matrix4f basePose, Matrix4f worldPose, int light) {
            Matrix4f finalPose = selectPose(basePose, worldPose).copy();
            finalPose.multiply(pose);
            if (model != null) {
                drawScheduler.enqueue(model, finalPose, light);
            } else {
                ModelCluster model = modelHolder.getUploadedModel();
                if (model != null) {
                    drawScheduler.enqueue(model, finalPose, light);
                }
            }
        }

        protected abstract Matrix4f selectPose(Matrix4f basePose, Matrix4f worldPose);
    }

    public static class ClusterDrawCall extends DrawCallBase {

        public ClusterDrawCall(ModelCluster model, Matrix4f pose) {
            super(model, pose);
        }

        public ClusterDrawCall(DynamicModelHolder model, Matrix4f pose) {
            super(model, pose);
        }

        @Override
        protected Matrix4f selectPose(Matrix4f basePose, Matrix4f worldPose) {
            return basePose;
        }
    }

    public static class WorldDrawCall extends DrawCallBase {
        public WorldDrawCall(ModelCluster model, Matrix4f pose) {
            super(model, pose);
        }

        public WorldDrawCall(DynamicModelHolder model, Matrix4f pose) {
            super(model, pose);
        }

        @Override
        protected Matrix4f selectPose(Matrix4f basePose, Matrix4f worldPose) {
            return worldPose;
        }
    }

    public static class PlaySoundCall {
        public SoundEvent sound;
        public Vector3f position;
        public float volume;
        public float pitch;

        public PlaySoundCall(SoundEvent sound, Vector3f position, float volume, float pitch) {
            this.sound = sound;
            this.position = position;
            this.volume = volume;
            this.pitch = pitch;
        }

        public void commit(ClientLevel level, Matrix4f worldPose) {
            Vector3f worldPos = worldPose.transform(position);
            level.playLocalSound(worldPos.x(), worldPos.y(), worldPos.z(),
                    sound, SoundSource.BLOCKS,
                    volume, pitch, false);
        }
    }

    public static class DrawCallMap implements DrawCall, Map<Object, DrawCall> {
        private final Map<Object, DrawCall> map = new HashMap<>();

        public DrawCallMap() {

        }

        public DrawCall put(Object key, DrawCall value) {
            return map.put(key, value);
        }

        public DrawCall get(Object key) {
            return map.get(key);
        }

        public DrawCall remove(Object key) {
            return map.remove(key);
        }

        public void clear() {
            map.clear();
        }

        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public Set<Object> keySet() {
            return map.keySet();
        }

        public Collection<DrawCall> values() {
            return map.values();
        }

        public Set<Map.Entry<Object, DrawCall>> entrySet() {
            return map.entrySet();
        }

        public int hashCode() {
            return map.hashCode();
        }

        public int size() {
            return map.size();
        }

        public void putAll(Map<? extends Object,? extends DrawCall> map) {
            this.map.putAll(map);
        }

        @Override
        public void commit(DrawScheduler drawScheduler, Matrix4f basePose, Matrix4f worldPose, int light) {
            for (DrawCall call : map.values()) {
                call.commit(drawScheduler, basePose, worldPose, light);
            }
        }
    }
}

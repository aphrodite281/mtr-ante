package cn.zbx1425.mtrsteamloco.scripting.train;

import cn.zbx1425.mtrsteamloco.scripting.AbstractScriptContext;
import cn.zbx1425.mtrsteamloco.scripting.util.client.DynamicModelHolder;
import cn.zbx1425.sowcer.math.Matrices;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcerext.model.ModelCluster;
import mtr.client.ClientData;
import mtr.data.TrainClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import cn.zbx1425.sowcerext.reuse.DrawScheduler;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.mtrsteamloco.scripting.AbstractDrawCalls.DrawCallMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

@SuppressWarnings("unused")
public class TrainScriptContext extends AbstractScriptContext {

    public TrainClient train;
    public TrainWrapper trainExtra;
    protected TrainWrapper trainExtraWriting;

    public TrainDrawCalls scriptResult;
    private TrainDrawCalls scriptResultWriting;
    public DrawCallMap[] drawCalls;

    public TrainScriptContext(TrainClient train) {
        scriptResult = new TrainDrawCalls(train.trainCars);
        scriptResultWriting = new TrainDrawCalls(train.trainCars);
        this.train = train;
        trainExtra = new TrainWrapper(train);
        trainExtraWriting = new TrainWrapper(train);
        drawCalls = new DrawCallMap[train.trainCars];
        for (int i = 0; i < train.trainCars; i++) {
            drawCalls[i] = new DrawCallMap();
        }
    }

    public void commitCar(int car, DrawScheduler drawScheduler, Matrix4f basePose, Matrix4f worldPose, int light) {
        drawCalls[car].commit(drawScheduler, basePose, worldPose, light);
        scriptResult.commitCar(car, drawScheduler, basePose, worldPose, light);
    }

    @Override
    public void renderFunctionFinished() {
        synchronized (this) {
            TrainDrawCalls temp = scriptResultWriting;
            scriptResultWriting = scriptResult;
            scriptResult = temp;
            scriptResultWriting.reset();
        }
    }

    @Override
    public Object getWrapperObject() {
        return trainExtra;
    }

    @Override
    public boolean isBearerAlive() {
        return !train.isRemoved && ClientData.TRAINS.contains(train);
    }

    public void extraFinished() {
        synchronized (this) {
            TrainWrapper temp = trainExtraWriting;
            trainExtraWriting = trainExtra;
            trainExtra = temp;
            trainExtraWriting.reset();
        }
    }

    public void drawCarModel(ModelCluster model, int carIndex, Matrices poseStack) {
        scriptResultWriting.addCarModel(carIndex, model, poseStack == null ? Matrix4f.IDENTITY : poseStack.last().copy());
    }

    public void drawCarModel(DynamicModelHolder model, int carIndex, Matrices poseStack) {
        scriptResultWriting.addCarModel(carIndex, model, poseStack == null ? Matrix4f.IDENTITY : poseStack.last().copy());
    }

    public void drawConnModel(ModelCluster model, int carIndex, Matrices poseStack) {
        scriptResultWriting.addConnModel(carIndex, model, poseStack == null ? Matrix4f.IDENTITY : poseStack.last().copy());
    }

    public void drawConnModel(DynamicModelHolder model, int carIndex, Matrices poseStack) {
        scriptResultWriting.addConnModel(carIndex, model, poseStack == null ? Matrix4f.IDENTITY : poseStack.last().copy());
    }

    public void drawConnStretchTexture(ResourceLocation location, int carIndex) {
        scriptResultWriting.drawConnStretchTexture(carIndex, location);
    }

    public void playCarSound(ResourceLocation sound, int carIndex, float x, float y, float z, float volume, float pitch) {
        scriptResultWriting.addCarSound(carIndex, 
#if MC_VERSION >= "11903"
        SoundEvent.createVariableRangeEvent(sound)
#else
        new SoundEvent(sound)
#endif
        , new Vector3f(x, y, z), volume, pitch
        );
    }

    public void playAnnSound(ResourceLocation sound, float volume, float pitch) {
        Minecraft.getInstance().execute(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && train.isPlayerRiding(player)) {
                Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                        sound, SoundSource.BLOCKS,
                        volume, pitch,
#if MC_VERSION >= "11900"
                        SoundInstance.createUnseededRandom(),
#endif
                        false, 0, SoundInstance.Attenuation.NONE, 0.0, 0.0, 0.0, true
                ));
            }
        });
    }
}

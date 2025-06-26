package cn.zbx1425.mtrsteamloco.scripting;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.scripting.util.client.*;
import cn.zbx1425.mtrsteamloco.scripting.util.*;
import cn.zbx1425.sowcer.math.Matrices;
import mtr.mappings.UtilitiesClient;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.Vector3f;
import cn.zbx1425.sowcerext.model.RawMesh;
import mtr.client.IDrawing;
import cn.zbx1425.sowcerext.model.RawModel;
import cn.zbx1425.sowcerext.model.ModelCluster;
import cn.zbx1425.sowcerext.model.integration.RawMeshBuilder;
import cn.zbx1425.sowcerext.util.ResourceUtil;
import mtr.client.ClientData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import mtr.block.IBlock;
import net.minecraft.world.entity.player.Player;
import cn.zbx1425.mtrsteamloco.scripting.util.WrappedEntity;
import cn.zbx1425.mtrsteamloco.scripting.AbstractDrawCalls;
import cn.zbx1425.mtrsteamloco.ClientConfig;
import cn.zbx1425.mtrsteamloco.data.ShapeSerializer;
import cn.zbx1425.mtrsteamloco.data.ConfigResponder;
import net.minecraft.network.chat.Component;
import com.google.gson.JsonObject;
import cn.zbx1425.mtrsteamloco.CustomResources;
import org.graalvm.polyglot.Value;
import com.google.gson.GsonBuilder;
import cn.zbx1425.sowcerext.reuse.ModelManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ScriptHolderClient extends ScriptHolderBase {
    public ScriptHolderClient() {
        super("client");
    }

    protected void appendImporter() {
        super.appendImporter();

        inject(ScriptResourceUtilClient.class, "Resources");
        inject(GraphicsTexture.class, "GraphicsTexture");
        inject(SoundHelper.class, "SoundHelper");
        inject(ParticleHelper.class, "ParticleHelper");
        inject(TickableSound.class, "TickableSound");
        inject(IScreen.class, "IScreen");
        inject(ConfigResponder.class, "ConfigResponder");
        inject(ClientConfig.class, "ClientConfig");
        inject(MinecraftClientUtil.class, "MinecraftClient");

        inject(AbstractDrawCalls.DrawCall.class, "DrawCall");
        inject(AbstractDrawCalls.ClusterDrawCall.class, "ClusterDrawCall");
        inject(AbstractDrawCalls.WorldDrawCall.class, "WorldDrawCall");

        eval("var ModelManager = Java.type('" + MainClient.class.getName() + "').modelManager;");
        inject(RawModel.class, "RawModel");
        inject(RawMesh.class, "RawMesh");
        inject(RawMeshBuilder.class, "RawMeshBuilder");
        inject(ModelCluster.class, "ModelCluster");
        inject(DynamicModelHolder.class, "DynamicModelHolder");

        inject(ClientData.class, "MTRClientData");
        inject(IBlock.class, "IBlock");
        inject(UtilitiesClient.class, "UtilitiesClient");
        inject(IDrawing.class, "IDrawing");

        try {
            String[] classesToLoad = {
                    "util.AddParticleHelper",
                    "particle.MadParticleOption",
                    "particle.SpriteFrom",
                    "command.inheritable.InheritableBoolean",
                    "particle.ParticleRenderTypes",
                    "particle.ChangeMode"
            };
            for (String classToLoad : classesToLoad) {
                Class<?> classToLoadClass = Class.forName("cn.ussshenzhou.madparticle." + classToLoad);
                inject(classToLoadClass, classToLoad.substring(classToLoad.lastIndexOf(".") + 1));
            }
            inject("foundMadParticle", true);
        } catch (ClassNotFoundException ignored) {
            inject("foundMadParticle", false);
            // Main.LOGGER.warn("MadParticle", ignored);
        }

        /*
        injects.put("Resources", new ScriptResourceUtil());
        injects.put("GraphicsTexture", GraphicsTexture.class);
        injects.put("SoundHelper", SoundHelper.class);
        injects.put("ParticleHelper", ParticleHelper.class);
        injects.put("TickableSound", TickableSound.class);
        injects.put("IScreen", IScreen.class);
        injects.put("ConfigResponder", ConfigResponder.class);
        injects.put("ClientConfig", ClientConfig.class);
        injects.put("MinecraftClient", MinecraftClientUtil.class);

        injects.put("DrawCall", AbstractDrawCalls.DrawCall.class);
        injects.put("ClusterDrawCall", AbstractDrawCalls.ClusterDrawCall.class);
        injects.put("WorldDrawCall", AbstractDrawCalls.WorldDrawCall.class);

        injects.put("ModelManager", MainClient.modelManager);
        injects.put("RawModel", RawModel.class);
        injects.put("RawMesh", RawMesh.class);
        injects.put("RawMeshBuilder", RawMeshBuilder.class);
        injects.put("ModelCluster", ModelCluster.class);
        injects.put("DynamicModelHolder", DynamicModelHolder.class);

        injects.put("MTRClientData", ClientData.class);
        injects.put("IBlock", IBlock.class);
        injects.put("UtilitiesClient", UtilitiesClient.class);
        injects.put("IDrawing", IDrawing.class);

        try {
            String[] classesToLoad = {
                    "util.AddParticleHelper",
                    "particle.MadParticleOption",
                    "particle.SpriteFrom",
                    "command.inheritable.InheritableBoolean",
                    "particle.ParticleRenderTypes",
                    "particle.ChangeMode"
            };
            for (String classToLoad : classesToLoad) {
                Class<?> classToLoadClass = Class.forName("cn.ussshenzhou.madparticle." + classToLoad);
                injects.put(classToLoad.substring(classToLoad.lastIndexOf(".") + 1), classToLoadClass);
            }
            injects.put("foundMadParticle", true);
        } catch (ClassNotFoundException ignored) {
            injects.put("foundMadParticle", false);
            // Main.LOGGER.warn("MadParticle", ignored);
        }*/
    }
}

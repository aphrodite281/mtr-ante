package cn.zbx1425.mtrsteamloco.scripting.util.client;

import mtr.mappings.TickableSoundInstanceMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import cn.zbx1425.sowcer.math.Vector3f;
import net.minecraft.resources.ResourceLocation;

public class TickableSound extends TickableSoundInstanceMapper {
    public TickableSound(ResourceLocation sound) {
        super(
#if MC_VERSION >= "11903"
        SoundEvent.createVariableRangeEvent(sound)
#else
        new SoundEvent(sound)
#endif
        , SoundSource.BLOCKS);
    }

    public TickableSound(ResourceLocation sound, SoundSource source) {
        super(
#if MC_VERSION >= "11903"
        SoundEvent.createVariableRangeEvent(sound)
#else
        new SoundEvent(sound)
#endif
        , source);
    }

    public void setData(float volume, float pitch, Vector3f pos) {
		this.pitch = pitch;
		this.volume = volume;

		x = pos.x();
		y = pos.y();
		z = pos.z();
	}

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setAttenuation(boolean attenuation) {
        if(attenuation) {
            this.attenuation = SoundInstance.Attenuation.LINEAR;
        }else{

            this.attenuation = SoundInstance.Attenuation.NONE;
        }
    }

    public void setRelative(boolean relative) {
        this.relative = relative;
    }

    public void play() {
        Minecraft.getInstance().getSoundManager().play(this);
    }

    public void quit() {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().getSoundManager().stop(this);
        });
    }

    public void pause() {
        stop();
    }

    @Override
	public boolean canStartSilent() {
		return true;
	}


	@Override
	public boolean canPlaySound() {
		return true;
	}

    @Override
    public void tick() {
        
    }
}
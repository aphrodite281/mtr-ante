package cn.zbx1425.mtrsteamloco.render.scripting.util;

import mtr.mappings.TickableSoundInstanceMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import cn.zbx1425.sowcer.math.Vector3f;

public class TickableSound extends TickableSoundInstanceMapper {
    public TickableSound(SoundEvent event){
        super(event, SoundSource.BLOCKS);
    }

    public TickableSound(SoundEvent event, SoundSource source){
        super(event, source);
    }

    public void setData(float volume, float pitch, Vector3f pos) {
		this.pitch = pitch;
		this.volume = volume;

		x = pos.x();
		y = pos.y();
		z = pos.z();
	}
    
    public void play(){
        Minecraft.getInstance().getSoundManager().play(this);
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
package cn.zbx1425.mtrsteamloco.scripting.util.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.client.renderer.LevelRenderer;
import cn.zbx1425.sowcer.math.Vector3f;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import java.lang.reflect.Field;
import net.minecraft.core.particles.ParticleType;
//import cn.zbx1425.mtrsteamloco.fabric.RegistriesWrapperImpl;
//import cn.zbx1425.mtrsteamloco.forge.RegistriesWrapperImpl;

public class ParticleHelper {
    public static void addParticle(ParticleOptions particle, Vector3f pos, Vector3f speed) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().level.addParticle(particle, pos.x(), pos.y(), pos.z(), speed.x(), speed.y(), speed.z());
        });
    }

    public static void addParticle(ParticleOptions particle, boolean overrideLimiter, Vector3f pos, Vector3f speed) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().level.addParticle(particle, overrideLimiter, pos.x(), pos.y(), pos.z(), speed.x(), speed.y(), speed.z());
        });
    }

    public static void addParticle(ParticleOptions particle, boolean b1, boolean b2,Vector3f pos, Vector3f speed) {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().levelRenderer.addParticle(particle, b1, b2, pos.x(), pos.y(), pos.z(), speed.x(), speed.y(), speed.z());
        });
    }

    /*public static SimpleParticleType registerParticleType(String name, boolean overrideLimiter) {
        try {
            RegistriesWrapperImpl registries = new cn.zbx1425.mtrsteamloco.fabric.RegistriesWrapperImpl();
            SimpleParticleType particleType = registries.createParticleType(overrideLimiter);
            register.registerParticleType(name, particleType);
            return particleType;
        }catch (Exception e) {
            RegistriesWrapperImpl registries = new cn.zbx1425.mtrsteamloco.forge.RegistriesWrapperImpl();
            SimpleParticleType particleType = registries.createParticleType(overrideLimiter);
            register.registerParticleType(name, particleType);
            return particleType;
        }
    }*/

    public static <T extends ParticleType> T getParticleType(String particleName) throws Exception {
        switch (particleName.toUpperCase()) { 
        case "AMBIENT_ENTITY_EFFECT":
            return (T)  ParticleTypes.AMBIENT_ENTITY_EFFECT;
        case "ANGRY_VILLAGER":
            return (T)  ParticleTypes.ANGRY_VILLAGER;
        case "BLOCK":
            return (T)  ParticleTypes.BLOCK;
#if MC_VERSION > "11701"
        case "BLOCK_MARKER":
            return (T)  ParticleTypes.BLOCK_MARKER;
#endif
        case "BUBBLE":
            return (T)  ParticleTypes.BUBBLE;
        case "CLOUD":
            return (T)  ParticleTypes.CLOUD;
        case "CRIT":
            return (T)  ParticleTypes.CRIT;
        case "DAMAGE_INDICATOR":
            return (T)  ParticleTypes.DAMAGE_INDICATOR;
        case "DRAGON_BREATH":
            return (T)  ParticleTypes.DRAGON_BREATH;
        case "DRIPPING_LAVA":
            return (T)  ParticleTypes.DRIPPING_LAVA;
        case "FALLING_LAVA":
            return (T)  ParticleTypes.FALLING_LAVA;
        case "LANDING_LAVA":
            return (T)  ParticleTypes.LANDING_LAVA;
        case "DRIPPING_WATER":
            return (T)  ParticleTypes.DRIPPING_WATER;
        case "FALLING_WATER":
            return (T)  ParticleTypes.FALLING_WATER;
        case "DUST":
            return (T)  ParticleTypes.DUST;
        case "DUST_COLOR_TRANSITION":
            return (T)  ParticleTypes.DUST_COLOR_TRANSITION;
        case "EFFECT":
            return (T)  ParticleTypes.EFFECT;
        case "ELDER_GUARDIAN":
            return (T)  ParticleTypes.ELDER_GUARDIAN;
        case "ENCHANTED_HIT":
            return (T)  ParticleTypes.ENCHANTED_HIT;
        case "ENCHANT":
            return (T)  ParticleTypes.ENCHANT;
        case "END_ROD":
            return (T)  ParticleTypes.END_ROD;
        case "ENTITY_EFFECT":
            return (T)  ParticleTypes.ENTITY_EFFECT;
        case "EXPLOSION_EMITTER":
            return (T)  ParticleTypes.EXPLOSION_EMITTER;
        case "EXPLOSION":
            return (T)  ParticleTypes.EXPLOSION;
        case "FALLING_DUST":
            return (T)  ParticleTypes.FALLING_DUST;
        case "FIREWORK":
            return (T)  ParticleTypes.FIREWORK;
        case "FISHING":
            return (T)  ParticleTypes.FISHING;
        case "FLAME":
            return (T)  ParticleTypes.FLAME;
        case "SOUL_FIRE_FLAME":
            return (T)  ParticleTypes.SOUL_FIRE_FLAME;
        case "SOUL":
            return (T)  ParticleTypes.SOUL;
        case "FLASH":
            return (T)  ParticleTypes.FLASH;
        case "HAPPY_VILLAGER":
            return (T)  ParticleTypes.HAPPY_VILLAGER;
        case "COMPOSTER":
            return (T)  ParticleTypes.COMPOSTER;
        case "HEART":
            return (T)  ParticleTypes.HEART;
        case "INSTANT_EFFECT":
            return (T)  ParticleTypes.INSTANT_EFFECT;
        case "ITEM":
            return (T)  ParticleTypes.ITEM;
        case "VIBRATION":
            return (T)  ParticleTypes.VIBRATION;
        case "ITEM_SLIME":
            return (T)  ParticleTypes.ITEM_SLIME;
        case "ITEM_SNOWBALL":
            return (T)  ParticleTypes.ITEM_SNOWBALL;
        case "LARGE_SMOKE":
            return (T)  ParticleTypes.LARGE_SMOKE;
        case "LAVA":
            return (T)  ParticleTypes.LAVA;
        case "MYCELIUM":
            return (T)  ParticleTypes.MYCELIUM;
        case "NOTE":
            return (T)  ParticleTypes.NOTE;
        case "POOF":
            return (T)  ParticleTypes.POOF;
        case "PORTAL":
            return (T)  ParticleTypes.PORTAL;
        case "RAIN":
            return (T)  ParticleTypes.RAIN;
        case "SMOKE":
            return (T)  ParticleTypes.SMOKE;
        case "SNEEZE":
            return (T)  ParticleTypes.SNEEZE;
        case "SPIT":
            return (T)  ParticleTypes.SPIT;
        case "SQUID_INK":
            return (T)  ParticleTypes.SQUID_INK;
        case "SWEEP_ATTACK":
            return (T)  ParticleTypes.SWEEP_ATTACK;
        case "TOTEM_OF_UNDYING":
            return (T)  ParticleTypes.TOTEM_OF_UNDYING;
        case "UNDERWATER":
            return (T)  ParticleTypes.UNDERWATER;
        case "SPLASH":
            return (T)  ParticleTypes.SPLASH;
        case "WITCH":
            return (T)  ParticleTypes.WITCH;
        case "BUBBLE_POP":
            return (T)  ParticleTypes.BUBBLE_POP;
        case "CURRENT_DOWN":
            return (T)  ParticleTypes.CURRENT_DOWN;
        case "BUBBLE_COLUMN_UP":
            return (T)  ParticleTypes.BUBBLE_COLUMN_UP;
        case "NAUTILUS":
            return (T)  ParticleTypes.NAUTILUS;
        case "DOLPHIN":
            return (T)  ParticleTypes.DOLPHIN;
        case "CAMPFIRE_COSY_SMOKE":
            return (T)  ParticleTypes.CAMPFIRE_COSY_SMOKE;
        case "CAMPFIRE_SIGNAL_SMOKE":
            return (T)  ParticleTypes.CAMPFIRE_SIGNAL_SMOKE;
        case "DRIPPING_HONEY":
            return (T)  ParticleTypes.DRIPPING_HONEY;
        case "FALLING_HONEY":
            return (T)  ParticleTypes.FALLING_HONEY;
        case "LANDING_HONEY":
            return (T)  ParticleTypes.LANDING_HONEY;
        case "FALLING_NECTAR":
            return (T)  ParticleTypes.FALLING_NECTAR;
        case "FALLING_SPORE_BLOSSOM":
            return (T)  ParticleTypes.FALLING_SPORE_BLOSSOM;
        case "ASH":
            return (T)  ParticleTypes.ASH;
        case "CRIMSON_SPORE":
            return (T)  ParticleTypes.CRIMSON_SPORE;
        case "WARPED_SPORE":
            return (T)  ParticleTypes.WARPED_SPORE;
        case "SPORE_BLOSSOM_AIR":
            return (T)  ParticleTypes.SPORE_BLOSSOM_AIR;
        case "DRIPPING_OBSIDIAN_TEAR":
            return (T)  ParticleTypes.DRIPPING_OBSIDIAN_TEAR;
        case "FALLING_OBSIDIAN_TEAR":
            return (T)  ParticleTypes.FALLING_OBSIDIAN_TEAR;
        case "LANDING_OBSIDIAN_TEAR":
            return (T)  ParticleTypes.LANDING_OBSIDIAN_TEAR;
        case "REVERSE_PORTAL":
            return (T)  ParticleTypes.REVERSE_PORTAL;
        case "WHITE_ASH":
            return (T)  ParticleTypes.WHITE_ASH;
        case "SMALL_FLAME":
            return (T)  ParticleTypes.SMALL_FLAME;
        case "SNOWFLAKE":
            return (T)  ParticleTypes.SNOWFLAKE;
        case "DRIPPING_DRIPSTONE_LAVA":
            return (T)  ParticleTypes.DRIPPING_DRIPSTONE_LAVA;
        case "FALLING_DRIPSTONE_LAVA":
            return (T)  ParticleTypes.FALLING_DRIPSTONE_LAVA;
        case "DRIPPING_DRIPSTONE_WATER":
            return (T)  ParticleTypes.DRIPPING_DRIPSTONE_WATER;
        case "FALLING_DRIPSTONE_WATER":
            return (T)  ParticleTypes.FALLING_DRIPSTONE_WATER;
        case "GLOW_SQUID_INK":
            return (T)  ParticleTypes.GLOW_SQUID_INK;
        case "GLOW":
            return (T)  ParticleTypes.GLOW;
        case "WAX_ON":
            return (T)  ParticleTypes.WAX_ON;
        case "WAX_OFF":
            return (T)  ParticleTypes.WAX_OFF;
        case "ELECTRIC_SPARK":
            return (T)  ParticleTypes.ELECTRIC_SPARK;
        case "SCRAPE":
            return (T)  ParticleTypes.SCRAPE;
        default:
            throw new Exception("Invalid particle name: " + particleName);
        }
    }
}
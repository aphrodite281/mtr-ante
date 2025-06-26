package cn.zbx1425.mtrsteamloco.block;

import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.network.PacketScreen;
import cn.zbx1425.mtrsteamloco.scripting.eyecandy.EyeCandyScriptContext;
import cn.zbx1425.sowcer.math.Vector3f;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.BlockEntityClientSerializableMapper;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.EntityBlockMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import cn.zbx1425.sowcer.math.Matrix4f;
import net.minecraft.world.level.block.state.StateDefinition;
#if MC_VERSION < "12000"
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
#endif
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import cn.zbx1425.mtrsteamloco.network.util.StringMapSerializer;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import mtr.block.IBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import java.util.Random;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import mtr.block.BlockTicketBarrier;
import mtr.data.TicketSystem;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import mtr.mappings.Utilities;
import mtr.SoundEvents;
import cn.zbx1425.mtrsteamloco.data.EyeCandyRegistry;
import cn.zbx1425.mtrsteamloco.scripting.ScriptHolderBase;
import cn.zbx1425.mtrsteamloco.data.EyeCandyProperties;
import cn.zbx1425.mtrsteamloco.data.ShapeSerializer;
import cn.zbx1425.mtrsteamloco.data.ConfigResponder;
import mtr.mappings.Text;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.world.phys.AABB;
import me.shedaniel.clothconfig2.impl.builders.TextDescriptionBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import java.util.*;
import java.io.IOException;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Supplier;

public class BlockEyeCandy extends BlockDirectionalMapper implements EntityBlockMapper {
    
    public static final EnumProperty<TicketSystem.EnumTicketBarrierOpen> OPEN = BlockTicketBarrier.OPEN;
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
    public static final ToIntFunction<BlockState> LIGHT_EMISSION = (p_153701_) -> {
        return p_153701_.getValue(LEVEL);
    };

    public BlockEyeCandy() {
        super(
#if MC_VERSION < "12000"
                BlockBehaviour.Properties.of(Material.METAL, MaterialColor.COLOR_GRAY)
#else
                BlockBehaviour.Properties.of()
#endif
                        .strength(2).lightLevel(LIGHT_EMISSION).dynamicShape()
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState()
            .setValue(FACING, ctx.getHorizontalDirection())
            .setValue(LEVEL, 0)
            .setValue(OPEN, TicketSystem.EnumTicketBarrierOpen.CLOSED);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEVEL, OPEN);
    }

    @Override
    public InteractionResult use(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hit) {
        if (player.getMainHandItem().is(mtr.Items.BRUSH.get())) {
            if (!level.isClientSide) {
                PacketScreen.sendScreenBlockS2C((ServerPlayer) player, "eye_candy", pos);
            } else {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        } else {
            if (level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof BlockEntityEyeCandy) {
                    BlockEntityEyeCandy blockEntityEyeCandy = (BlockEntityEyeCandy) blockEntity;
                    blockEntityEyeCandy.tryCallUseFunctionAsync(player);
                } else {
                    Main.LOGGER.warn("BlockEntityEyeCandy not found at " + pos + ", " + level);
                    return InteractionResult.PASS;
                }
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.PASS;
            }
        }
    }

    @Override
    public BlockEntityMapper createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new BlockEntityEyeCandy(blockPos, blockState);
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState blockState) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext collisionContext) {
        final BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof BlockEntityEyeCandy) {
            BlockEntityEyeCandy e = (BlockEntityEyeCandy) entity;
            try {
                return ShapeSerializer.getShape(e.getShape(), (int)state.getValue(FACING).toYRot());
            } catch (Exception e1) {
                // Main.LOGGER.error("Error getting shape :" + e1);
                return Shapes.block();
            }
        } else {
            // Main.LOGGER.error("BlockEntityEyeCandy not found at " + pos + ", " + world + ", " + state + ", " + entity);
            return Shapes.block();
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext collisionContext) {
        final BlockEntity entity = world.getBlockEntity(pos);
        if (entity instanceof BlockEntityEyeCandy) {
            BlockEntityEyeCandy e = (BlockEntityEyeCandy) entity;
            try {
                return ShapeSerializer.getShape(e.getCollisionShape(), (int)state.getValue(FACING).toYRot());
            } catch (Exception e1) {
                // Main.LOGGER.error("Error getting collision shape :" + e1);
                return Block.box(0, 0, 0, 16, 32, 16);
            }
        } else {
            // Main.LOGGER.error("BlockEntityEyeCandy not found at " + pos + ", " + world + ", " + state + ", " + entity);
            return Block.box(0, 0, 0, 16, 32, 16);
        }
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return Shapes.empty();
    }

	@Override
	public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        final BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof BlockEntityEyeCandy) {
            if (((BlockEntityEyeCandy) blockEntity).isTicketBarrier() == false) return;
        } else {
            return;
        }
        boolean isEntrance = ((BlockEntityEyeCandy) blockEntity).isEntrance;
		if (!world.isClientSide && entity instanceof Player) {
			final Direction facing = IBlock.getStatePropertySafe(state, FACING);
			final Vec3 playerPosRotated = entity.position().subtract(pos.getX() + 0.5, 0, pos.getZ() + 0.5).yRot((float) Math.toRadians(facing.toYRot()));
			final TicketSystem.EnumTicketBarrierOpen open = IBlock.getStatePropertySafe(state, OPEN);

			if (open.isOpen() && playerPosRotated.z > 0) {
				world.setBlockAndUpdate(pos, state.setValue(OPEN, TicketSystem.EnumTicketBarrierOpen.CLOSED));
			} else if (!open.isOpen() && playerPosRotated.z < 0) {
				final TicketSystem.EnumTicketBarrierOpen newOpen = TicketSystem.passThrough(world, pos, (Player) entity, isEntrance, !isEntrance, SoundEvents.TICKET_BARRIER, SoundEvents.TICKET_BARRIER_CONCESSIONARY, SoundEvents.TICKET_BARRIER, SoundEvents.TICKET_BARRIER_CONCESSIONARY, null, false);
				world.setBlockAndUpdate(pos, state.setValue(OPEN, newOpen));
				if (newOpen != TicketSystem.EnumTicketBarrierOpen.CLOSED && !world.getBlockTicks().hasScheduledTick(pos, this)) {
					Utilities.scheduleBlockTick(world, pos, this, 40);
				}
			}
		}
	}

	@Override
	public void tick(BlockState state, ServerLevel world, BlockPos pos) {
		world.setBlockAndUpdate(pos, state.setValue(OPEN, TicketSystem.EnumTicketBarrierOpen.CLOSED));
	}

    public static class BlockEntityEyeCandy extends BlockEntityClientSerializableMapper {

        public String prefabId = null;

        public float translateX = 0, translateY = 0, translateZ = 0;
        public float rotateX = 0, rotateY = 0, rotateZ = 0;
        public float scaleX = 1, scaleY = 1, scaleZ = 1;

        public boolean fullLight = false;

        private Map<String, String> customConfigs;
        private Map<String, ConfigResponder> customResponders;

        public EyeCandyScriptContext scriptContext = null;

        public boolean asPlatform = false;
        public float doorValue = 0;
        public boolean doorTarget = false;

        private String shape = "0, 0, 0, 16, 16, 16";
        private String collisionShape = "0, 0, 0, 0, 0, 0";

        public boolean fixedMatrix = false;
        private int lightLevel = 0;
        public boolean isTicketBarrier = false;
        public boolean isEntrance = false;

        public BlockEntityEyeCandy(BlockPos pos, BlockState state) {
            super(Main.BLOCK_ENTITY_TYPE_EYE_CANDY.get(), pos, state);
            customConfigs = new HashMap<>();
            customResponders = new HashMap<>();
        }

        @Override
        public void readCompoundTag(CompoundTag compoundTag) {
            String id = compoundTag.getString("prefabId");
            if (StringUtils.isEmpty(id)) id = null;
            setPrefabId(id);
            fullLight = compoundTag.getBoolean("fullLight");
            try {
                if (compoundTag.contains("customConfigs")) {
                    byte[] dataBytes = compoundTag.getByteArray("customConfigs");
                    customConfigs = StringMapSerializer.deserialize(dataBytes);
                } else if (compoundTag.contains("data")) {
                    byte[] configBytes = compoundTag.getByteArray("data");
                    customConfigs = StringMapSerializer.deserialize(configBytes);
                }
            }catch (IOException e) {
            }
            
            translateX = compoundTag.contains("translateX") ? compoundTag.getFloat("translateX") : 0;
            translateY = compoundTag.contains("translateY") ? compoundTag.getFloat("translateY") : 0;
            translateZ = compoundTag.contains("translateZ") ? compoundTag.getFloat("translateZ") : 0;
            rotateX = compoundTag.contains("rotateX") ? compoundTag.getFloat("rotateX") : 0;
            rotateY = compoundTag.contains("rotateY") ? compoundTag.getFloat("rotateY") : 0;
            rotateZ = compoundTag.contains("rotateZ") ? compoundTag.getFloat("rotateZ") : 0;
            scaleX = compoundTag.contains("scaleX") ? compoundTag.getFloat("scaleX") : 1;
            scaleY = compoundTag.contains("scaleY") ? compoundTag.getFloat("scaleY") : 1;
            scaleZ = compoundTag.contains("scaleZ") ? compoundTag.getFloat("scaleZ") : 1;
            asPlatform = compoundTag.contains("asPlatform") ? compoundTag.getBoolean("asPlatform") : false;
            // doorValue = compoundTag.contains("doorValue") ? compoundTag.getFloat("doorValue") : 0;
            // doorTarget = compoundTag.contains("doorTarget") ? compoundTag.getBoolean("doorTarget") : false;
            shape = compoundTag.contains("shape") ? compoundTag.getString("shape") : "0, 0, 0, 16, 16, 16";
            collisionShape = compoundTag.contains("collisionShape") ? compoundTag.getString("collisionShape") : "0, 0, 0, 0, 0, 0";
            fixedMatrix = compoundTag.contains("fixedMatrix") ? compoundTag.getBoolean("fixedMatrix") : false;
            lightLevel = compoundTag.contains("lightLevel") ? compoundTag.getInt("lightLevel") : 0;
            isTicketBarrier = compoundTag.contains("isTicketBarrier") ? compoundTag.getBoolean("isTicketBarrier") : false;
            isEntrance = compoundTag.contains("isEntrance") ? compoundTag.getBoolean("isEntrance") : false;
        }

        @Override
        public void writeCompoundTag(CompoundTag compoundTag) {
            compoundTag.putString("prefabId", prefabId == null ? "" : prefabId);
            compoundTag.putBoolean("fullLight", fullLight);
            try {
                byte[] configBytes = StringMapSerializer.serializeToByteArray(customConfigs);
                compoundTag.putByteArray("customConfigs", configBytes);
            }catch (IOException e) {
            }
            
            compoundTag.putFloat("translateX", translateX);
            compoundTag.putFloat("translateY", translateY);
            compoundTag.putFloat("translateZ", translateZ);
            compoundTag.putFloat("rotateX", rotateX);
            compoundTag.putFloat("rotateY", rotateY);
            compoundTag.putFloat("rotateZ", rotateZ);
            compoundTag.putFloat("scaleX", scaleX);
            compoundTag.putFloat("scaleY", scaleY);
            compoundTag.putFloat("scaleZ", scaleZ);
            compoundTag.putBoolean("asPlatform", asPlatform);
            // compoundTag.putFloat("doorValue", doorValue);
            // compoundTag.putBoolean("doorTarget", doorTarget);
            compoundTag.putString("shape", shape);
            compoundTag.putString("collisionShape", collisionShape);
            compoundTag.putBoolean("fixedMatrix", fixedMatrix);
            compoundTag.putInt("lightLevel", lightLevel);
            compoundTag.putBoolean("isTicketBarrier", isTicketBarrier);
            compoundTag.putBoolean("isEntrance", isEntrance);
        }

        public Matrix4f getBaseMatrix() {
            final Direction facing = IBlock.getStatePropertySafe(getBlockState(), FACING);
            final Matrix4f matrix = new Matrix4f();
            BlockPos pos = getWorldPos();
            matrix.translate(pos.getX() + 0.5F + translateX, pos.getY() + translateY, pos.getZ() + 0.5F + translateZ);
            matrix.rotateY((float) Math.toRadians(180F - facing.toYRot()));
            matrix.rotateX(rotateX);
            matrix.rotateY(rotateY);
            matrix.rotateZ(rotateZ);
            matrix.scale(scaleX, scaleY, scaleZ);
            return matrix;
        }

        public AABB getRenderBoundingBox() {
            return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        }

        public void setPrefabId(String new1) {
            String old = prefabId;
            prefabId = new1;
            if ((old == null && prefabId != null) || (old != null && !old.equals(prefabId))) {
                restore();
            } else if (getProperties().script != null && scriptContext == null) {
                scriptContext = new EyeCandyScriptContext(this);
            }
        }

        public void restore() {
            customConfigs.clear();
            customResponders.clear();
            if (scriptContext != null) {
                scriptContext.disposeForReload = true;
            }
            scriptContext = null;
            EyeCandyProperties properties = getProperties();
            setShape(properties.shape);
            setCollisionShape(properties.collisionShape);
            fixedMatrix = properties.fixedMatrix;
            setLightLevel(properties.lightLevel);
            isTicketBarrier = properties.isTicketBarrier;
            isEntrance = properties.isEntrance;
            asPlatform = properties.asPlatform;
            if (properties.script != null) {
                scriptContext = new EyeCandyScriptContext(this);
            }
        }

        public void setLightLevel(int lightLevel) {
            if (lightLevel >= 0 && lightLevel <= 15) {
                this.lightLevel = lightLevel;
            } else {
                Main.LOGGER.error("Invalid light level: " + lightLevel);
            }
        }

        public int getLightLevel() {
            return lightLevel;
        }

        public boolean setShape(String shape) {
            try {
                if (ShapeSerializer.isValid(shape, (int)getBlockYRot())) {
                    if (this.shape != shape) {
                        this.shape = shape;
                        return true;
                    }
                } else {
                    throw new Exception("Invalid!");
                }
            } catch (Exception e) {
                Main.LOGGER.error("Error setting shape for " + shape + " : " + e);
            }
            return false;
        }

        public boolean setCollisionShape(String collisionShape) {
            try {
                if (ShapeSerializer.isValid(collisionShape, (int)getBlockYRot())) {
                    if (this.collisionShape != collisionShape) {
                        this.collisionShape = collisionShape;
                        return true;
                    }
                } else {
                    throw new Exception("Invalid!");
                }
            } catch (Exception e) {
                Main.LOGGER.error("Error setting collision shape for " + collisionShape + " : " + e);
            }
            return false;
        }

        public String getShape() {
            return shape;
        }

        public String getCollisionShape() {
            return collisionShape;
        }

        public BlockPos getWorldPos() {
            return this.worldPosition;
        }

        public Vector3f getWorldPosVector3f() {
            return new Vector3f(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
        }

        public Vector3f getTransformPosVector3f() {
            return new Vector3f(this.worldPosition.getX() + translateX, this.worldPosition.getY() + translateY, this.worldPosition.getZ() + translateZ);
        }

        public void sendUpdateC2S() {
            PacketUpdateBlockEntity.sendUpdateC2S(this);
        }

        public float getBlockYRot(){
            try {
                final Direction facing = IBlock.getStatePropertySafe(getBlockState(), FACING);
                return facing.toYRot();
            } catch (Exception e) {
                return 0;
            }
        }

        public void setDoorValue(float value) {
            doorValue = value;
        }

        public void setDoorTarget(boolean target) {
            doorTarget = target;
        }

        public boolean isPlatform() {
            return asPlatform;
        }

        public boolean isTicketBarrier() {
            return isTicketBarrier;
        }

        public boolean isOpened() {
            try {
                return getBlockState().getValue(BlockEyeCandy.OPEN).isOpen();
            } catch (Exception e) {
                Main.LOGGER.info("Error in isOpened:" + e.getMessage());
                return false;
            }
        }

        public EyeCandyProperties getProperties() {
            EyeCandyProperties property = EyeCandyRegistry.getProperty(prefabId);
            return property != null ? property : EyeCandyProperties.DEFAULT;
        }

        public void tryCallUseFunctionAsync(Player player) {
            if (scriptContext == null) return;
            EyeCandyProperties prop = getProperties();
            if (prop == null) return;
            ScriptHolderBase scriptHolder = prop.script;
            if (scriptHolder == null) return;
            scriptHolder.tryCallUseFunctionAsync(scriptContext, player);
        }

        public Map<String, String> getCustomConfigs() {
            return customConfigs;
        } 

        public Map<String, ConfigResponder> getCustomResponders() {
            return customResponders;
        }

        public String getCustomConfig(String key) {
            return customConfigs.get(key);
        }

        public void registerCustomConfig(ConfigResponder responder) {
            responder.init(customConfigs);
            customResponders.put(responder.key(), responder);
        }

        public void removeCustomConfig(String key) {
            customConfigs.remove(key);
            customResponders.remove(key);
        }

        public void putCustomConfig(String key, String value) {
            customConfigs.put(key, value);
        }
    }
}
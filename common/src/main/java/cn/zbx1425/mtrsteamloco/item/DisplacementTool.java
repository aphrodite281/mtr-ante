package cn.zbx1425.mtrsteamloco.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.context.UseOnContext;
import mtr.item.ItemNodeModifierBase;
import mtr.data.Rail;
import mtr.data.RailType;
import net.minecraft.world.level.block.Block;
import mtr.data.TransportMode;
import mtr.block.BlockNode;
import cn.zbx1425.sowcer.math.Matrix4f;
import mtr.mappings.Text;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import mtr.packet.PacketTrainDataGuiServer;
import cn.zbx1425.sowcer.math.Vector3f;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import mtr.data.RailwayData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import cn.zbx1425.mtrsteamloco.data.RailActionsModuleExtraSupplier;
import net.minecraft.world.level.block.state.BlockState;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import cn.zbx1425.mtrsteamloco.network.util.IntegerArraySerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import cn.zbx1425.mtrsteamloco.network.PacketScreen;
import mtr.data.RailAngle;
import cn.zbx1425.mtrsteamloco.Main;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import cn.zbx1425.mtrsteamloco.data.RailModelProperties;
import cn.zbx1425.mtrsteamloco.data.RailModelRegistry;
import mtr.item.ItemWithCreativeTabBase;
import mtr.CreativeModeTabs;
import cn.zbx1425.mtrsteamloco.mixin.RailwayDataAccessor;
import cn.zbx1425.mtrsteamloco.mixin.RailAccessor;
import net.minecraft.server.level.ServerLevel;

import java.util.*;
import java.io.*;

public class DisplacementTool extends ItemWithCreativeTabBase {


    public DisplacementTool() {
        super(CreativeModeTabs.CORE);
    }

    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level world = ctx.getLevel();
        if (player == null || world == null) return InteractionResult.PASS;
        BlockPos pos = ctx.getClickedPos();
        BlockState blockState = world.getBlockState(pos);
        if (!(blockState.getBlock() instanceof mtr.block.BlockNode)) return InteractionResult.PASS;

        RailwayData railwayData = RailwayData.getInstance(world);

        if (railwayData == null) return InteractionResult.PASS;

        Map<BlockPos, Map<BlockPos, Rail>> railMap = ((RailwayDataAccessor) railwayData).getRails();

        if (railMap.get(pos) == null) return InteractionResult.SUCCESS;

        Optional<Map.Entry<BlockPos, Rail>> closestEntry = railMap.get(pos).entrySet().stream().min(Comparator.comparingDouble(entry ->
                Mth.degreesDifferenceAbs((float) -Math.toDegrees(Math.atan2(entry.getKey().getX() - pos.getX(), entry.getKey().getZ() - pos.getZ())), player.getYRot())
        ));
        if (closestEntry.isEmpty()) return InteractionResult.SUCCESS;
        BlockPos target = closestEntry.get().getKey();
        Rail rail = closestEntry.get().getValue();
        RailAccessor ra = (RailAccessor) rail;

        Vec3 playerPos = player.getPosition(1);
        Vec3 start = rail.getPosition(0);
        Vec3 diff = playerPos.subtract(start);

        double rot = ra.invokeGetRailAngle(true).getOpposite().angleRadians - ra.invokeGetRailAngle(false).angleRadians;

        diff = diff.yRot((float) -rot);

        Vec3 t = diff.add(rail.getPosition(rail.getLength()));

        rot = Math.toDegrees(rot);

        if (player instanceof ServerPlayer sp) {
            if (world instanceof ServerLevel sw) {
                final float fr = (float) rot;
                sw.getServer().execute(() -> {
                    sp.teleportTo(sw, t.x, t.y, t.z, player.getYRot() + fr, player.getXRot());
                });
            }
        }
        return InteractionResult.SUCCESS;
    }
}
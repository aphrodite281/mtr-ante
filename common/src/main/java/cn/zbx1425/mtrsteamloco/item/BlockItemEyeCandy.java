package cn.zbx1425.mtrsteamloco.item;

import mtr.CreativeModeTabs;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import mtr.mappings.Text;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import cn.zbx1425.mtrsteamloco.block.BlockDirectNode.BlockEntityDirectNode;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import  net.minecraft.world.item.CreativeModeTab;
import mtr.mappings.RegistryUtilities;
import net.minecraft.core.NonNullList;
import cn.zbx1425.mtrsteamloco.data.EyeCandyRegistry;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.data.EyeCandyProperties;
import cn.zbx1425.mtrsteamloco.gui.EyeCandyScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.client.Minecraft;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.List;

public class BlockItemEyeCandy extends BlockItem {

    public BlockItemEyeCandy(Block block)  {
		super(block, RegistryUtilities.createItemProperties(() -> Main.EYE_CANDY_TAB));
    }

    @Override
    public InteractionResult place(BlockPlaceContext blockPlaceContext) {
        System.out.println("BlockItemEyeCandy:place" + blockPlaceContext.getLevel().isClientSide);
        return super.place(blockPlaceContext);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide) {
            openEyeCandyScreen(usedHand);
        }
        ItemStack itemStack = player.getItemInHand(usedHand);
        return InteractionResultHolder.success(itemStack);
    }

    private static void openEyeCandyScreen(InteractionHand hand) {
        Minecraft.getInstance().setScreen(EyeCandyScreen.createScreen(hand, null));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
        if (stack.getItem() instanceof BlockItemEyeCandy bi) {
            CompoundTag tag = stack.getTagElement("BlockEntityTag");
            if (tag == null) {
                return;
            }
            if (tag.contains("prefabId")) {
                if (EyeCandyRegistry.ELEMENTS.containsKey(tag.getString("prefabId"))) {
                    list.add(EyeCandyRegistry.ELEMENTS.get(tag.getString("prefabId")).name);
                } else {
                    list.add(Text.literal(tag.getString("prefabId")));
                }
            }
        }
    }
#if MC_VERSION <= "11902"
    @Override
    public void fillItemCategory(CreativeModeTab category, NonNullList<ItemStack> items) {
        if (category == Main.EYE_CANDY_TAB) {
            Client.fillItemCategory(items);
        }
    }
#endif

    public static class Client {
        public static void fillItemCategory(List<ItemStack> items) {
            items.add(new ItemStack(Main.ITEM_EYE_CANDY.get()));
            for (EyeCandyProperties prop : EyeCandyRegistry.ELEMENTS.values()) {
                ItemStack stack = new ItemStack(Main.ITEM_EYE_CANDY.get());
                CompoundTag tag = stack.getOrCreateTagElement("BlockEntityTag");
                VirtualEyeCandy virtualEyeCandy = new VirtualEyeCandy(() -> stack.getOrCreateTagElement("BlockEntityTag"));
                virtualEyeCandy.setPrefabId(prop.key);
                virtualEyeCandy.sendUpdateC2S();
                items.add(stack);
            }
        }

        private static class VirtualEyeCandy extends BlockEyeCandy.BlockEntityEyeCandy {
            private Supplier<CompoundTag> tagSupplier;

            public VirtualEyeCandy(Supplier<CompoundTag> tagSupplier) {
                super(new BlockPos(0, -1145141919, 0), null);
                readCompoundTag(tagSupplier.get());
                this.tagSupplier = tagSupplier;
            }

            @Override
            public void sendUpdateC2S() {
                writeCompoundTag(tagSupplier.get());
            }
        }
    }
}
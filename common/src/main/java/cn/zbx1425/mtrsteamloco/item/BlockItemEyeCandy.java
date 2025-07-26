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

import java.util.function.Function;
import java.util.List;

public class BlockItemEyeCandy extends BlockItem {

    public BlockItemEyeCandy(Block block)  {
		super(block, RegistryUtilities.createItemProperties(() -> Main.EYE_CANDY_TAB));
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
#if MC_VERSION <= "11903"
    @Override
    public void fillItemCategory(CreativeModeTab category, NonNullList<ItemStack> items) {
        if (category == Main.EYE_CANDY_TAB) {
            Client.fillItemCategory(items);
        }
    }
#endif

    public static class Client {
        public static void fillItemCategory(NonNullList<ItemStack> items) {
            items.add(new ItemStack(Main.ITEM_EYE_CANDY.get()));
            for (EyeCandyProperties prop : EyeCandyRegistry.ELEMENTS.values()) {
                ItemStack stack = new ItemStack(Main.ITEM_EYE_CANDY.get());
                CompoundTag tag = stack.getOrCreateTagElement("BlockEntityTag");
                tag.putString("prefabId", prop.key);
                items.add(stack);
            }
        }
    }
}
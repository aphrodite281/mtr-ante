package cn.zbx1425.mtrsteamloco.gui;

import net.minecraft.client.gui.screens.Screen;
import mtr.mappings.Text;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import cn.zbx1425.mtrsteamloco.Main;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import cn.zbx1425.mtrsteamloco.network.PacketRoutePathCreator;
import cn.zbx1425.mtrsteamloco.data.IRoute;
import mtr.data.Route;
import mtr.data.TransportMode;
import mtr.mappings.UtilitiesClient;
import mtr.data.Rail;
import mtr.client.IDrawing;
import net.minecraft.client.Minecraft;
import cn.zbx1425.mtrsteamloco.data.RailExtraSupplier;
import mtr.path.PathData;
import cn.zbx1425.mtrsteamloco.network.PacketUpdateHoldingItem;
import cn.zbx1425.mtrsteamloco.item.RoutePathCreator;
import net.minecraft.client.gui.components.EditBox;
import mtr.data.RailType;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import cn.zbx1425.mtrsteamloco.gui.entries.ButtonListEntry;

#if MC_VERSION >= "12000"
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.CreativeModeTabs;
#else
import net.minecraft.client.gui.GuiComponent;
#endif

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;

public class RoutePathCreatorScreen extends Screen implements IGraphics {
    public static Screen createScreen(Screen parent) {
        return new RoutePathCreatorScreen(parent);
    }

    Screen parent;
    CompoundTag tag = new CompoundTag();
    List<PathData> pathData = new ArrayList<>();
    Set<Integer> selectedParts = new HashSet<>();
    List<WidgetLabel> pathParts = new ArrayList<>();
    int currentPage = 0;
    EditBox pageIn = null;
    Button btnNextPage = UtilitiesClient.newButton(Text.literal(">"), btn -> {
        currentPage++;
        pageIn.setValue(String.format("%d", currentPage));
        pageIn.moveCursorToStart();    
    });
    Button btnPrevPage = UtilitiesClient.newButton(Text.literal("<"), btn -> {
        currentPage--;
        pageIn.setValue(String.format("%d", currentPage));
        pageIn.moveCursorToStart();
    });

    Button btnReturn = UtilitiesClient.newButton(Text.literal("X"), btn -> onClose());
    Button btnRemove = UtilitiesClient.newButton(Text.literal("-"), btn -> {
        if (pathData.size() > 0) {
            pathData.remove(pathData.size() - 1);
            selectPart();
            pathParts.remove(pathParts.size() - 1);
            clearWidgets(); 
            init();
        }

        if (pathData.size() <= 0) {
            if (tag.contains("last_pos")) tag.remove("last_pos");
            else return;
        } else tag.putLong("last_pos", getPos(pathData.get(pathData.size() - 1).rail, false).asLong());
        pushData();
    });
    Button btnClear = UtilitiesClient.newButton(Text.literal("Clear"), btn -> {
        pathData.clear();
        pathParts.clear();
        tag.remove("last_pos");
        pushData();
        selectPart();
        clearWidgets();
        init();
    });
    Button btnActionScreen = UtilitiesClient.newButton(Text.literal("Action"), btn -> enterActionScreen());
    EditBox railTypeIn = null;
    EditBox dwellTimeIn = null;


    private RoutePathCreatorScreen(Screen parent) {
        super(Text.literal("Route Path Creator Screen"));
        this.parent = parent;
        pullData();

        railTypeIn = new EditBox(Minecraft.getInstance().font, 0, 0, 10, 20, Text.literal(""));
        railTypeIn.active = false;
        railTypeIn.setResponder(s -> {
            try {
                RailType type = RailType.valueOf(s);
                boolean changed = false;
                for (int i : selectedParts) {
                    PathData pd = pathData.get(i);
                    if (pd.rail.railType != type) {
                        ((RailExtraSupplier) (Object) pathData.get(i).rail).setRailType(type);
                        changed = true;
                        pathParts.set(i, genGuiLabel(i));
                    }
                }
                if (changed) {
                    pushData();
                    clearWidgets();
                    init();
                }
                railTypeIn.setTextColor(0xffffff);
            } catch (Exception e) {
                railTypeIn.setTextColor(0xff0000);
            }
        });

        dwellTimeIn = new EditBox(Minecraft.getInstance().font, 0, 0, 10, 20, Text.literal(""));
        dwellTimeIn.active = false;
        dwellTimeIn.setResponder(s -> {
            try {
                float dwellTime = Float.parseFloat(s);
                dwellTime *= 2;
                if (Math.abs(dwellTime % 1) > 1e-6 || dwellTime < 0) throw new Exception();
                dwellTime += 0.1f;
                boolean changed = false;
                for (int i : selectedParts) {
                    PathData pd = pathData.get(i);
                    if (pd.dwellTime != dwellTime) {
                        pathData.set(i, new PathData(pd.rail, pd.savedRailBaseId, (int) dwellTime, pd.startingPos, getPos(pd.rail, false), pd.stopIndex));
                        changed = true;
                        pathParts.set(i, genGuiLabel(i));
                    }
                }
                if (changed) {
                    pushData();
                    clearWidgets();
                    init();
                }
                railTypeIn.setTextColor(0xffffff);
            } catch (Exception e) {
                railTypeIn.setTextColor(0xff0000);
            }
        });

        pageIn = new EditBox(Minecraft.getInstance().font, 0, 0, 10, 20, Text.literal(""));
        pageIn.setResponder(s -> {
            try {
                int page = Integer.parseInt(s);
                if (page < 0 || page > maxPage()) throw new Exception();
                currentPage = page;
                pageIn.setTextColor(0xffffff);
            } catch (Exception e) {
                pageIn.setTextColor(0xff0000);
            }
        });
        pageIn.setValue("0");
        pageIn.moveCursorToStart();
    }

    private int maxPage() {
        int res = (int) Math.ceil(pathParts.size() * 1.0f / partPerPage());
        if (res > 0) res -= 1;
        return res;
    }

    private int partPerPage() {
        return Math.max((height - 50 - 30) / 30, 1);
    }

    private boolean pullTag() {
        ItemStack itemStack = Minecraft.getInstance().player.getMainHandItem();
        if (!itemStack.is(Main.ROUTE_PATH_CREATOR.get())) return false;
        tag = itemStack.getOrCreateTagElement("ANTE-Data").copy();
        return true;
    }

    private boolean pushTag() {
        ItemStack itemStack = Minecraft.getInstance().player.getMainHandItem();
        if (!itemStack.is(Main.ROUTE_PATH_CREATOR.get())) return false;
        itemStack.getOrCreateTag().put("ANTE-Data", tag.copy());
        PacketUpdateHoldingItem.sendUpdateC2S();
        return true;
    }

    private void pullData() {
        pathParts.clear();
        pathData.clear();
        pullTag();
        pathData = RoutePathCreator.readPath(tag);

        for (int i = 0; i < pathData.size(); i++) {
            pathParts.add(genGuiLabel(i));
        }
    }

    private void pushData() {
        RoutePathCreator.writePath(pathData, tag);
        pushTag();
    }

    private WidgetLabel genGuiLabel(int index) {
        PathData data = pathData.get(index);

        final boolean tb0 = index > 0 ? pathData.get(index - 1).isOppositeRail(data) : false;
        final boolean tb1 = index < pathData.size() - 1 ? pathData.get(index + 1).isOppositeRail(data) : false;
        WidgetLabel label = new WidgetLabel(
            -40, -40, 20, 
            Text.literal(
                //getPos(data.rail, true).toShortString() + " -> " + getPos(data.rail, false).toShortString()  + '\n' + 
                //data.rail.railType.name() + " " + 
                (tb0 ? "▲▲▲    \n": (tb1 ? " \n" : "")) + 
                data.rail.railType + ":" + data.rail.railType.speedLimit + "km/h \n" + 
                String.format("%.1f", data.rail.getLength()) + "m " + (data.dwellTime * 0.5f) + "s" + 
                (tb1 ? "\n    ▼▼▼": (tb0 ? "\n " : ""))
            ),
            () -> selectPart(index)
        );
        return label;
    }

    private void selectPart(Integer... index) {
        selectedParts.clear();

        railTypeIn.active = false;
        dwellTimeIn.active = false;
        railTypeIn.setValue("");
        dwellTimeIn.setValue("");

        for (int i : index) {
            selectedParts.add(i);
            railTypeIn.active = true;
            dwellTimeIn.active = true;
            PathData data = pathData.get(i);

            railTypeIn.setValue(data.rail.railType.name());
            dwellTimeIn.setValue(String.format("%.1f", data.dwellTime * 0.5f));
        }

        railTypeIn.moveCursorToStart();
        dwellTimeIn.moveCursorToStart();
    }

    private static BlockPos getPos(Rail rail, boolean isStart) {
        RailExtraSupplier supplier = (RailExtraSupplier) (Object) rail;
        return isStart? supplier.getPosStart() : supplier.getPosEnd();
    }

    private void enterActionScreen() {
        boolean changed = false;
        final String routeName;
        if (!tag.contains("route_name")) {
            routeName = "Route Pro Max";
            tag.putString("route_name", routeName);
            changed = true;
        } else routeName = tag.getString("route_name");

        final int routeColor;
        if (!tag.contains("route_color")) {
            routeColor = 0xFFFFFF;
            tag.putInt("route_color", routeColor);
            changed = true;
        } else routeColor = tag.getInt("route_color");

        if (changed) pushData();

        ConfigBuilder builder = ConfigBuilder.create()
        .setParentScreen(this)
        .setTitle(Text.translatable("gui.mtrsteamloco.route_path_creator.action_screen.title"))
        .setDoesConfirmSave(false)
        .setSavingRunnable(() -> pushData());
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory common = builder.getOrCreateCategory(
            Text.translatable("gui.mtrsteamloco.config.client.category.common")
        );

        common.addEntry(entryBuilder
            .startTextField(Text.translatable("gui.mtr.route_name"), routeName)
            .setErrorSupplier(s -> {
                if (s == routeName) return Optional.empty();
                tag.putString("route_name", s);
                pushTag();
                return Optional.empty();
            })
            .build()
        );

        common.addEntry(entryBuilder
            .startColorField(Text.translatable("gui.mtr.route_color"), routeColor)
            .setErrorSupplier(i -> {
                if (i == routeColor) return Optional.empty();
                tag.putInt("route_color", i);
                pushTag();
                return Optional.empty();
            })
            .build()
        );

        if (pathData.size() > 2 && pathData.get(pathData.size() - 1).rail.railType == RailType.PLATFORM && (!pathData.get(pathData.size() - 2).isOppositeRail(pathData.get(pathData.size() - 1)))) {
            common.addEntry(ButtonListEntry.createCenteredInstance(Text.literal("创建路线"), btn -> {
                Route route = new Route(TransportMode.TRAIN);
                route.name = tag.getString("route_name");
                route.color = tag.getInt("route_color");
                ((IRoute) (Object) route).setPathData(pathData);
                PacketRoutePathCreator.sendRouteC2S(route);
            }));
        } else {
            common.addEntry(entryBuilder.startTextDescription(Text.translatable("gui.mtrsteamloco.route_path_creator.action_screen.illegal_path")).build());
        }
        

        minecraft.setScreen(builder.build());
    }

    @Override
    protected void init() {
        addRenderableWidget(btnReturn);
        IDrawing.setPositionAndWidth(btnReturn, 10, 10, 20);

        int w0 = 80;

        int w1 = 100;
        int x0 = w0 + (width - w0 - w1) / 2;
        addRenderableWidget(railTypeIn);
        IDrawing.setPositionAndWidth(railTypeIn, x0, 60, w1);
        addRenderableWidget(dwellTimeIn);
        IDrawing.setPositionAndWidth(dwellTimeIn, x0, 85, w1);
        addRenderableWidget(btnActionScreen);
        IDrawing.setPositionAndWidth(btnActionScreen, x0, 110, w1);

        int y = 50;
        for (WidgetLabel label : pathParts) {
            addRenderableWidget(label);
            IDrawing.setPositionAndWidth(label, 0, y - label.getHeight() / 2, w0);
            y +=30;
        }

        IDrawing.setPositionAndWidth(btnPrevPage, 5, height - 30, 20);
        IDrawing.setPositionAndWidth(pageIn, 30, height - 30, 20);
        IDrawing.setPositionAndWidth(btnNextPage, 55, height - 30, 20);
        addRenderableWidget(btnPrevPage);
        addRenderableWidget(pageIn);
        addRenderableWidget(btnNextPage);

        addRenderableWidget(btnClear);
        IDrawing.setPositionAndWidth(btnClear, width - 50, 10, 40);
        addRenderableWidget(btnRemove);
        IDrawing.setPositionAndWidth(btnRemove, width - 50, 30, 40);
    }

    @Override
#if MC_VERSION >= "12000"
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float partialTick) {
#else
    public void render(PoseStack ctx, int mouseX, int mouseY, float partialTick) {
#endif
        renderDirtBackground(this, ctx);
        drawCenteredString(ctx, minecraft.font, "Route Path Creator Screen", width / 2, 10, 0xFFFFFF);
        drawCenteredString(ctx, minecraft.font, selectedParts + " " + partPerPage() + " " + maxPage(), width / 2, height - 10, 0xFFFFFF);

        if (currentPage < 0) currentPage = 0;
        else if (currentPage >= maxPage()) currentPage = maxPage();
        if (currentPage == 0) btnPrevPage.active = false;
        else btnPrevPage.active = true;
        if (currentPage == maxPage()) btnNextPage.active = false;
        else btnNextPage.active = true;

        for (int i = 0; i <= maxPage(); i++) {
            for (int j = 0; j < partPerPage(); j++) {
                int index = i * partPerPage() + j;
                if(index >= pathParts.size()) break;

                WidgetLabel label = pathParts.get(index);
                
                if (i == currentPage) label.visible = true;
                else label.visible = false;
                
                if (selectedParts.contains(index)) label.color = 0xff78cb;
                else label.color = 0xffffff;

                IDrawing.setPositionAndWidth(label, 0, 50 + 30 * j - label.getHeight() / 2, 80);
            }
        }
        super.render(ctx, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        railTypeIn.tick();
        dwellTimeIn.tick();
    }
}
package cn.zbx1425.mtrsteamloco.scripting;

import cn.zbx1425.mtrsteamloco.BuildConfig;
import cn.zbx1425.mtrsteamloco.Main;
import cn.zbx1425.mtrsteamloco.render.integration.MtrModelRegistryUtil;
import cn.zbx1425.mtrsteamloco.scripting.util.client.GraphicsTexture;
import cn.zbx1425.sowcerext.util.ResourceUtil;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import mtr.client.ClientData;
import mtr.mappings.Utilities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleType;
#if MC_VERSION >= "11903"
import net.minecraft.core.registries.BuiltInRegistries;
#else
import net.minecraft.core.Registry;
#endif
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.zbx1425.mtrsteamloco.CustomResources;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Context;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedString;
import java.util.*;
import java.util.List;

@SuppressWarnings("unused")
public class ScriptResourceUtil {

    protected static Context activeContext;
    protected static final Stack<ResourceLocation> scriptLocationStack = new Stack<>();
    protected static final Logger LOGGER = LoggerFactory.getLogger("MTR-ANTE JS");

    public static final boolean ANTE_FLAG = true;

    static {
        Main.LOGGER.info("NTE version: " + getNTEVersion() + " (int " + getNTEVersionInt() + ") (protocol " + getNTEProtoVersion() + ")");
    }

    public static void executeScript(Context ctx, String script, ResourceLocation identifier) throws IOException {
        activeContext = ctx;

        scriptLocationStack.push(identifier);

        Source source = Source.newBuilder("js", script, identifier.getPath())
            .mimeType("application/javascript")
            .cached(true)
            .build();
            
        ctx.eval(source);

        scriptLocationStack.pop();
    }

    public static void includeScript(Object pathOrIdentifier) throws IOException {
        if (activeContext == null) throw new RuntimeException(
                "Cannot use include in functions, as by that time ANTE no longer processes scripts."
        );
        ResourceLocation identifier;
        if (pathOrIdentifier instanceof ResourceLocation) {
            identifier = (ResourceLocation) pathOrIdentifier;
        } else {
            identifier = idRelative(pathOrIdentifier.toString());
        }

        if (!hasResource(identifier)) {
            Main.LOGGER.warn("File not found in include: at " + scriptLocationStack.peek().toString() + "for input " + pathOrIdentifier.toString() + " resolved to " + identifier.toString());
            return;
        }
        
        executeScript(activeContext, readString(identifier), identifier);
    }

    public static void print(Object... objects) {
        if (objects.length == 0) objects = new Object[] {"null"};
        StringBuilder sb = new StringBuilder();
        sb.append("[ANTE-JS] ");
        for (Object object : objects) {
            sb.append(object.toString());
            sb.append(" ");
        }
        Main.LOGGER.info(sb.toString().trim());
    }

    public static ResourceManager manager() {
        return MtrModelRegistryUtil.resourceManager;
    }

    public static ResourceManager mgr() {
        return MtrModelRegistryUtil.resourceManager;
    }

    public static ResourceLocation identifier(String textForm) {
        return new ResourceLocation(textForm);
    }
    public static ResourceLocation id(String textForm) {
        return new ResourceLocation(textForm);
    }

    public static ResourceLocation idRelative(String textForm) {
        if (scriptLocationStack.empty()) throw new RuntimeException(
                "Cannot use idRelative in functions."
        );
        return ResourceUtil.resolveRelativePath(scriptLocationStack.peek(), textForm, null);
    }
    public static ResourceLocation idr(String textForm) {
        if (scriptLocationStack.empty()) throw new RuntimeException(
                "Cannot use idr in functions."
        );
        return ResourceUtil.resolveRelativePath(scriptLocationStack.peek(), textForm, null);
    }

    public static InputStream readStream(ResourceLocation identifier) throws IOException {
#if MC_VERSION >= "11902"
        final List<Resource> resources = manager().getResourceStack(identifier);
#else
        final List<Resource> resources = manager().getResources(identifier);
#endif
        if (resources.isEmpty()) throw new FileNotFoundException(identifier.toString());
        return Utilities.getInputStream(resources.get(0));
    }

    public static boolean hasResource(ResourceLocation identifier) {
#if MC_VERSION >= "11902"
        final List<Resource> resources = manager().getResourceStack(identifier);
        return  resources != null && resources.size() > 0;
#else
        return manager().hasResource(identifier);
#endif
    }

    public static String readString(ResourceLocation identifier) {
        try {
            return ResourceUtil.readResource(manager(), identifier);
        } catch (IOException e) {
            return null;
        }
    }

    protected static final FontRenderContext FONT_CONTEXT = new FontRenderContext(new AffineTransform(), true, false);

    public static FontRenderContext getFontRenderContext() {
        return FONT_CONTEXT;
    }

    public static AttributedString ensureStrFonts(String text, Font font) {
        AttributedString result = new AttributedString(text);
        if (text.isEmpty()) return result;
        result.addAttribute(TextAttribute.FONT, font, 0, text.length());
        for (int characterIndex = 0; characterIndex < text.length(); characterIndex++) {
            final char character = text.charAt(characterIndex);
            if (!font.canDisplay(character)) {
                Font defaultFont = null;
                for (final Font testFont : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
                    if (testFont.canDisplay(character)) {
                        defaultFont = testFont;
                        break;
                    }
                }
                final Font newFont = (defaultFont == null ? new Font(null) : defaultFont)
                        .deriveFont(font.getStyle(), font.getSize2D());
                result.addAttribute(TextAttribute.FONT, newFont, characterIndex, characterIndex + 1);
            }
        }
        return result;
    }

    public static BufferedImage readBufferedImage(ResourceLocation identifier) throws IOException {
        try (InputStream is = readStream(identifier)) {
            return GraphicsTexture.createArgbBufferedImage(ImageIO.read(is));
        }
    }

    public static Font readFont(ResourceLocation identifier) throws IOException, FontFormatException {
        try (InputStream is = readStream(identifier)) {
            return Font.createFont(Font.TRUETYPE_FONT, is);
        }
    }

    public static int getParticleTypeId(ResourceLocation identifier) {
#if MC_VERSION >= "11903"
        Optional<ParticleType<?>> particleType = BuiltInRegistries.PARTICLE_TYPE.getOptional(identifier);
        return particleType.map(BuiltInRegistries.PARTICLE_TYPE::getId).orElse(-1);
#else
        Optional<ParticleType<?>> particleType = Registry.PARTICLE_TYPE.getOptional(identifier);
        return particleType.map(Registry.PARTICLE_TYPE::getId).orElse(-1);
#endif
    }

    public static CompoundTag parseNbtString(String text) throws CommandSyntaxException {
        return TagParser.parseTag(text);
    }

    public static String getMTRVersion() {
        String mtrModVersion;
        try {
            mtrModVersion = (String) mtr.Keys.class.getField("MOD_VERSION").get(null);
        } catch (ReflectiveOperationException ignored) {
            mtrModVersion = "0.0.0-0.0.0";
        }
        return mtrModVersion;
    }

    public static String getNTEVersion() {
        return BuildConfig.MOD_VERSION.split("\\-", 3)[0];
    }

    public static int getNTEVersionInt() {
        int[] components = Arrays.stream(BuildConfig.MOD_VERSION.split("\\-", 3)[0].split("\\+", 2)[0].split("\\.", 3))
                .mapToInt(Integer::parseInt).toArray();
        return components[0] * 10000 + components[1] * 100 + components[2];
    }

    public static int getNTEProtoVersion() {
        return BuildConfig.MOD_PROTOCOL_VERSION;
    }

    public static void resetComponents() {
        CustomResources.resetComponents();
    }

}

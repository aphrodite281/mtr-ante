package cn.zbx1425.mtrsteamloco.scripting.util;

import mtr.mappings.Text;
import net.minecraft.network.chat.Component;

public interface ComponentUtil{

    public static Component translatable(String key, Object... args) {
        return (Component) Text.translatable(key, args);
    }

    public static Component literal(String text) {
        return (Component) Text.literal(text);
    }

    public static String getString(Component component) {
        return component.getString();
    }
}
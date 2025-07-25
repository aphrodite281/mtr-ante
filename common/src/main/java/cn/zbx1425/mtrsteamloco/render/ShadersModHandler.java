package cn.zbx1425.mtrsteamloco.render;

import cn.zbx1425.sowcer.ContextCapability;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BooleanSupplier;

public final class ShadersModHandler {

    private static InternalHandler internalHandler;

    public static void init() {
        internalHandler = new InternalHandler() { };

        try {
            Class<?> ignored = Class.forName("optifine.Installer");
            internalHandler = new Optifine();
        } catch (Exception ignored) { }

        try {
            Class<?> ignored = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            internalHandler = new Iris();
        } catch (Exception ignored) { }
    }

    public static boolean canInstance() {
        return canUseCustomShader() && ContextCapability.supportVertexAttribDivisor;
    }

    public static boolean canUseCustomShader() {
        return !internalHandler.isShaderPackInUse() && !ContextCapability.isGL4ES;
    }

    public static boolean canDrawWithBuffer() {
        return !(internalHandler instanceof Optifine) || canUseCustomShader();
    }

    public static boolean isRenderingShadowPass() {
        return internalHandler.isRenderingShadowPass();
    }

    public static boolean isShaderPackInUse() {
        return internalHandler.isShaderPackInUse();
    }

    private interface InternalHandler {
        default boolean isShaderPackInUse() {
            return false;
        }
        default boolean isRenderingShadowPass() { return false; };
    }

    private static class Iris implements InternalHandler {
        private final BooleanSupplier shadersEnabledSupplier;
        private final BooleanSupplier isRenderingShadowPassSupplier;

        Iris() {
            shadersEnabledSupplier = createShadersEnabledSupplier();
            isRenderingShadowPassSupplier = createIsRenderingShadowPassSupplier();
        }

        @Override
        public boolean isShaderPackInUse() {
            return shadersEnabledSupplier.getAsBoolean();
        }

        private static BooleanSupplier createShadersEnabledSupplier() {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object irisApiInstance = irisApiClass.getMethod("getInstance").invoke(null);
                Method fnIsShaderPackInUse = irisApiClass.getMethod("isShaderPackInUse");
                return () -> {
                    try {
                        return (Boolean)fnIsShaderPackInUse.invoke(irisApiInstance);
                    } catch (Exception ignored) {
                        return false;
                    }
                };
            } catch (Exception ignored) {
                return () -> false;
            }
        }

        @Override
        public boolean isRenderingShadowPass() {
            return isRenderingShadowPassSupplier.getAsBoolean();
        }

        private static BooleanSupplier createIsRenderingShadowPassSupplier() {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Object irisApiInstance = irisApiClass.getMethod("getInstance").invoke(null);
                Method fnIsRenderingShadowPass = irisApiClass.getMethod("isRenderingShadowPass");
                return () -> {
                    try {
                        return (Boolean)fnIsRenderingShadowPass.invoke(irisApiInstance);
                    } catch (Exception ignored) {
                        return false;
                    }
                };
            } catch (Exception ignored) {
                return () -> false;
            }
        }
    }

    private static class Optifine implements InternalHandler {
        private final BooleanSupplier shadersEnabledSupplier;
        private final BooleanSupplier isRenderingShadowPassSupplier;

        Optifine() {
            shadersEnabledSupplier = createShadersEnabledSupplier();
            isRenderingShadowPassSupplier = createIsRenderingShadowPassSupplier();
        }

        @Override
        public boolean isShaderPackInUse() {
            return shadersEnabledSupplier.getAsBoolean();
        }

        private static BooleanSupplier createShadersEnabledSupplier() {
            try {
                Class<?> ofShaders = Class.forName("net.optifine.shaders.Shaders");
                Field field = ofShaders.getDeclaredField("activeProgramID");
                // field.setAccessible(true);
                return () -> {
                    try {
                        return (int)field.get(null) != 0;
                    } catch (IllegalAccessException ignored) {
                        return false;
                    }
                };
            } catch (Exception ignored) {
                return () -> false;
            }
        }

        @Override
        public boolean isRenderingShadowPass() {
            return isRenderingShadowPassSupplier.getAsBoolean();
        }

        private static BooleanSupplier createIsRenderingShadowPassSupplier() {
            try {
                Class<?> ofShaders = Class.forName("net.optifine.shaders.Shaders");
                Field field = ofShaders.getDeclaredField("isShadowPass");
                field.setAccessible(true);
                return () -> {
                    try {
                        return field.getBoolean(null);
                    } catch (IllegalAccessException ignored) {
                        return false;
                    }
                };
            } catch (Exception ignored) {
                return () -> false;
            }
        }
    }
}
package cn.zbx1425.mtrsteamloco.render;

import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class UniformQuery {
    private static int tick = 0;

    public static void queryAllUniforms(int program) {
        tick++;
        if (tick % 1000 != 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 获取活动Uniform数量
            IntBuffer numUniforms = stack.mallocInt(1);
            GL30.glGetProgramiv(program, GL30.GL_ACTIVE_UNIFORMS, numUniforms);

            // 获取最大名称长度
            IntBuffer maxNameLength = stack.mallocInt(1);
            GL30.glGetProgramiv(program, GL30.GL_ACTIVE_UNIFORM_MAX_LENGTH, maxNameLength);

            // 准备查询参数
            ByteBuffer nameBuffer = ByteBuffer.allocateDirect(maxNameLength.get(0));
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            IntBuffer length = stack.mallocInt(1);

            System.out.printf("%-20s %-6s %-20s %s%n", "Name", "Loc", "Type", "Size");
            System.out.println("---------------------------------------------------");

            for (int i = 0; i < numUniforms.get(0); i++) {
                nameBuffer.clear();
                
                // 获取Uniform信息
                GL30.glGetActiveUniform(
                    program, 
                    i, 
                    // maxNameLength.get(0), 
                    length, 
                    size, 
                    type, 
                    nameBuffer
                );

                // 提取名称并获取位置
                String name = getNullTerminatedString(nameBuffer);
                int location = GL30.glGetUniformLocation(program, name);

                // 转换类型枚举值为字符串
                String typeName = getTypeName(type.get(0));

                System.out.printf("%-20s %-6d %-20s %d%n", 
                    name, 
                    location,
                    typeName,
                    size.get(0));
            }
        }
    }

    private static String getNullTerminatedString(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == 0) break;
            sb.append((char) b);
        }
        return sb.toString();
    }

    // OpenGL类型枚举映射
    private static String getTypeName(int glType) {
        switch (glType) {
            case GL30.GL_FLOAT: return "float";
            case GL30.GL_FLOAT_VEC2: return "vec2";
            case GL30.GL_FLOAT_VEC3: return "vec3";
            case GL30.GL_FLOAT_VEC4: return "vec4";
            case GL30.GL_INT: return "int";
            case GL30.GL_INT_VEC2: return "ivec2";
            case GL30.GL_INT_VEC3: return "ivec3";
            case GL30.GL_INT_VEC4: return "ivec4";
            case GL30.GL_BOOL: return "bool";
            case GL30.GL_FLOAT_MAT2: return "mat2";
            case GL30.GL_FLOAT_MAT3: return "mat3";
            case GL30.GL_FLOAT_MAT4: return "mat4";
            case GL30.GL_SAMPLER_2D: return "sampler2D";
            case GL30.GL_SAMPLER_CUBE: return "samplerCube";
            default: return "0x" + Integer.toHexString(glType);
        }
    }
}
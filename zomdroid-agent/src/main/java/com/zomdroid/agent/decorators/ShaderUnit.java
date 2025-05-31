package com.zomdroid.agent.decorators;

import com.zomdroid.agent.glsl.GLSLCoreToESVisitor;
import com.zomdroid.agent.glsl.antlr.GLSLLexer;
import com.zomdroid.agent.glsl.antlr.GLSLParser;
import net.bytebuddy.asm.Advice;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;


/**
 * Decorator for {@code zombie.core.opengl.ShaderUnit},
 * primarily used to preprocess shaders for GLSL ES compatibility.
 * */
public class ShaderUnit {
    public static class loadShaderFile {
        @Advice.OnMethodExit
        public static void onExit(@Advice.Argument(1) ArrayList<String> additionalShadersList) {
            additionalShadersList.clear(); // additional shaders were already inlined via custom preProcessShaderFile
        }
    }

    public static class preProcessShaderFile {

        public static String preprocessForGLSLES(String code) {
            //System.out.println("preprocessForGLSLES \n" + code);
            CharStream input = CharStreams.fromString(code);
            GLSLLexer lexer = new GLSLLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            tokens.fill();
            GLSLParser parser = new GLSLParser(tokens);
            ParseTree tree = parser.translation_unit();
            GLSLCoreToESVisitor visitor = new GLSLCoreToESVisitor();
            visitor.visit(tree);
            StringBuilder result = new StringBuilder();
            int lastPos = 0;

            for (Token token : tokens.getTokens()) {
                if (token.getType() == Token.EOF) {
                    break;
                }

                int start = token.getStartIndex();
                int stop = token.getStopIndex() + 1;

                // append any intermediate text between last token and this one
                if (lastPos < start) {
                    result.append(code, lastPos, start);
                }

                String replacement = visitor.replacements.getOrDefault(token.getTokenIndex(), token.getText());

                result.append(replacement);

                lastPos = stop;
            }

            // append remaining trailing code after last token
            if (lastPos < code.length()) {
                result.append(code.substring(lastPos));
            }

            return result.toString();
        }

        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 0, readOnly = false) String shaderCode) {

        }

        @Advice.OnMethodExit
        public static void onExit(@Advice.Argument(value = 0, readOnly = false) String shaderPath,
                                  @Advice.Argument(1) ArrayList<String> additionalShadersList,
                                  @Advice.Return(readOnly = false) String shaderCode) {
            if (shaderPath.endsWith(".vert") || shaderPath.endsWith(".frag")) {
                System.out.println("Preprocessing for GLES: " + shaderPath);
                shaderCode = preprocessForGLSLES(shaderCode);
                additionalShadersList.clear(); // additional shaders were already hardcoded in verts and frags in custom processIncludeLine
            }
        }
    }

    /**
     * By default, PZ replaces {@code #include X} directives in shaders by inserting function declarations from {@code X.h},
     * and adds the corresponding {@code X.glsl} files to a list of additional shader units to be linked into the final program.
     *
     * Since GL4ES only supports linking two shader units (1 vertex, 1 fragment), we will inline the function
     * definitions from {@code X.glsl} directly into the shader source instead, to avoid relying on separate shader units.
     */
    public static class processIncludeLine {
        public static final Class<?> clazz;
        public static final Method preProcessShaderFileMethod;

        static {
            try {
                clazz = Class.forName("zombie.core.opengl.ShaderUnit");
                preProcessShaderFileMethod = clazz.getDeclaredMethod("preProcessShaderFile", String.class, ArrayList.class);
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        @Advice.OnMethodEnter
        public static void onEnter(@Advice.Argument(value = 1, readOnly = false) StringBuilder shaderCodeBuilder,
                                   @Advice.Argument(4) ArrayList<String> additionalShadersList,
                                   @Advice.Local("additionalShadersCount") int additionalShadersCount
                                   ) {
            additionalShadersCount = additionalShadersList.size();
        }

        @Advice.OnMethodExit
        public static void onExit(@Advice.This Object thiz,
                                  @Advice.Argument(0) String shaderPath,
                                  @Advice.Argument(1) StringBuilder shaderCodeBuilder,
                                  @Advice.Argument(2) String includeLine,
                                  @Advice.Argument(3) String lineSeparator,
                                  @Advice.Argument(4) ArrayList<String> additionalShadersList,
                                  @Advice.Return boolean success,
                                  @Advice.Local("additionalShadersCount") int additionalShadersCount
        ) {
            if (!success) return; // if game reports something is wrong with the line, we don't process it

            System.out.println("Preprocessing for GLES: " + shaderPath + " - include directive " + includeLine);

            if (additionalShadersList.size() == additionalShadersCount) return; // no new files were added

            String additionalShaderPath = additionalShadersList.get(additionalShadersList.size() - 1); // last added shader file is what we need

            try {
                String includePath = additionalShaderPath + ".glsl";
                String additionalShaderCode = (String) preProcessShaderFileMethod.invoke(thiz, includePath, additionalShadersList);
                additionalShaderCode = additionalShaderCode.replaceFirst("(?m)^\\s*#version\\s+.*\\R?", ""); // remove version directive, since we are inlining
                shaderCodeBuilder.append(additionalShaderCode);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

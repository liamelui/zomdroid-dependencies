package com.zomdroid.agent;

import com.zomdroid.agent.decorators.ShaderUnit;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.pool.TypePool;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class Main {
    private static ClassLoader classLoader = Main.class.getClassLoader();
    private static TypePool typePool = TypePool.Default.of(classLoader);
    private static ClassFileLocator locator = ClassFileLocator.ForClassLoader.of(classLoader);
    public static void premain(String args, Instrumentation inst) {
        System.out.println("Hello from zomdroid agent");

        if (args == null)
            args = "";

        String[] argsArray = args.split(",");
        for (String arg: argsArray) {

        }

        String renderer = System.getProperty("zomdroid.renderer");
        boolean isGL4ES = renderer.equals("GL4ES");

        try {
            if (isGL4ES) {
                new ByteBuddy().with(TypeValidation.DISABLED)
                        .rebase(typePool.describe("zombie.core.opengl.ShaderUnit").resolve(), locator)
                        .visit(Advice.to(ShaderUnit.loadShaderFile.class).on(named("loadShaderFile")))
                        .visit(Advice.to(ShaderUnit.preProcessShaderFile.class).on(named("preProcessShaderFile")))
                        .visit(Advice.to(ShaderUnit.processIncludeLine.class).on(named("processIncludeLine")))
                        .make()
                        .load(classLoader, ClassReloadingStrategy.of(inst));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
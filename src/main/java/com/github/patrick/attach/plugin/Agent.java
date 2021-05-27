package com.github.patrick.attach.plugin;

import kotlin.jvm.functions.Function0;
import net.bytebuddy.asm.Advice;

public class Agent {
    public static Function0<?> _prefix;
    public static Function0<?> _postfix;

    public Agent(Function0<?> var1, Function0<?> var2) {
        _prefix = var1;
        _postfix = var2;
    }

    @Advice.OnMethodEnter
    public static void enter() {
        _prefix.invoke();
    }

    @Advice.OnMethodExit
    public static void exit() {
        _postfix.invoke();
    }
}

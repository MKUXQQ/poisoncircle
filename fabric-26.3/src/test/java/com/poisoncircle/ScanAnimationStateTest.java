package com.poisoncircle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ScanAnimationStateTest {
    @TempDir Path classes;

    @Test
    void requiresServerCompletionAndReleaseBeforeAnotherScan() throws Exception {
        // Client sources are intentionally absent from Loom's common test classpath.
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classes.toString(),
            "src/client/java/com/poisoncircle/ScanAnimationState.java"));
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, null)) {
            Class<?> type = loader.loadClass("com.poisoncircle.ScanAnimationState");
            Object state = type.getConstructor().newInstance();
            var tick = type.getMethod("tick", boolean.class, boolean.class);
            var active = type.getMethod("active");
            var completed = type.getMethod("completed");
            var lock = type.getMethod("shouldLockMovement", boolean.class, boolean.class);
            for (int i = 0; i < 140; i++) tick.invoke(state, true, true);
            assertEquals(true, active.invoke(state), "local five seconds cannot grant completion");
            assertEquals(false, completed.invoke(state));
            assertEquals(true, lock.invoke(state, true, true));
            assertEquals(false, lock.invoke(state, true, false), "physical movement resumes on release");
            float wrist = (float) type.getMethod("wrist", float.class).invoke(state, 0.5F);
            assertTrue(Float.isFinite(wrist) && Math.abs(wrist) <= 0.055F);
            tick.invoke(state, false, true);
            assertEquals(false, active.invoke(state), "invalid target immediately cancels");
            tick.invoke(state, true, true);
            type.getMethod("complete").invoke(state);
            assertEquals(false, active.invoke(state));
            tick.invoke(state, true, true);
            assertEquals(false, active.invoke(state), "holding after reveal cannot restart");
            assertEquals(true, lock.invoke(state, true, true), "completed hold still locks movement");
            tick.invoke(state, false, true);
            tick.invoke(state, true, true);
            assertEquals(false, active.invoke(state), "looking away cannot bypass release latch");
            tick.invoke(state, false, false);
            tick.invoke(state, true, true);
            assertEquals(true, active.invoke(state), "release and press starts another scan");
            tick.invoke(state, true, false);
            assertEquals(false, active.invoke(state), "release cancels");
        }
    }
}

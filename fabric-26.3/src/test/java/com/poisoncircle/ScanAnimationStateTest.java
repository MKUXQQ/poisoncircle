package com.poisoncircle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ScanAnimationStateTest {
    @TempDir Path classes;

    @Test
    void inputMixinUsesClientInputSuperclassInsteadOfShadowingInheritedFields() throws Exception {
        String source = Files.readString(Path.of("src/client/java/com/poisoncircle/mixin/ScanInputLockMixin.java"));
        assertTrue(source.contains("extends ClientInput"));
        assertFalse(source.contains("@Shadow"), "KeyboardInput does not declare the inherited input fields");
        assertTrue(source.contains("keyPresses = Input.EMPTY"));
        assertTrue(source.contains("moveVector = Vec2.ZERO"));
        String mouse = Files.readString(Path.of("src/client/java/com/poisoncircle/mixin/ScanMouseLockMixin.java"));
        assertTrue(mouse.contains("turnPlayer(D)V"), "26.3 turnPlayer takes one double");
        assertTrue(mouse.contains("ScanDeviceClient.shouldLockMovement()"));
    }

    @Test
    void usesServerProgressAndRequiresReleaseAfterEveryEnd() throws Exception {
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", classes.toString(),
            "src/client/java/com/poisoncircle/ScanAnimationState.java"));
        try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{classes.toUri().toURL()}, null)) {
            Class<?> type = loader.loadClass("com.poisoncircle.ScanAnimationState");
            Object state = type.getConstructor().newInstance();
            var tick = type.getMethod("tick", boolean.class);
            var sync = type.getMethod("sync", boolean.class, int.class);
            var active = type.getMethod("active");
            var completed = type.getMethod("completed");
            var progress = type.getMethod("progressTicks");
            var age = type.getMethod("age", float.class);
            sync.invoke(state, true, 32);
            for (int i = 0; i < 140; i++) tick.invoke(state, true);
            assertEquals(32, progress.invoke(state), "client elapsed time never advances the progress bar");
            assertEquals(36F, age.invoke(state, 0.5F), "interpolation is bounded to the server heartbeat interval");
            assertEquals(true, active.invoke(state), "local five seconds cannot grant completion");
            sync.invoke(state, true, 60);
            assertEquals(60.5F, age.invoke(state, 0.5F), "new server progress resynchronizes animation age");
            float wrist = (float) type.getMethod("wrist", float.class).invoke(state, 0.5F);
            assertTrue(Float.isFinite(wrist) && Math.abs(wrist) <= 0.20F);
            for (int endingProgress : new int[]{100, 20}) {
                sync.invoke(state, false, endingProgress);
                assertEquals(false, active.invoke(state));
                tick.invoke(state, true);
                assertEquals(true, completed.invoke(state), "both success and cancellation latch until release");
                tick.invoke(state, false);
                assertEquals(false, completed.invoke(state));
                sync.invoke(state, true, 0);
                assertEquals(true, active.invoke(state));
            }
            sync.invoke(state, true, 900);
            assertEquals(100, progress.invoke(state));
            sync.invoke(state, true, -10);
            assertEquals(0, progress.invoke(state));
        }
    }
}

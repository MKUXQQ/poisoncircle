package com.poisoncircle;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScanSessionPayloadTest {
    @Test void codecPreservesStartProgressAndEndIncludingFailure() {
        for (ScanSessionPayload payload : new ScanSessionPayload[] {
            new ScanSessionPayload(42, new BlockPos(-3, 64, 9), true, 0, -90, 41.5F),
            new ScanSessionPayload(42, new BlockPos(-3, 64, 9), true, 48, -90, 41.5F),
            new ScanSessionPayload(42, new BlockPos(-3, 64, 9), false, 100, -90, 41.5F),
            new ScanSessionPayload(42, new BlockPos(-3, 64, 9), false, 12, -90, 41.5F)
        }) {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                ScanSessionPayload.STREAM_CODEC.encode(buffer, payload);
                assertEquals(payload, ScanSessionPayload.STREAM_CODEC.decode(buffer));
                assertEquals(0, buffer.readableBytes());
                assertEquals("poisoncircle:scanner_session", payload.type().id().toString());
            } finally {
                buffer.release();
            }
        }
    }
}

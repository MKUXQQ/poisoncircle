package com.poisoncircle;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoundDurationSettingsTest {
    @Test
    void savesIndividualRoundDurationsForFutureCircles() throws Exception {
        Class<?> type = Class.forName("com.poisoncircle.RoundDurationSettings");
        Object settings = type.getConstructor(int.class, int.class).newInstance(5, 120);
        Method setTimes = type.getMethod("setTimes", int.class, int.class, int.class);
        Method secondsForRound = type.getMethod("secondsForRound", int.class);
        Method copyWaitTicks = type.getMethod("copyWaitTicks");
        Method copyShrinkTicks = type.getMethod("copyShrinkTicks");
        setTimes.invoke(settings, 2, 120, 10);
        setTimes.invoke(settings, 5, 30, 20);
        assertEquals(120, secondsForRound.invoke(settings, 1));
        assertEquals(10, secondsForRound.invoke(settings, 2));
        assertEquals(20, secondsForRound.invoke(settings, 5));
        assertEquals(2400, ((int[]) copyWaitTicks.invoke(settings))[1]);
        assertEquals(200, ((int[]) copyShrinkTicks.invoke(settings))[1]);
    }
}

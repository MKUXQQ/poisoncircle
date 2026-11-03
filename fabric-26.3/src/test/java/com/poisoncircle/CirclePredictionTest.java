package com.poisoncircle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CirclePredictionTest {
    @Test
    void advancesCenterAndRadiusBetweenServerSyncs() {
        CirclePrediction.State state = new CirclePrediction.State(0, 0, 100, 40, 20, 50, 100);

        CirclePrediction.State predicted = CirclePrediction.advance(state, 25);

        assertEquals(10, predicted.x(), 0.0001);
        assertEquals(5, predicted.z(), 0.0001);
        assertEquals(87.5, predicted.radius(), 0.0001);
        assertEquals(75, predicted.remainingTicks());
    }

    @Test
    void keepsTheCircleFixedDuringItsWaitingTime() {
        CirclePrediction.State state = new CirclePrediction.State(0, 0, 100, 40, 20, 50, 100);

        CirclePrediction.State predicted = CirclePrediction.advance(state, 25, false);

        assertEquals(state, predicted);
    }
}

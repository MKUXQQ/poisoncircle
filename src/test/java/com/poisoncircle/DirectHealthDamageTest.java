package com.poisoncircle;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectHealthDamageTest {
    @Test
    void reducesHealthWithoutAnyArmorCalculation() throws Exception {
        Class<?> type = Class.forName("com.poisoncircle.DirectHealthDamage");
        Method remaining = type.getMethod("remainingHealth", float.class, double.class);
        assertEquals(14.5F, (Float) remaining.invoke(null, 20.0F, 5.5D));
        assertEquals(0.0F, (Float) remaining.invoke(null, 3.0F, 8.0D));
    }

    @Test
    void identifiesTheHitThatMustUseTheVanillaDeathPipeline() {
        assertTrue(DirectHealthDamage.isLethal(3.0F, 3.0D));
        assertFalse(DirectHealthDamage.isLethal(3.0F, 2.99D));
    }
}

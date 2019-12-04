package de.upb.codingpirates.battleships.server.gui.controllers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public final class ConfigurationControllerTest {

    @CsvSource({
        "0,   A",
        "1,   B",
        "2,   C",
        "3,   D",
        "4,   E",
        /* ... */
        "21,  V",
        "22,  W",
        "23,  X",
        "24,  Y",
        "25,  Z",
        "26, AA",
        "27, AB",
        "28, AC",
        "29, AD",
        "30, AE"
    })
    @ParameterizedTest
    public void testToShipTypeLabel(final int n, final String expected) {
        assertEquals(ConfigurationController.toShipTypeLabel(n), expected);
    }
}

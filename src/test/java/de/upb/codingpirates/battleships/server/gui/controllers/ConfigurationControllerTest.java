package de.upb.codingpirates.battleships.server.gui.controllers;

import java.util.Set;
import java.util.stream.Stream;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.server.gui.controllers.ConfigurationController.ShipTypeConfiguration;

import static com.google.common.collect.Sets.newHashSet;

import static org.junit.jupiter.api.Assertions.*;

public final class ConfigurationControllerTest {

    @CsvSource({
        "0,   A",
        "1,   B",
        "2,   C",
        "3,   D",
        "4,   E",
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

    public static final class ShipTypeConfigurationTest {

        // <editor-fold desc="testCheckMarksConnected">
        @MethodSource
        @ParameterizedTest
        public void testCheckMarksConnected(@NotNull final ShipTypeConfiguration toTest, final boolean actual) {
            assertEquals(toTest.checkMarksConnected(), actual);
        }

        @NotNull
        @Contract(pure = true)
        @SuppressWarnings("unused")
        public static Stream<Arguments> testCheckMarksConnected() {
            return Stream.of(
                Arguments.of(
                    new ShipTypeConfiguration(
                        "A",
                        newHashSet()),
                    true),
                Arguments.of(
                    new ShipTypeConfiguration(
                        "B",
                        newHashSet(new Point2D(0, 0))),
                    true),
                Arguments.of(
                    new ShipTypeConfiguration(
                        "C",
                        newHashSet(
                            new Point2D(0, 0),
                            new Point2D(1, 1))),
                    false),
                Arguments.of(
                    new ShipTypeConfiguration(
                        "D",
                        newHashSet(
                            new Point2D(0, 0),
                            new Point2D(1, 0))),
                    true));
        }
        // </editor-fold>

        // <editor-fold desc="testNormalize">
        @MethodSource
        @ParameterizedTest
        public void testNormalize(final Set<Point2D> points, final Set<Point2D> normalized) {
            assertEquals(normalized, ShipTypeConfiguration.normalize(points));
        }

        @NotNull
        @Contract(pure = true)
        public static Stream<Arguments> testNormalize() {
            return Stream.of(
                Arguments.of(
                    newHashSet(),
                    newHashSet()),
                Arguments.of(
                    newHashSet(new Point2D(1, 1)),
                    newHashSet(new Point2D(0, 0))),
                Arguments.of(
                    newHashSet(new Point2D(0, 1)),
                    newHashSet(new Point2D(0, 0))),
                Arguments.of(
                    newHashSet(new Point2D(1, 0)),
                    newHashSet(new Point2D(0, 0))),
                Arguments.of(
                    newHashSet(
                        new Point2D(0, 0),
                        new Point2D(0, 1)),
                    newHashSet(
                        new Point2D(0, 0),
                        new Point2D(0, 1))));
        }
        // </editor-fold>
    }
}

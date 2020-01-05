package de.upb.codingpirates.battleships.server.gui.controllers;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;

import org.jetbrains.annotations.Contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.server.gui.controllers.ConfigurationValidator.InsufficientFieldSizeException;

import static org.junit.jupiter.api.Assertions.*;

/** @author Andre Blanke */
public final class BoundingBoxConfigurationValidatorTest {

    private ConfigurationValidator validator;

    @BeforeEach
    public void setUp() {
        validator = new BoundingBoxConfigurationValidator();
    }

    @Nonnull
    @Contract(pure = true)
    private static Stream<Arguments> testThrowsInsufficientFieldSizeException() {
        final Map<Integer, ShipType> ships0 = new IdentityHashMap<>();
        ships0.put(0, new ShipType(new Point2D(0, 0), new Point2D(0, 1), new Point2D(1, 0)));

        final Configuration configuration0 =
            new Configuration.Builder()
                .width(1)
                .height(2)
                .ships(ships0)
                .build();

        return Stream.of(Arguments.of(configuration0));
    }

    @MethodSource
    @ParameterizedTest
    public void testThrowsInsufficientFieldSizeException(final Configuration configuration) {
        assertThrows(InsufficientFieldSizeException.class, () -> validator.validate(configuration));
    }
}

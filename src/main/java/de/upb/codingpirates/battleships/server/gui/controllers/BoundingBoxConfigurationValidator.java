package de.upb.codingpirates.battleships.server.gui.controllers;

import java.util.Collection;

import javax.annotation.Nonnull;

import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.Point2D;
import de.upb.codingpirates.battleships.logic.ShipType;

import static java.util.Collections.max;
import static java.util.Comparator.comparingInt;

/* TODO: improve implementation */
/**
 * A {@link ConfigurationValidator} which checks for the validity of a {@link Configuration} instance by comparing the
 * configuration's field area to the sum of the areas of the bounding boxes of available {@link ShipType}s.
 *
 * @author Andre Blanke
 */
public final class BoundingBoxConfigurationValidator implements ConfigurationValidator {

    @Override
    public void validate(@Nonnull final Configuration configuration) throws InsufficientFieldSizeException {
        final int totalBoundingArea =
            configuration
                .getShips()
                .values()
                .stream()
                .mapToInt(BoundingBoxConfigurationValidator::getShipTypeBoundingBoxArea)
                .sum();
        if (totalBoundingArea > (configuration.getWidth() * configuration.getHeight())) {
            final int recommendedSize = (int) Math.sqrt(totalBoundingArea) + 1;

            throw new InsufficientFieldSizeException(recommendedSize);
        }
    }

    /**
     * Determines the area of the bounding box occupied by the provided {@link ShipType}.
     *
     * The bounding box is defined as the rectangle containing all the positions belonging to the {@code ShipType}.
     *
     * @param shipType The {@code ShipType} whose total bounding box area is to be calculated.
     *
     * @return The area of the bounding box associated with the provided {@code ShipType}.
     *
     * @throws IllegalStateException If the provided {@code ShipType}'s {@link ShipType#getPositions()} method returns
     *                               an empty collection.
     */
    private static int getShipTypeBoundingBoxArea(@Nonnull final ShipType shipType) {
        final Collection<Point2D> positions = shipType.getPositions();

        final int maxX = max(positions, comparingInt(Point2D::getX)).getX();
        final int maxY = max(positions, comparingInt(Point2D::getY)).getY();

        return ((maxX + 1) * (maxY + 1));
    }
}

package de.upb.codingpirates.battleships.server.gui.controllers;

import de.upb.codingpirates.battleships.logic.Configuration;

/**
 * Denotes types which are able to check whether a given {@link Configuration} instance is valid or not via the
 * {@link #validate(Configuration)} method.
 *
 * @implNote Implementations are expected to throw fitting {@link RuntimeException}s in the {@code validate} method,
 *           or the provided (checked) {@link InsufficientFieldSizeException} to indicate that the playing field of
 *           a provided {@code Configuration} does not provide enough room for all the ships.
 *
 * @author Andre Blanke
 *
 * @see Configuration
 */
public interface ConfigurationValidator {

    /**
     * Checks whether the provided {@link Configuration} is valid according to this implementation.
     *
     * @param configuration The {@code Configuration} which is to be checked for validity.
     *
     * @throws InsufficientFieldSizeException Thrown to indicate that the provided {@code Configuration}'s playing field
     *                                        has an insufficient size.
     */
    void validate(Configuration configuration) throws InsufficientFieldSizeException;

    /**
     * An exception thrown to indicate that a {@link Configuration}'s associated dimensions do not provide enough room
     * for fitting all {@link de.upb.codingpirates.battleships.logic.ShipType}s.
     */
    final class InsufficientFieldSizeException extends Exception {

        private final int recommendedSize;

        InsufficientFieldSizeException(final int recommendedSize) {
            if (recommendedSize <= 0)
                throw new IllegalArgumentException("recommendedSize must be a positive integer.");
            this.recommendedSize = recommendedSize;
        }

        int getRecommendedSize() {
            return recommendedSize;
        }
    }
}

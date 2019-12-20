package de.upb.codingpirates.battleships.server.util;

import com.google.common.collect.Lists;
import de.upb.codingpirates.battleships.logic.BoundingBox;
import de.upb.codingpirates.battleships.logic.Configuration;
import de.upb.codingpirates.battleships.logic.ShipType;
import de.upb.codingpirates.battleships.server.exceptions.InvalidGameSizeException;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public interface ConfigurationChecker {

    /**
     * checks if the ships can fit into the field
     *
     * @param configuration the configuration which should be checked
     * @throws InvalidGameSizeException if it does not fits
     */
    default void checkField(@Nonnull Configuration configuration) throws InvalidGameSizeException {//TODO better algorithm
        Collection<ShipType> ships = configuration.getShips().values();

        List<BoundingBox> boxes = Lists.newArrayList();

        for (ShipType ship : ships) {
            int x = ship.getPositions().stream().max((a, b) -> Math.max(Math.abs(a.getX()), Math.abs(b.getX()))).get().getX();
            int y = ship.getPositions().stream().max((a, b) -> Math.max(Math.abs(a.getY()), Math.abs(b.getY()))).get().getY();
            boxes.add(new BoundingBox(x + 1, y + 1));
        }

        int maxFields = boxes.stream().mapToInt(BoundingBox::getSize).sum();
        if (maxFields > configuration.getHeight() * configuration.getWidth()) {
            throw new InvalidGameSizeException(configuration.getHeight() * configuration.getWidth(),(int) Math.sqrt(maxFields));
        }
    }
}

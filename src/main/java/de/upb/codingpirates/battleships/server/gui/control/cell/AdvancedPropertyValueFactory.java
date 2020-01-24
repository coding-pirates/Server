package de.upb.codingpirates.battleships.server.gui.control.cell;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javafx.beans.NamedArg;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;

import com.sun.javafx.property.PropertyReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A convenience implementation of the {@link Callback} interface designed specifically for use within the
 * {@link javafx.scene.control.TableColumn}
 * {@link javafx.scene.control.TableColumn#cellValueFactoryProperty() cell value factory}.
 *
 * This class is basically an extension to {@link javafx.scene.control.cell.PropertyValueFactory}
 * which allows access to nested properties {@code similar.to.this}, which would end up emulating
 * {@code getSimilar().getTo().getThis()} (and possible variants, such as {@code is} instead of {@code get}
 * in case of a boolean) on the item contained within the row.
 *
 * @param <S> The type of the class contained within the TableView.items list.
 * @param <T> The type of the class contained within the TableColumn cells.
 *
 * @see javafx.scene.control.TableColumn
 * @see javafx.scene.control.TableView
 * @see javafx.scene.control.TableCell
 * @see javafx.scene.control.cell.MapValueFactory
 * @see javafx.scene.control.cell.PropertyValueFactory
 * @see javafx.scene.control.cell.TreeItemPropertyValueFactory
 *
 * @author Andre Blanke
 */
public final class AdvancedPropertyValueFactory<S, T> implements Callback<CellDataFeatures<S, T>, ObservableValue<T>> {

    /* We cache the PropertyReference, as otherwise performance suffers when working with large data models. */
    private PropertyReference<?>[] propertyRefs;

    @Nonnull
    private final String propertyAccessor;

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Creates a default {@code AdvancedPropertyValueFactory} to extract the value from a given
     * {@link javafx.scene.control.TableRow} item reflectively, supporting nested properties.
     *
     * @param propertyAccessor The dot-separated names of the properties with which to attempt to reflectively
     *                         extract a corresponding value for in a given object.
     */
    public AdvancedPropertyValueFactory(@NamedArg("propertyAccessor") @Nonnull final String propertyAccessor) {
        if (propertyAccessor.isEmpty())
            throw new IllegalArgumentException();
        this.propertyAccessor = propertyAccessor;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public ObservableValue<T> call(@Nonnull final CellDataFeatures<S, T> cellDataFeatures) {
        return getCellDataReflectively(cellDataFeatures.getValue());
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private ObservableValue<T> getCellDataReflectively(@Nullable final S rowData) {
        if (rowData == null)
            return null;

        try {
            if (propertyRefs == null) {
                Class<?> currentClass = rowData.getClass();

                final String[] properties = getPropertyAccessor().split("\\.");
                propertyRefs = new PropertyReference[properties.length];

                for (int i = 0; i < properties.length; ++i) {
                    propertyRefs[i] = new PropertyReference<>(currentClass, properties[i]);
                    currentClass = propertyRefs[i].getType();
                }
            }

            Object currentObject = rowData;
            for (int i = 0; i < propertyRefs.length; ++i) {
                final PropertyReference<?> ref = propertyRefs[i];

                if (i == propertyRefs.length - 1) {
                    if (ref.hasProperty())
                        return (ReadOnlyProperty<T>) ref.getProperty(currentObject);
                    else
                        return new ReadOnlyObjectWrapper<>((T) ref.get(currentObject));
                }
                currentObject = ref.get(currentObject);
            }
        } catch (final IllegalStateException exception) {
            /* Log the warning and move on. */
            LOGGER.warn("Can not retrieve property '{}' in AdvancedPropertyValueFactory.", getPropertyAccessor());
        }
        /* Should be unreachable. */
        return null;
    }

    /** Returns the {@code propertyAccessor} provided in the constructor. */
    @Nonnull
    public String getPropertyAccessor() {
        return propertyAccessor;
    }
}

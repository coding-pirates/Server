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

public final class AdvancedPropertyValueFactory<S, T> implements Callback<CellDataFeatures<S, T>, ObservableValue<T>> {

    private PropertyReference<?>[] propertyRefs;

    @Nonnull
    private final String propertyAccessor;

    public AdvancedPropertyValueFactory(@NamedArg("propertyAccessor") @Nonnull final String propertyAccessor) {
        if (propertyAccessor.isEmpty())
            throw new IllegalArgumentException();
        this.propertyAccessor = propertyAccessor;
    }

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

        if (propertyRefs == null) {
            Class<?> currentClass = rowData.getClass();

            final String[] properties = propertyAccessor.split("\\.");
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
                    return new ReadOnlyObjectWrapper<T>((T) ref.get(currentObject));
            }
            currentObject = ref.get(currentObject);
        }
        /* Unreachable. */
        return null;
    }

    @Nonnull
    public String getPropertyAccessor() {
        return propertyAccessor;
    }
}

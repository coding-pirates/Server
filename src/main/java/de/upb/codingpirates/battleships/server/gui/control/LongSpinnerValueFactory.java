package de.upb.codingpirates.battleships.server.gui.control;

import javafx.beans.NamedArg;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.StringConverter;
import javafx.util.converter.LongStringConverter;

import org.jetbrains.annotations.Contract;

/**
 * @author Andre Blanke
 */
public class LongSpinnerValueFactory extends SpinnerValueFactory<Long> {

    private LongProperty min = new SimpleLongProperty(this, "min") {
        @Override
        protected void invalidated() {
            final Long currentValue = LongSpinnerValueFactory.this.getValue();

            if (currentValue == null)
                return;

            long newMin = get();
            if (newMin > getMax()) {
                setMin(getMax());
                return;
            }

            if (currentValue < newMin)
                LongSpinnerValueFactory.this.setValue(newMin);
        }
    };

    private LongProperty max = new SimpleLongProperty(this, "max") {
        @Override
        protected void invalidated() {
            final Long currentValue = LongSpinnerValueFactory.this.getValue();

            if (currentValue == null)
                return;

            long newMax = get();
            if (newMax < getMin()) {
                setMax(getMin());
                return;
            }

            if (currentValue > newMax)
                LongSpinnerValueFactory.this.setValue(newMax);
        }
    };

    private LongProperty amountToStepBy = new SimpleLongProperty(this, "amountToStepBy");

    public LongSpinnerValueFactory(@NamedArg("min")          final long min,
                                   @NamedArg("max")          final long max,
                                   @NamedArg("initialValue") final long initialValue,
                                   @NamedArg("converter")    final StringConverter<Long> converter) {
        this(min, max, initialValue, 1, converter);
    }

    // <editor-fold desc="Default constructors">
    public LongSpinnerValueFactory(@NamedArg("min") final long min,
                                   @NamedArg("max") final long max) {
        this(min, max, min);
    }

    public LongSpinnerValueFactory(@NamedArg("min")          final long min,
                                   @NamedArg("max")          final long max,
                                   @NamedArg("initialValue") final long initialValue) {
        this(min, max, initialValue, 1);
    }

    public LongSpinnerValueFactory(@NamedArg("min")            final long min,
                                   @NamedArg("max")            final long max,
                                   @NamedArg("initialValue")   final long initialValue,
                                   @NamedArg("amountToStepBy") final long amountToStepBy) {
        this(min, max, initialValue, amountToStepBy, new LongStringConverter());
    }

    public LongSpinnerValueFactory(@NamedArg("min")            final long min,
                                   @NamedArg("max")            final long max,
                                   @NamedArg("initialValue")   final long initialValue,
                                   @NamedArg("amountToStepBy") final long amountToStepBy,
                                   @NamedArg("converter")      final StringConverter<Long> converter) {
        setMin(min);
        setMax(max);
        setAmountToStepBy(amountToStepBy);
        setConverter(converter);

        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue < getMin())
                setValue(getMin());
            else if (newValue > getMax())
                setValue(getMax());
        });
        setValue(initialValue >= min && initialValue <= max ? initialValue : min);
    }
    // </editor-fold>

    /**
     * Convenience method to support wrapping values around their min/max constraints.
     *
     * Adopted from the source code of {@link javafx.scene.control.Spinner#wrapValue(int, int, int)}
     * which is both package private and does not support the {@code long} data type.
     */
    @Contract(pure = true)
    private static long wrapValue(final long value, final long min, final long max) {
        if (max == 0)
            throw new RuntimeException();

        long rest = value % max;

        if ((rest > min && max < min) || (rest < min && max > min))
            return rest + max - min;
        return rest;
    }

    /** {@inheritDoc} */
    @Override
    public void decrement(final int steps) {
        final long min      = getMin();
        final long max      = getMax();
        final long newIndex = getValue() - steps * getAmountToStepBy();

        setValue(newIndex >= min ? newIndex : (isWrapAround() ? wrapValue(newIndex, min, max) + 1 : min));
    }

    /** {@inheritDoc} */
    @Override
    public void increment(final int steps) {
        final long min      = getMin();
        final long max      = getMax();
        final long newIndex = getValue() + steps * getAmountToStepBy();

        setValue(newIndex <= max ? newIndex : (isWrapAround() ? wrapValue(newIndex, min, max) + 1 : min));
    }

    public final long getMin() {
        return min.get();
    }

    public final void setMin(final long value) {
        min.set(value);
    }

    @Contract(pure = true)
    public final LongProperty min() {
        return min;
    }

    public final long getMax() {
        return max.get();
    }

    public final void setMax(final long value) {
        max.set(value);
    }

    @Contract(pure = true)
    public final LongProperty max() {
        return max;
    }

    public final long getAmountToStepBy() {
        return amountToStepBy.get();
    }

    public final void setAmountToStepBy(final long value) {
        amountToStepBy.set(value);
    }

    @Contract(pure = true)
    public final LongProperty amountToStepBy() {
        return amountToStepBy;
    }
}

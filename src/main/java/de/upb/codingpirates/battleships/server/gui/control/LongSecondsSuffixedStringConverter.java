package de.upb.codingpirates.battleships.server.gui.control;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.util.StringConverter;

import org.jetbrains.annotations.NotNull;

/**
 * @author Andre Blanke
 */
public final class LongSecondsSuffixedStringConverter extends StringConverter<Long> {

    private static final Pattern SECONDS_SUFFIX_PATTERN = Pattern.compile("^(?<seconds>[1-9][0-9]*)s$");

    @Override
    public String toString(final Long value) {
        return String.format("%ds", value);
    }

    @NotNull
    @Override
    public Long fromString(final String value) {
        final Matcher matcher = SECONDS_SUFFIX_PATTERN.matcher(value);

        //noinspection ResultOfMethodCallIgnored
        matcher.matches();

        return Long.valueOf(matcher.group("seconds"));
    }
}

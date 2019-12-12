package de.upb.codingpirates.battleships.server.gui.util;

import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.annotation.Nonnull;

/**
 * Wraps a {@link ResourceBundle} and delegates all lookups to a {@code ResourceBundle} instance
 * specified at instantiation time.
 *
 * This class exists as a workaround for a bug in OpenJFX 8's {@link javafx.fxml.FXMLLoader} implementation which
 * causes a {@link NullPointerException} to be thrown when specifying a {@code resource} attribute for an
 * {@code fx:include} tag in an FXML file.
 *
 * http://hg.openjdk.java.net/openjfx/8/master/rt/file/tip/modules/fxml/src/main/java/javafx/fxml/FXMLLoader.java#l1093
 *
 * The line in question, namely line 1093, uses {@code FXMLLoader.this.resources.getClass().getClassLoader()} to get
 * the {@link ClassLoader} instance which was used to load the {@link ResourceBundle} class in order to pass it to
 * {@link ResourceBundle#getBundle(String, Locale, ClassLoader)}.
 *
 * {@link ResourceBundle#getBundle(String, Locale, ClassLoader)} expects a not-{@code null} {@code ClassLoader} and
 * correctly throws a {@code NullPointerException}, however, according to the documentation
 * {@link Class#getClassLoader()} may return {@code null} to represent the bootstrap class loader, which is the case
 * here.
 * The {@code FXMLLoader} fails to handle that case and expects the return value of {@link Class#getClassLoader()}
 * to be not-{@code null} and passed it to {@link ResourceBundle#getBundle(String, Locale, ClassLoader)},
 * causing the {@code NullPointerException}.
 *
 * @author Andre Blanke
 */
public final class ResourceBundleWrapper extends ResourceBundle {

    private final ResourceBundle bundle;

    public ResourceBundleWrapper(@Nonnull final ResourceBundle bundle) {
        this.bundle = bundle;
    }

    @Nonnull
    @Override
    protected Object handleGetObject(@Nonnull final String key) {
        return bundle.getObject(key);
    }

    @Nonnull
    @Override
    public Enumeration<String> getKeys() {
        return bundle.getKeys();
    }
}

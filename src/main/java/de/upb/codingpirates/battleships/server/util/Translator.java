package de.upb.codingpirates.battleships.server.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * implement this class to translate text
 */
public interface Translator {
    ResourceBundle resourceBundle = ResourceBundle.getBundle(String.format("lang.%s", "game"), Locale.GERMAN);//TODO language

    default String translate(String string){
        try {
            return resourceBundle.getString(string);
        }catch (MissingResourceException e){
            return string;
        }
    }
}

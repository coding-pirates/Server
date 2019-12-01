package de.upb.codingpirates.battleships.server.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public interface Translator {
    ResourceBundle resourceBundle = ResourceBundle.getBundle(String.format("lang.%s", "game"), Locale.US);//TODO language

    default String translate(String string){
        try {
            return resourceBundle.getString(string);
        }catch (MissingResourceException e){
            return string;
        }
    }
}

package de.upb.codingpirates.battleships.server.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class ServerMarker {

    public static final Marker CLIENT = MarkerManager.getMarker("Client Handling");
    public static final Marker CONNECTION = MarkerManager.getMarker("Connection Handling");
    public static final Marker GAME = MarkerManager.getMarker("Game Handling");
    public static final Marker INGAME = MarkerManager.getMarker("In Game");
    public static final Marker HANDLER = MarkerManager.getMarker("Message Handler");

}

package de.upb.codingpirates.battleships.server.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TestProperties {

    public static final int playerCount = 2;
    public static final String hostAddress;

    static {
        String host = null;
        try {
            host = InetAddress.getLocalHost().getHostAddress();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        hostAddress = host;
    }
}

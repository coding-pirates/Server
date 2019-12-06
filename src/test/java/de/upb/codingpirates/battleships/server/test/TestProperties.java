package de.upb.codingpirates.battleships.server.test;

import de.upb.codingpirates.battleships.network.Properties;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

class TestProperties {
    //you can edit following attributes for testing
    static final int playerCount = 2;
    static final boolean simple = true;
    /**
     * set this address if you want to test on a server with different ip
     */
    private static final String preferedHostAddress = null;



    //do not edit
    static final String hostAddress;
    static final boolean isServerOnline;



    static {
        //noinspection ConstantConditions
        if(preferedHostAddress != null){
            hostAddress = preferedHostAddress;
        }else {
            String host;
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new IllegalStateException("No HostAddress available");
            }
            hostAddress = host;
        }
        AtomicBoolean online = new AtomicBoolean(false);
        try {
            Thread testConnection = new Thread(()-> {
                try {
                    online.set(new Socket(hostAddress, Properties.PORT).isConnected());
                } catch (IOException ignored) {
                }
            });
            testConnection.start();
            long timer = System.currentTimeMillis();
            while (timer > System.currentTimeMillis() - 100){

            }
            testConnection.stop();
            online.set(new Socket(hostAddress, Properties.PORT).isConnected());
        } catch (IOException ignored) {
        }


        isServerOnline = online.get();
    }
}

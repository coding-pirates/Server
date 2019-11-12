package de.upb.codingpirates.battleships.server;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerMain {

    private static final Logger LOGGER = LogManager.getLogger();
    public static void main(String[] args) {
        LOGGER.info("start");//just for information purpose
        LOGGER.error("error",new NullPointerException());//print error in try & catch without failing the programm
        LOGGER.warn("warn");//this might be a problem
        LOGGER.debug("testing");//just for debugging purpose & in dev environment
    }
}

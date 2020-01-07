package de.upb.codingpirates.battleships.server.test;

import de.upb.codingpirates.battleships.logic.Client;
import de.upb.codingpirates.battleships.logic.ClientType;
import de.upb.codingpirates.battleships.network.exceptions.game.InvalidActionException;
import de.upb.codingpirates.battleships.network.exceptions.game.NotAllowedException;
import de.upb.codingpirates.battleships.server.exceptions.InvalidGameSizeException;
import de.upb.codingpirates.battleships.server.game.TournamentHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

public class TournamentTest extends ServerTest{
    private static final Logger LOGGER = LogManager.getLogger();

    @Test
    void tournamentTest() throws InvalidGameSizeException, NotAllowedException, InvalidActionException {
        serverStart();
        long timer = System.currentTimeMillis();
        while (timer > System.currentTimeMillis() - 1000){
        }
        assert gameManager != null;
        assert clientManager != null;
        assert tournamentManager != null;
        TournamentHandler handler = tournamentManager.createTournament(TEST_CONFIG,"test");
        for(int i = 0; i< TestProperties.tournamentPlayer;i++) {
            tournamentManager.addClientToTournament(handler.getTournamentId(), new Client(i,"t"+i), ClientType.PLAYER);
        }
        handler.start();
        LOGGER.debug(handler.getGames().size());
        assert handler.getGames().size() == (int)(((float)TestProperties.tournamentPlayer / (float)TEST_CONFIG.getMaxPlayerCount())+0.5f);
    }
}

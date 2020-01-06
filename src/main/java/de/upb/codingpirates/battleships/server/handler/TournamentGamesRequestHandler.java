package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.TournamentGamesRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class TournamentGamesRequestHandler extends AbstractServerMessageHandler<TournamentGamesRequest> {
    @Inject
    public TournamentGamesRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager) {
        super(clientManager, gameManager, TournamentGamesRequest.class);
    }

    @Override
    protected void handleMessage(TournamentGamesRequest message, Id connectionId) throws GameException {

    }
}

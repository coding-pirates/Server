package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.TournamentPointsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;

public final class TournamentPointsRequestHandler extends AbstractServerMessageHandler<TournamentPointsRequest> {
    public TournamentPointsRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager) {
        super(clientManager, gameManager, TournamentPointsRequest.class);
    }

    @Override
    protected void handleMessage(TournamentPointsRequest message, Id connectionId) throws GameException {

    }
}

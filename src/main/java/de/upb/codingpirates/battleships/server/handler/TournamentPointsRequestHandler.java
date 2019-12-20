package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.TournamentPointsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.TournamentManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class TournamentPointsRequestHandler extends AbstractServerMessageHandler<TournamentPointsRequest> {
    @Nonnull
    private final TournamentManager tournamentManager;

    @Inject
    public TournamentPointsRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager, @Nonnull TournamentManager tournamentManager) {
        super(clientManager, gameManager, TournamentPointsRequest.class);
        this.tournamentManager = tournamentManager;
    }

    @Override
    protected void handleMessage(TournamentPointsRequest message, Id connectionId) throws GameException {

    }
}

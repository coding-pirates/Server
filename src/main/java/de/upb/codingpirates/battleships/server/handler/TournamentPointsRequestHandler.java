package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.TournamentPointsRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.TournamentManager;
import de.upb.codingpirates.battleships.server.game.TournamentHandler;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * MessageHandler for {@link TournamentPointsRequest}
 */
public final class TournamentPointsRequestHandler extends AbstractServerMessageHandler<TournamentPointsRequest> {
    @Nonnull
    private final TournamentManager tournamentManager;

    @Inject
    public TournamentPointsRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager, @Nonnull TournamentManager tournamentManager) {
        super(clientManager, gameManager, TournamentPointsRequest.class);
        this.tournamentManager = tournamentManager;
    }

    @Override
    protected void handleMessage(TournamentPointsRequest message, @Nonnull Id connectionId) throws GameException {
        TournamentHandler handler = this.tournamentManager.getTournamentByClient(connectionId.getInt());
        this.clientManager.sendMessageToAll(ResponseBuilder.tournamentPointsResponse(handler.getScore()));
    }
}

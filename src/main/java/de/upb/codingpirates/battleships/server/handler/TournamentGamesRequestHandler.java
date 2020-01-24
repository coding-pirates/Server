package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.TournamentGamesRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.TournamentManager;
import de.upb.codingpirates.battleships.server.game.TournamentHandler;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * MessageHandler for {@link TournamentGamesRequest}
 */
public final class TournamentGamesRequestHandler extends AbstractServerMessageHandler<TournamentGamesRequest> {

    @Nonnull
    private final TournamentManager tournamentManager;

    @Inject
    public TournamentGamesRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager, @Nonnull TournamentManager tournamentManager) {
        super(clientManager, gameManager, TournamentGamesRequest.class);
        this.tournamentManager = tournamentManager;
    }

    @Override
    protected void handleMessage(TournamentGamesRequest message, @Nonnull Id connectionId) throws GameException {
        TournamentHandler handler = this.tournamentManager.getTournamentByClient(connectionId.getInt());
        this.clientManager.sendMessage(ResponseBuilder.tournamentGamesResponse(handler.getGames()), connectionId);
    }
}

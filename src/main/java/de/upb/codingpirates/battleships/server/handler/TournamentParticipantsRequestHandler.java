package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.TournamentParticipantsRequest;
import de.upb.codingpirates.battleships.network.message.response.ResponseBuilder;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;
import de.upb.codingpirates.battleships.server.TournamentManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class TournamentParticipantsRequestHandler extends AbstractServerMessageHandler<TournamentParticipantsRequest> {

    @Nonnull
    private final TournamentManager tournamentManager;

    @Inject
    public TournamentParticipantsRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager, @Nonnull TournamentManager tournamentManager) {
        super(clientManager, gameManager, TournamentParticipantsRequest.class);
        this.tournamentManager = tournamentManager;
    }

    @Override
    protected void handleMessage(TournamentParticipantsRequest message, Id connectionId) {
        this.clientManager.sendMessageToId(ResponseBuilder.tournamentParticipantsResponse(this.tournamentManager.isParticipating(connectionId.getInt())), connectionId);
    }
}

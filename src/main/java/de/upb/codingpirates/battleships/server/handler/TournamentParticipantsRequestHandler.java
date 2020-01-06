package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.TournamentParticipantsRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class TournamentParticipantsRequestHandler extends AbstractServerMessageHandler<TournamentParticipantsRequest> {

    @Inject
    public TournamentParticipantsRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager) {
        super(clientManager, gameManager, TournamentParticipantsRequest.class);
    }

    @Override
    protected void handleMessage(TournamentParticipantsRequest message, Id connectionId) throws GameException {

    }
}

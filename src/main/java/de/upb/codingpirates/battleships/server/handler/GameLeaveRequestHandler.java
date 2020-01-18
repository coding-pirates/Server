package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.request.GameLeaveRequest;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public final class GameLeaveRequestHandler extends AbstractServerMessageHandler<GameLeaveRequest> {

    @Inject
    public GameLeaveRequestHandler(@Nonnull ClientManager clientManager, @Nonnull GameManager gameManager) {
        super(clientManager, gameManager, GameLeaveRequest.class);
    }

    @Override
    protected void handleMessage(GameLeaveRequest message, Id connectionId) throws GameException {

    }
}

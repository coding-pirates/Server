package de.upb.codingpirates.battleships.client.handler;

import de.upb.codingpirates.battleships.network.exceptions.game.GameException;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.MessageHandler;
import de.upb.codingpirates.battleships.network.message.request.GameJoinSpectatorRequest;
import de.upb.codingpirates.battleships.network.message.response.GameJoinSpectatorResponse;

public class GameJoinSpectatorResponseHandler implements MessageHandler<GameJoinSpectatorResponse> {
    @Override
    public void handle(GameJoinSpectatorResponse message, Id connectionId) throws GameException {

    }

    @Override
    public boolean canHandle(Message message) {
        return message instanceof GameJoinSpectatorRequest;
    }
}

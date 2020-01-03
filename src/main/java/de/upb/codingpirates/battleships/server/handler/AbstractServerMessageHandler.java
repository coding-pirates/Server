package de.upb.codingpirates.battleships.server.handler;

import de.upb.codingpirates.battleships.network.message.Message;
import de.upb.codingpirates.battleships.network.message.handler.ExceptionMessageHandler;
import de.upb.codingpirates.battleships.network.message.handler.MessageHandler;
import de.upb.codingpirates.battleships.server.ClientManager;
import de.upb.codingpirates.battleships.server.GameManager;

import javax.annotation.Nonnull;

/**
 * Common subclass of all server-side {@link MessageHandler}s.
 *
 * @author Andre Blanke
 */
public abstract class AbstractServerMessageHandler<T extends Message> extends ExceptionMessageHandler<T> {

    @Nonnull
    private final Class<T> messageType;

    @Nonnull
    protected final ClientManager clientManager;

    @Nonnull
    protected final GameManager gameManager;

    protected AbstractServerMessageHandler(@Nonnull final ClientManager clientManager,
                                           @Nonnull final GameManager   gameManager,
                                           @Nonnull final Class<T>      messageType) {
        this.clientManager = clientManager;
        this.gameManager   = gameManager;

        this.messageType = messageType;
    }

    @Override
    public final boolean canHandle(@Nonnull final Message message) {
        return messageType.isInstance(message);
    }
}

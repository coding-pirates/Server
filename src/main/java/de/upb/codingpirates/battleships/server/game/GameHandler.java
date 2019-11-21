package de.upb.codingpirates.battleships.server.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.upb.codingpirates.battleships.network.id.Id;
import de.upb.codingpirates.battleships.network.id.IntId;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class GameHandler {
    private final Game game;
    private final Map<Id, Client> player = Maps.newHashMap();
    private final Map<Id, Client> spectator = Maps.newHashMap();

    public GameHandler(Configuration configuration, String name, Id id) {
        this.game = new Game(name, (Integer) id.getRaw(), 0, GameState.LOBBY, configuration);
    }

    public void addClient(Client client, ClientType clientType) {
        if (!player.containsKey(client.getId()) && !spectator.containsKey(client.getId())) {
            switch (clientType) {
                case PLAYER:
                    player.putIfAbsent(new IntId(client.getId()), client);
                    break;
                case SPECTATOR:
                    spectator.putIfAbsent(new IntId(client.getId()), client);
                    break;
            }
        }
    }

    public boolean isClientAllowed(Id client, @Nullable ClientType clientType) {
        boolean player = true;
        boolean spectator = true;
        if (!ClientType.SPECTATOR.equals(clientType))
            player = this.player.containsKey(client);
        if (!ClientType.PLAYER.equals(clientType))
            spectator = this.spectator.containsKey(client);
        return player && spectator;
    }

    public void removeClient(Client client) {
        player.remove(client.getId());
        spectator.remove(client.getId());
    }

    public List<Client> getAllClients() {
        List<Client> clients = Lists.newArrayList();
        clients.addAll(this.player.values());
        clients.addAll(this.spectator.values());
        return clients;
    }

    public void run() {

    }
}

package de.bytemc.network.cluster;

import de.bytemc.network.cluster.impl.ClusteringConnectedClient;
import de.bytemc.network.cluster.types.NetworkType;
import de.bytemc.network.master.IServer;
import de.bytemc.network.master.cache.IConnectedClient;
import de.bytemc.network.packets.IPacket;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface INode extends IServer {

    default List<IConnectedClient> getAllClientsByType(NetworkType type) {
        return this.getAllCachedConnectedClients()
            .stream()
            .filter(it -> it instanceof ClusteringConnectedClient)
            .map(it -> ((ClusteringConnectedClient) it))
            .filter(it -> it.getNetworkType().equals(type))
            .collect(Collectors.toList());
    }

    default Optional<IConnectedClient> getClient(String name) {
        return this.getAllCachedConnectedClients().stream()
            .filter(client -> client.getName().equals(name)).findAny();
    }

    default List<IConnectedClient> getAllServices() {
        return this.getAllClientsByType(NetworkType.SERVICE);
    }

    default List<IConnectedClient> getAllNodes() {
        return this.getAllClientsByType(NetworkType.NODE);
    }

    default int getAmountOfConnectedNodes() {
        return this.getAllNodes().size();
    }

    default int getAmountOfConnectedServices() {
        return this.getAllNodes().size();
    }

    default void sendPacketToType(final IPacket packet, NetworkType type) {
       this.getAllClientsByType(type).forEach(it -> it.sendPacket(packet));
    }

}

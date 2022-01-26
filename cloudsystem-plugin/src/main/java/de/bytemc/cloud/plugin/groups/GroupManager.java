package de.bytemc.cloud.plugin.groups;

import de.bytemc.cloud.api.CloudAPI;
import de.bytemc.cloud.api.groups.impl.AbstractGroupManager;
import de.bytemc.cloud.api.network.packets.group.ServiceGroupCacheUpdatePacket;

public class GroupManager extends AbstractGroupManager {

    public GroupManager() {
        CloudAPI.getInstance().getNetworkHandler().registerPacketListener(ServiceGroupCacheUpdatePacket.class, (ctx, packet) -> {
            System.out.println("packet packet packet");
            setAllCachedServiceGroups(packet.getGroups());
        });
    }
}

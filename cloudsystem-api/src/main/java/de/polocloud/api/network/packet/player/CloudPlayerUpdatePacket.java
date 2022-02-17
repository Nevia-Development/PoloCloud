package de.polocloud.api.network.packet.player;

import de.polocloud.api.CloudAPI;
import de.polocloud.api.event.player.CloudPlayerUpdateEvent;
import de.polocloud.api.player.ICloudPlayer;
import de.polocloud.api.service.IService;
import de.polocloud.network.packet.IPacket;
import de.polocloud.network.packet.NetworkByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class CloudPlayerUpdatePacket implements IPacket {

    private UUID uuid;
    private IService server;
    private CloudPlayerUpdateEvent.UpdateReason updateReason;

    public CloudPlayerUpdatePacket(@NotNull ICloudPlayer cloudPlayer, @NotNull CloudPlayerUpdateEvent.UpdateReason updateReason) {
        this.uuid = cloudPlayer.getUniqueId();
        this.server = cloudPlayer.getServer();
        this.updateReason = updateReason;
    }

    @Override
    public void write(NetworkByteBuf byteBuf) {
        byteBuf.writeUUID(this.uuid);
        byteBuf.writeString(this.server.getName());
        byteBuf.writeEnum(this.updateReason);
    }

    @Override
    public void read(NetworkByteBuf byteBuf) {
        this.uuid = byteBuf.readUUID();
        this.server = CloudAPI.getInstance().getServiceManager().getServiceByNameOrNull(byteBuf.readString());
        this.updateReason = byteBuf.readEnum();
    }

}
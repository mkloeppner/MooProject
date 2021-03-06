package de.superioz.moo.proxy.listeners;

import de.superioz.moo.api.database.DatabaseType;
import de.superioz.moo.api.util.Validation;
import de.superioz.moo.client.Moo;
import de.superioz.moo.network.common.MooCache;
import de.superioz.moo.network.common.MooPlayer;
import de.superioz.moo.network.packet.PacketAdapter;
import de.superioz.moo.network.packet.PacketHandler;
import de.superioz.moo.network.packets.PacketUpdatePermission;
import de.superioz.moo.network.queries.MooQueries;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

public class PacketUpdatePermissionListener implements PacketAdapter {

    @PacketHandler
    public void onUpdatePermission(PacketUpdatePermission packet) {
        DatabaseType type = packet.type;
        String key = packet.key;

        if(type == DatabaseType.PLAYER) {
            // update ones player permission
            UUID uuid = Validation.UNIQUEID.matches(key)
                    ? UUID.fromString(key)
                    : (ProxyServer.getInstance().getPlayer(key) == null ? null : ProxyServer.getInstance().getPlayer(key).getUniqueId());
            if(uuid != null) {
                Moo.getInstance().runAsync((Runnable) () -> MooQueries.getInstance().updatePermission(uuid));
            }
        }
        else if(type == DatabaseType.GROUP) {
            // update every player in this group
            for(ProxiedPlayer player : ProxyServer.getInstance().getPlayers()){
                UUID uuid = player.getUniqueId();
                MooPlayer mooPlayer = MooCache.getInstance().getPlayerMap().get(uuid);

                if(mooPlayer.nexists() || !mooPlayer.getGroup().getName().equals(key)) continue;
                Moo.getInstance().runAsync((Runnable) () -> MooQueries.getInstance().updatePermission(uuid));
            }
        }
    }

}

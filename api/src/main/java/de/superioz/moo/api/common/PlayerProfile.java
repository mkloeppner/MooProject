package de.superioz.moo.api.common;

import de.superioz.moo.api.database.objects.Ban;
import de.superioz.moo.api.database.objects.PlayerData;
import de.superioz.moo.api.utils.ReflectionUtil;
import de.superioz.moo.api.utils.StringUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class of every database information we can get from a player
 * (ban and data)
 *
 * @see PlayerData
 * @see Ban
 */
@AllArgsConstructor
@Getter
public class PlayerProfile {

    /**
     * The playerdata from the database
     */
    private PlayerData data;

    /**
     * His current ban (if he isn't ban = null)
     */
    @Setter
    private Ban currentBan;

    /**
     * Every archived ban of this player
     */
    private List<Ban> archivedBans;

    /**
     * Gets the playername
     *
     * @return The name as string
     */
    public String getName() {
        return data == null ? "" : data.getLastName();
    }

    /**
     * Converts the packetData into the playerInfo (from playerInfo request packets)
     *
     * @param packetData The packets data
     * @return The playerInfo object
     */
    public static PlayerProfile fromPacketData(List<String> packetData) {
        // player data
        PlayerData data = null;
        String playerDataString = packetData.get(0);
        if(!playerDataString.isEmpty()) {
            data = ReflectionUtil.deserialize(playerDataString, PlayerData.class);
        }

        // ban status
        String currentBan = packetData.get(1);
        Ban ban = null;
        if(!currentBan.isEmpty()) {
            ban = ReflectionUtil.deserialize(currentBan, Ban.class);
        }

        // former bans
        String archivedBansString = packetData.get(2);
        List<Ban> archivedBans = new ArrayList<>();
        if(!archivedBansString.isEmpty()) {
            for(String s : archivedBansString.split(StringUtil.SEPERATOR_2)) {
                archivedBans.add(ReflectionUtil.deserialize(s, Ban.class));
            }
        }
        return new PlayerProfile(data, ban, archivedBans);
    }

}

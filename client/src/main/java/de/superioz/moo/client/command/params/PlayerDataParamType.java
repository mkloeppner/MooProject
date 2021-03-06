package de.superioz.moo.client.command.params;

import de.superioz.moo.api.command.param.ParamType;
import de.superioz.moo.api.database.objects.PlayerData;
import de.superioz.moo.api.util.Validation;
import de.superioz.moo.network.queries.MooQueries;

import java.util.UUID;

public class PlayerDataParamType extends ParamType<PlayerData> {

    @Override
    public String label() {
        return "playerdata";
    }

    @Override
    public PlayerData resolve(String s) {
        return Validation.UNIQUEID.matches(s)
                ? MooQueries.getInstance().getPlayerData(UUID.fromString(s))
                : MooQueries.getInstance().getPlayerData(s);
    }

    @Override
    public Class<PlayerData> typeClass() {
        return PlayerData.class;
    }

    @Override
    public boolean checkCustom(String arg, String s) {
        return true;
    }

    @Override
    public String handleCustomException(String s) {
        return null;
    }
}

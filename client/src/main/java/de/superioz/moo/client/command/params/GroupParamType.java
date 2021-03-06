package de.superioz.moo.client.command.params;

import de.superioz.moo.api.command.param.ParamType;
import de.superioz.moo.api.database.objects.Group;
import de.superioz.moo.network.common.MooProxy;

public class GroupParamType extends ParamType<Group> {

    @Override
    public String label() {
        return "group";
    }

    @Override
    public Group resolve(String s) {
        return MooProxy.getGroup(s).unwrap();
    }

    @Override
    public Class<Group> typeClass() {
        return Group.class;
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

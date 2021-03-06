package de.superioz.moo.proxy.commands.perm;

import de.superioz.moo.api.collection.PageableList;
import de.superioz.moo.api.command.Command;
import de.superioz.moo.api.command.CommandInstance;
import de.superioz.moo.api.command.help.ArgumentHelp;
import de.superioz.moo.api.command.help.ArgumentHelper;
import de.superioz.moo.api.command.param.ParamSet;
import de.superioz.moo.api.command.tabcomplete.TabCompletion;
import de.superioz.moo.api.command.tabcomplete.TabCompletor;
import de.superioz.moo.api.common.GroupPermission;
import de.superioz.moo.api.common.RunAsynchronous;
import de.superioz.moo.api.console.format.InfoListFormat;
import de.superioz.moo.api.console.format.PageableListFormat;
import de.superioz.moo.api.database.object.DataResolver;
import de.superioz.moo.api.database.objects.Group;
import de.superioz.moo.api.database.query.DbQuery;
import de.superioz.moo.api.database.query.DbQueryNode;
import de.superioz.moo.api.io.LanguageManager;
import de.superioz.moo.api.util.Validation;
import de.superioz.moo.api.utils.StringUtil;
import de.superioz.moo.network.common.MooGroup;
import de.superioz.moo.network.common.MooProxy;
import de.superioz.moo.network.queries.ResponseStatus;
import de.superioz.moo.proxy.command.BungeeCommandContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunAsynchronous
public class GroupCommand {

    private static final String LABEL = "group";
    private static final String INFO_COMMAND = "info";
    private static final String LIST_COMMAND = "list";
    private static final String LIST_PERM_COMMAND = "listperm";
    private static final String ADD_PERM_COMMAND = "addperm";
    private static final String REMOVE_PERM_COMMAND = "remperm";
    private static final String CLEAR_PERM_COMMAND = "clearperm";
    private static final String MODIFY_COMMAND = "modify";
    private static final String CREATE_COMMAND = "create";
    private static final String DELETE_COMMAND = "delete";

    @ArgumentHelp
    public void onArgumentHelp(ArgumentHelper helper) {
        // subcommands
        helper.react(0, Collections.singletonList(
                LanguageManager.get("available-subcommands",
                        StringUtil.getListToString(helper.getContext().getCommand().getChildrens(), ", ",
                                CommandInstance::getLabel))
        ), LABEL);

        // groups
        helper.react(0, Collections.singletonList(
                LanguageManager.get("available-groups",
                        StringUtil.getListToString(MooProxy.getGroups(), ", ", MooGroup::getName))
        ), INFO_COMMAND, MODIFY_COMMAND, DELETE_COMMAND);

        // update syntax
        helper.react(1, Arrays.asList(
                LanguageManager.get("key-value-updates-syntax"),
                LanguageManager.get("available-fields",
                        StringUtil.getListToString(DataResolver.getResolvableFields(Group.class), ", ",
                                field -> "&f" + field.getName() + "&7")),
                LanguageManager.get("available-operators",
                        StringUtil.getListToString(DbQueryNode.Type.values(), ", ",
                                operator -> "&f" + operator.toString() + "&7"))
        ), MODIFY_COMMAND, CREATE_COMMAND);
    }

    @TabCompletion
    public void onTabComplete(TabCompletor completor) {
        // subcommands
        completor.reactSubCommands(LABEL);

        // groups
        completor.react(2, StringUtil.getStringList(MooProxy.getGroups(),
                MooGroup::getName
        ), INFO_COMMAND, MODIFY_COMMAND, CREATE_COMMAND, DELETE_COMMAND);
    }

    @Command(label = LABEL, usage = "<subCommand>")
    public void onCommand(BungeeCommandContext context, ParamSet args) {
    }

    @Command(label = LIST_COMMAND, parent = LABEL, usage = "[page]", flags = {"h"})
    public void list(BungeeCommandContext context, ParamSet args) {
        // list the page
        int page = args.getInt(0, 0);

        // list the ordered pageable list and check page
        // if flag 'h' exists order the groups after the rank in descending order
        PageableList<MooGroup> pageableList = new PageableList<>(MooProxy.getGroups(),
                args.hasFlag("h") ? MooGroup.HIRARCHY_COMPARISON : MooGroup.DEFAULT_COMPARISON);

        // sends the pageable list with page as list format
        context.sendDisplayFormat(new PageableListFormat<MooGroup>(pageableList)
                .page(page).doesntExist("error-page-doesnt-exist")
                .emptyList("group-list-empty").header("group-list-header").emptyEntry("group-list-entry-empty")
                .entryFormat("group-list-entry").entry(replacor -> replacor.accept(replacor.get().getName(), replacor.get().getRank()))
                .footer("group-list-next-page", page + 1)
        );
    }

    @Command(label = INFO_COMMAND, parent = LABEL, usage = "<name>")
    public void info(BungeeCommandContext context, ParamSet args) {
        String groupName = args.get(0);
        MooGroup group = MooProxy.getGroup(groupName);
        context.invalidArgument(group.nexists(), "group-doesnt-exist", groupName);

        // send info
        context.sendDisplayFormat(new InfoListFormat().header("group-info-header", groupName).entryFormat("group-info-entry")
                .entry("group-info-entry-name", groupName)
                .entryc("group-info-entry-permissions", group.getPermissions().size() != 0,
                        group.getPermissions().size(), groupName)
                .entryc("group-info-entry-parents", group.getParents().size() != 0,
                        group.getParents().size(), StringUtil.getListToString(group.getParents(), "\n", s -> "&8- &7" + s))
                .entry("group-info-entry-prefix", group.getPrefix())
                .entry("group-info-entry-suffix", group.getSuffix())
                .entry("group-info-entry-color", group.getColor())
                .entry("group-info-entry-tabprefix", group.getTabPrefix())
                .entry("group-info-entry-tabsuffix", group.getTabSuffix())
        );
    }

    @Command(label = MODIFY_COMMAND, parent = LABEL, usage = "<name> <updates>")
    public void modify(BungeeCommandContext context, ParamSet args) {
        String groupName = args.get(0);
        MooGroup group = MooProxy.getGroup(groupName);
        context.invalidArgument(!group.exists(), "group-doesnt-exist", groupName);

        // list updates (for modification)
        String rawParam = args.get(1);
        DbQuery updates = DbQuery.fromParameter(Group.class, rawParam);

        // execute modification
        context.sendMessage("group-modify-load", groupName);
        ResponseStatus status = group.modify(updates);
        context.sendMessage(status.isOk(), "group-modify-complete-success", status)
                .or("group-modify-complete-failure", status);
    }

    @Command(label = CREATE_COMMAND, parent = LABEL, usage = "<name> [updates]")
    public void create(BungeeCommandContext context, ParamSet args) {
        String groupName = args.get(0);
        MooGroup group = MooProxy.getGroup(groupName);
        context.invalidArgument(group.exists(), "group-already-exists", groupName);

        // if group not exists create it
        // apply updates (optional)
        if(args.size() > 1) {
            String rawParam = args.get(1);
            DbQuery updates = DbQuery.fromParameter(Group.class, rawParam);
            group.lazyLock().modify(updates);
        }

        // execute creation
        context.sendMessage("group-create-load", groupName);
        ResponseStatus status = group.init(groupName).create();
        context.sendMessage(status.isOk(), "group-create-complete-success", status)
                .or("group-create-complete-failure", status);
    }

    @Command(label = DELETE_COMMAND, parent = LABEL, usage = "<name>")
    public void delete(BungeeCommandContext context, ParamSet args) {
        String groupName = args.get(0);
        MooGroup group = MooProxy.getGroup(groupName);
        context.invalidArgument(!group.exists(), "group-doesnt-exist", groupName);

        // execute deletion
        context.sendMessage("group-delete-load", groupName);
        ResponseStatus status = group.delete();
        context.sendMessage(status.isOk(), "group-delete-complete-success", status)
                .or("group-delete-complete-failure", status);
    }

    /*
    =====================
    PERMISSION
    =====================
     */

    @Command(label = LIST_PERM_COMMAND, parent = LABEL, usage = "<name> [page]")
    public void listperm(BungeeCommandContext context, ParamSet args) {
        String groupName = args.get(0);
        MooGroup group = MooProxy.getGroup(groupName);
        context.invalidArgument(!group.exists(), "group-doesnt-exist", groupName);

        // get list of perms
        int page = args.getInt(1, 0);
        PageableList<GroupPermission> pageableList = new PageableList<>(group.getGroupPermissions(), 10);

        // display em
        context.sendDisplayFormat(new PageableListFormat<GroupPermission>(pageableList)
                .page(page).doesntExist("error-page-doesnt-exist")
                .emptyList("permission-list-empty").header("permission-list-header")
                .emptyEntry("permission-list-entry-empty")
                .entryFormat("permission-list-entry")
                .entry(replacor -> {
                    GroupPermission groupPermission = replacor.get();
                    String prefix = groupPermission.isProxied() ? "&bb" : (groupPermission.isStar() ? "&9*" : "&es");

                    replacor.accept(prefix, groupPermission.getPerm()
                            .replace("*", "&9*&7")
                            .replace("-", "&c-&7"));
                })
                .footer("permission-list-next-page", "/group listperms " + groupName + (page + 1))
        );
    }

    @Command(label = ADD_PERM_COMMAND, parent = LABEL, usage = "<name> <permission>")
    public void addperm(BungeeCommandContext context, ParamSet args) {
        String groupName = args.get(0);
        MooGroup group = MooProxy.getGroup(groupName);
        context.invalidArgument(group.nexists(), "group-doesnt-exist", groupName);

        // list permissions from arguments to be added
        String rawArg = args.get(1);
        List<String> argPermissions = StringUtil.find(Validation.PERMISSION.getRawRegex(), rawArg);
        context.invalidArgument(argPermissions.isEmpty(), "permission-format-invalid");

        // set permissions
        context.sendMessage("permission-add-load");
        ResponseStatus status = group.addPermission(argPermissions);
        context.sendMessage(status.isOk(), "permission-add-complete-success", status)
                .or("permission-add-complete-failure", status);
    }

    @Command(label = REMOVE_PERM_COMMAND, parent = LABEL, usage = "<name> <permission>")
    public void remperm(BungeeCommandContext context, ParamSet args) {
        String groupName = args.get(0);
        MooGroup group = MooProxy.getGroup(groupName);
        context.invalidArgument(group.nexists(), "group-doesnt-exist", groupName);

        // list permissions from arguments to be added
        String rawArg = args.get(1);
        List<String> argPermissions = StringUtil.find(Validation.PERMISSION.getRawRegex(), rawArg);
        context.invalidArgument(argPermissions.isEmpty(), "permission-format-invalid");

        // set permissions
        context.sendMessage("permission-remove-load");
        ResponseStatus status = group.removePermission(argPermissions);
        context.sendMessage(status.isOk(), "permission-remove-complete-success", status)
                .or("permission-remove-complete-failure", status);
    }

    @Command(label = CLEAR_PERM_COMMAND, parent = LABEL, usage = "<name>")
    public void clearperm(BungeeCommandContext context, ParamSet args) {
        String groupName = args.get(0);
        MooGroup group = context.get(MooGroup.class, () -> MooProxy.getGroup(groupName));
        context.invalidArgument(group.nexists(), "group-doesnt-exist", groupName);

        // choice
        context.simpleChoice("permission-clear-ask", groupName);

        // set permissions
        context.sendMessage("permission-clear-load");
        ResponseStatus status = group.clearPermission();
        context.sendMessage(status.isOk(), "permission-remove-complete-success", status)
                .or("permission-remove-complete-failure", status);
    }

}

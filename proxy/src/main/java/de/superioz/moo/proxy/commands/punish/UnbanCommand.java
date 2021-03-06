package de.superioz.moo.proxy.commands.punish;

import de.superioz.moo.api.command.Command;
import de.superioz.moo.api.command.help.ArgumentHelp;
import de.superioz.moo.api.command.help.ArgumentHelper;
import de.superioz.moo.api.command.param.ParamSet;
import de.superioz.moo.api.command.tabcomplete.TabCompletion;
import de.superioz.moo.api.command.tabcomplete.TabCompletor;
import de.superioz.moo.api.common.RunAsynchronous;
import de.superioz.moo.api.database.objects.Ban;
import de.superioz.moo.api.database.objects.PlayerData;
import de.superioz.moo.api.io.LanguageManager;
import de.superioz.moo.api.utils.StringUtil;
import de.superioz.moo.network.queries.MooQueries;
import de.superioz.moo.network.queries.ResponseStatus;
import de.superioz.moo.proxy.command.BungeeCommandContext;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@RunAsynchronous
public class UnbanCommand {

    private static final String LABEL = "unban";

    @ArgumentHelp
    public void onArgumentHelp(ArgumentHelper helper) {

    }

    @TabCompletion
    public void onTabComplete(TabCompletor completor) {
        completor.react(1, StringUtil.getStringList(
                ProxyServer.getInstance().getPlayers(), ProxiedPlayer::getDisplayName)
        );
    }

    @Command(label = LABEL, usage = "<player> [reason]")
    public void onCommand(BungeeCommandContext context, ParamSet args) {
        PlayerData data = args.get(0, PlayerData.class);
        context.invalidArgument(data == null, LanguageManager.get("error-player-doesnt-exist", args.get(0)));
        String playerName = args.get(0);

        // list ban
        Ban ban = MooQueries.getInstance().getBan(data.getUuid());
        context.invalidArgument(ban == null, LanguageManager.get("unban-player-isnt-banned", playerName));

        // list the reason (optional)
        String reason = args.getString(1, "");

        // unban and if unsuccessful then send message
        ResponseStatus status = MooQueries.getInstance().unban(ban);
        context.invalidArgument(status.isNok(), LanguageManager.get("unban-couldnt-execute", status));

        // send teamchat message or only direct to him
        /*if(Moo.getInstance().canTeamchat(context.getCommandSender())) {
            String target = Moo.getInstance().getColoredName(ban.getBanned()) + playerName;

            BanCategory banSubType = ban.getSubType();
            String executor = BungeeTeamChat.getInstance().getColoredName(context.getCommandSender());
            String typeColor = banSubType.getBanType() == BanType.GLOBAL ? "&c" : "&9";
            String start = TimeUtil.getFormat(ban.getStart());
            String end = TimeUtil.getFormat(ban.getStart() + ban.getDuration());

            BungeeTeamChat.getInstance().send(
                    LanguageManager.get("unban-successful-team",
                            target,
                            context.isConsole() ? CommandContext.CONSOLE_NAME : context.getCommandSender().getName(),
                            Arrays.asList("Former Ban",
                                    start,
                                    end,
                                    typeColor + reason,
                                    executor))
            );
        }
        else {
            context.sendMessage(LanguageManager.get("unban-successful", playerName));
        }*/
    }

}

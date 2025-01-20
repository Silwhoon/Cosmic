package client.command.commands.gm1;

import client.Client;
import client.command.Command;
import constants.id.NpcId;

public class DailyCommand extends Command {

    @Override
    public void execute(Client client, String[] params) {
        client.getAbstractPlayerInteraction().openNpc(NpcId.MAPLE_ADMINISTRATOR, "daily_reward");
    }
}

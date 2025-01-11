package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import constants.id.NpcId;

public class QuestRewardCommand extends Command {
    @Override
    public void execute(Client client, String[] params) {
        client.getAbstractPlayerInteraction().openNpc(NpcId.MAPLE_ADMINISTRATOR, "quest_reward");
    }
}

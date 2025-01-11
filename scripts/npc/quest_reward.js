/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
/* Quest Reward NPC script
	Handles providing players with various quest rewards based on how many completed quests they have.
 */

var status = -1;

// Adding rewards example:
//      [database id, reward item id, amount, required quest count]
//
// Notes:
// The database ID must be unique for this script to function as intended. This script only checks if the database ID is
// claimed, so if a player claims database ID 1 and the reward item ID is changed AFTER the reward has already been claimed
// then they will not be able to re-claim the new reward unless you also change the database ID.
var rewards = [
    [1, 2000000, 100, 0],   // 100 Red Potions for 0 quest completions
    [2, 2000001, 75, 1],    // 75 Orange Potions for 1 quest completions
    [3, 2000002, 75, 5],    // 75 White Potions for 5 quest completions
    [4, 2000003, 50, 5],    // 50 Blue Potions for 5 quest completions
    [5, 2000003, 50, 10],   // 50 Blue Potions for 10 quest completions
    [6, 2000003, 50, 100],  // 50 Blue Potions for 100 quest completions
];

function start() {
    action(1, 0, 0);
}

function action(mode, type, selection) {
    if (mode === -1) {
        cm.dispose();
    } else {
        if (mode === 0 && type > 0) {
            cm.dispose();
            return;
        }
        if (mode === 1) {
            status++;
        } else {
            status--;
        }

        const CustomQuestManager = Java.type('server.quest.CustomQuestManager');
        var customQuestRewards = CustomQuestManager.getCustomQuestRewards(cm.getPlayer().getId());
        var completedQuests = cm.getPlayer().getCompletedQuests().size();

        if (status === 0) {
            var str = "Hello #b#h ##k, I can award you various items based on how many quests you've completed!\r\n\r\n";
            str += "You have currently completed #b" + completedQuests + "#k quest(s).\r\n\r\n";
            for (var i = 0; i < rewards.length; i++) {
                var rewardId = rewards[i][1];
                var rewardAmount = rewards[i][2];
                var rewardReq = rewards[i][3];
                var claimed = customQuestRewards.contains(rewards[i][0]);

                str += "#L" + i + "#";
                if (completedQuests >= rewardReq) {
                    if (claimed) {
                        str += "#g[" + rewardReq + " quests] Claimed #z" + rewardId + "# (x" + rewardAmount + ")#k\r\n";
                    } else {
                        str += "#b[" + rewardReq + " quests] Click to claim!#k\r\n";
                    }
                } else {
                    const leftToComplete = rewardReq - completedQuests;
                    str += "#r[" + rewardReq + " quests] Complete " + leftToComplete + " more to claim.#k\r\n";
                }
            }
            cm.sendSimple(str);
        } else if (status === 1) {
            const databaseId = rewards[selection][0];
            rewardId = rewards[selection][1];
            rewardAmount = rewards[selection][2];
            rewardReq = rewards[selection][3];
            claimed = customQuestRewards.contains(rewards[selection][0]);

            if (claimed) {
                cm.sendPrev("You have already claimed this reward!");
                return;
            }

            if (completedQuests < rewardReq) {
                const leftToComplete = rewardReq - completedQuests;
                cm.sendPrev("You haven't completed enough quests yet. You must complete #b" + leftToComplete + "#k more before claiming this reward.");
                return;
            }

            if (!cm.canHold(rewardId, rewardAmount)) {
                cm.sendOk("Please make sure you have enough inventory space before claiming your reward!");
                cm.dispose();
                return;
            }

            CustomQuestManager.addCustomQuestReward(cm.getPlayer().getId(), databaseId);
            cm.gainItem(rewardId, rewardAmount);
            str = "Enjoy your reward and congratulations on completing #b" + rewardReq + "#k quest(s)!\r\n\r\n";
            str += "#b#v" + rewardId + "# #z" + rewardId + "# x" + rewardAmount
            cm.sendOk(str);
        } else {
            cm.dispose();
        }
    }
}
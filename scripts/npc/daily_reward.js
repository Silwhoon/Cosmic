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
/* Daily Reward NPC script
	Handles providing players with daily rewards as long as they have the designated 'VIP item' in their inventory
 */

var status = -1;
var rewards = [
    [2000000, 50], // 50 red potions
    [2000001, 40], // 40 orange potions
    [1302000, 1], // 1 Sword
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

        // Verify the custom VIP system is enabled first
        const YamlConfig = Java.type('config.YamlConfig');
        if (!YamlConfig.config.server.USE_CUSTOM_VIP_SYSTEM) {
            cm.sendOk("This feature is not enabled.");
            cm.dispose();
            return;
        }

        // Verify the player has the 'VIP item' in their inventory
        if (!cm.hasItem(YamlConfig.config.server.CUSTOM_VIP_SYSTEM_ITEM_ID)) {
            cm.sendOk("You must have a #b#z" + YamlConfig.config.server.CUSTOM_VIP_SYSTEM_ITEM_ID + "##k in your inventory to claim daily rewards.");
            cm.dispose();
            return;
        }


        const DailyRewardManager = Java.type('server.DailyRewardManager');
        const eligibleForReward = DailyRewardManager.canClaimDailyReward(cm.getPlayer().getId());
        const nextRewardString = DailyRewardManager.getTimeUntilNextRewardString(cm.getPlayer().getId());

        if (status === 0) {
            var str = "Hello #b#h ##k, I can award you daily rewards!\r\n";
            str += "Time until next claim: #r" + nextRewardString + "\r\n\r\n#b";
            str += "#L0#Claim my daily reward#l";
            cm.sendSimple(str);
        } else if (status === 1) {
            // First check to make sure the player has at least 1 slot in each inventory slot
            if (!doesPlayerHaveEnoughSlots()) {
                cm.sendOk("Please make sure you have at least 1 open slot in each of your inventory tabs.");
                cm.dispose();
                return;
            }

            // If the player is not eligible then notify them
            if (!eligibleForReward) {
                cm.sendOk("You cannot receive your reward yet. Please wait #r" + nextRewardString + "#k before claiming again.");
                cm.dispose();
                return;
            }

            // Make sure the database receives the update before providing the reward
            if (DailyRewardManager.claimDailyReward(cm.getPlayer().getId())) {
                var randomIndex = Math.floor(Math.random() * rewards.length);
                var reward = rewards[randomIndex];
                cm.gainItem(reward[0], reward[1]);

                str = "Here's your reward, I hope you enjoy it and come back again tomorrow!\r\n\r\n";
                str += "#v" + reward[0] + "# #b#z" + reward[0] + "# (x" + reward[1] + ")"
                cm.sendOk(str);
            } else {
                cm.sendOk("Something went wrong, please try again later.");
            }
            cm.dispose();
        }
    }
}

function doesPlayerHaveEnoughSlots() {
    return cm.getInventory(1).getNumFreeSlot() > 0
        && cm.getInventory(2).getNumFreeSlot() > 0
        && cm.getInventory(3).getNumFreeSlot() > 0
        && cm.getInventory(4).getNumFreeSlot() > 0
        && cm.getInventory(5).getNumFreeSlot() > 0;
}
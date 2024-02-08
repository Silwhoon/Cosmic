/*
    This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
               Matthias Butz <matze@odinms.de>
               Jan Christian Meyer <vimes@odinms.de>

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

package server.partyquest.pyramid;

import client.Character;
import net.server.Server;
import net.server.channel.Channel;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import server.TimerManager;
import server.life.LifeFactory;
import server.life.Monster;
import server.maps.MapleMap;
import tools.PacketCreator;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author kevintjuh93
 * @author Silwhoon
 */
public class Pyramid {

    private static final int ENTRANCE_MAP_ID = 926010000;
    private static final int END_MAP_ID = 926010001;
    private static final int EXIT_MAP_ID = 926020001;
    // TODO: There is also another bonus (926010010) Although I dont know why.. its exactly the same
    private static final int BONUS_MAP_ID = 926010010;

    private final PyramidDifficulty difficulty;
    private final boolean solo;
    private final List<Character> characters = new ArrayList<>();
    private final Map<Integer, MapleMap> maps = new HashMap<>();
    private int gauge;
    private int counter = 0;
    private int currentStage;
    private PyramidRank currentRank = PyramidRank.C;
    private int totalKills = 0; // TODO: This needs to be seperated between all party members
    private int totalMisses = 0; // TODO: This needs to be seperated between all party members
    private int totalCools = 0; // TODO: This needs to be seperated between all party members
    private int coolAdd;
    private int decrease;
    private int hitAdd;
    private int missSub;
    private int total;
    private ScheduledFuture<?> stageTimer = null;
    private ScheduledFuture<?> coreTimer = null;


    protected Pyramid(Character soloCharacter, PyramidDifficulty difficulty) {
        this.difficulty = difficulty;
        this.solo = true;

        addCharacter(soloCharacter);
    }

    public Pyramid(Party party, PyramidDifficulty difficulty) {
        this.difficulty = difficulty;
        this.solo = false;

        for (PartyCharacter partyCharacter : party.getPartyMembersOnline()) {
            Character character = partyCharacter.getPlayer();
            addCharacter(character);
        }
    }

    private void addCharacter(Character character) {
        synchronized (this.characters) {
            this.characters.add(character);
        }
    }

    private void removeCharacter(Character character) {
        PyramidProcessor.closePyramid(character.getId());

        synchronized (this.characters) {
            this.characters.remove(character);
        }
    }

    public void start() {
        synchronized (this.characters) {
            for (Character character : this.characters) {
                PyramidProcessor.registerPyramid(character.getId(), this);
            }
        }

        broadcastInfo("party", solo ? 0 : 1);
        broadcastInfo("hit", 0);
        broadcastInfo("miss", 0);
        broadcastInfo("cool", 0);
        broadcastInfo("skill", 0);
        broadcastInfo("laststage", 1);

        currentStage = 1;

        // Begin stage 1
        warpAllToNextStage();
    }

    public void leave(Character character) {
        removeCharacter(character);

        synchronized (this.characters) {
            if (this.characters.size() < 2 && !solo) {
                fail();
            }
        }
    }

    public void fail() {
        if (stageTimer != null) stageTimer.cancel(true);
        if (coreTimer != null) coreTimer.cancel(true);
        this.currentRank = PyramidRank.D;
        synchronized (this.characters) {
            for (Character character : this.characters) {
                character.changeMap(END_MAP_ID);
            }
        }
    }

    private void startStageTimer() {
        if (stageTimer != null) {
            stageTimer.cancel(false);
            stageTimer = null;
        }

        stageTimer = TimerManager.getInstance().schedule(() -> {
            // Stop the core timer, warpAllToNextStage will restart it again
            if (coreTimer != null) {
                coreTimer.cancel(false);
                coreTimer = null;
            }

            // Increment stage and then warp all players to new stage
            currentStage++;
            warpAllToNextStage();
        }, SECONDS.toMillis(getTotalTime()));
    }

    private void startCoreTimer() {
        // Core timer loop - Runs every second
        coreTimer = TimerManager.getInstance().register(() -> {
            gauge -= decrease;
            if (gauge <= 0) {
                fail();
            }
        }, SECONDS.toMillis(1));
    }

    private void startBonusTimer(Character character) {
        TimerManager.getInstance().schedule(() -> {
            if (isBonusMap(character.getMap().getId())) {
                character.changeMap(ENTRANCE_MAP_ID);
            }

            removeCharacter(character);
        }, SECONDS.toMillis(60));
    }

    public void startBonus(Character character) {
        character.changeMap(BONUS_MAP_ID);

        int monsterId = this.difficulty.equals(PyramidDifficulty.HELL) ? 9700029 : 9700019;
        int numberOfMonsters = switch (this.difficulty) {
            case EASY -> 30;
            case NORMAL -> 40;
            case HARD, HELL -> 50;
        };

        Point topLeft = new Point(-361, -115);
        Point topRight = new Point(352, -115);
        Point bottomMiddle = new Point(4, 125);

        for (int i = 0; i < (numberOfMonsters / 3); i++) {
            character.getMap().spawnMonsterOnGroundBelow(LifeFactory.getMonster(monsterId), topLeft);
        }
        for (int i = 0; i < (numberOfMonsters / 3); i++) {
            character.getMap().spawnMonsterOnGroundBelow(LifeFactory.getMonster(monsterId), topRight);
        }
        for (int i = 0; i < (numberOfMonsters / 3); i++) {
            character.getMap().spawnMonsterOnGroundBelow(LifeFactory.getMonster(monsterId), bottomMiddle);
        }
        if (numberOfMonsters % 3 != 0) {
            for (int i = 0; i < (numberOfMonsters % 3); i++) {
                character.getMap().spawnMonsterOnGroundBelow(LifeFactory.getMonster(monsterId), bottomMiddle);
            }
        }

        startBonusTimer(character);
    }

    public void skipBonus(Character character) {
        // TODO: Check to make sure the player can hold the ETC item
        // TODO: Give relevant gem
        leave(character);
    }

    private void warpAllToNextStage() {
        coolAdd = getMap(getCurrentMap()).getPyramidInfo().getCoolAdd();
        decrease = getMap(getCurrentMap()).getPyramidInfo().getDecrease();
        hitAdd = getMap(getCurrentMap()).getPyramidInfo().getHitAdd();
        missSub = getMap(getCurrentMap()).getPyramidInfo().getMissSub();
        total = getMap(getCurrentMap()).getPyramidInfo().getTotal();
        gauge = total;
        counter = 0;

        synchronized (this.characters) {
            for (Character character : this.characters) {
                character.changeMap(getCurrentMap());
            }
        }

        if (currentStage <= 5) {
            startCoreTimer();
            startStageTimer();
        }

        // TODO: Re-calc rank here?
    }

    private int getCurrentMap() {
        // There are only 5 stages
        if (currentStage == 6) return END_MAP_ID;

        int mapId = 926010000;
        if (!solo) {
            mapId += 10000;
        }

        mapId += (this.difficulty.getMode() * 1000);
        mapId += (this.currentStage * 100);

        return mapId;
    }

    public void hitMonster(Character killedBy, Monster monster, int damage) {
        totalKills++;
        if (gauge < total) {
            counter++;
        }
        gauge += hitAdd;

        broadcastInfo("hit", totalKills);
        if (gauge >= total) {
            gauge = total;
        }

        if (damage >= monster.getStats().getCoolDamage()) {
            int rand = (new Random().nextInt(100) + 1);
            if (rand <= monster.getStats().getCoolDamageProb()) {
                coolProc();
            }
        }
    }

    private void coolProc() {
        totalCools++;
        int plus = coolAdd;
        if ((gauge + coolAdd) > total) {
            plus -= ((gauge + coolAdd) - total);
        }
        gauge += plus;
        counter += plus;
        if (gauge >= total) {
            gauge = total;
        }
        broadcastInfo("cool", totalCools);
    }

    // TODO: Implement misses
    public void missMonster(Character missedBy, Monster monster) {
        totalMisses++;
        gauge -= missSub;

        broadcastInfo("miss", totalMisses);
    }

    public boolean checkCharactersArePresent() {
        synchronized (this.characters) {
            for (Character character : this.characters) {
                if (character.getMap().getId() != ENTRANCE_MAP_ID) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean checkCharacterLevels() {
        synchronized (this.characters) {
            for (Character character : this.characters) {
                if (character.getLevel() < getMinLevel() || character.getLevel() > getMaxLevel()) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean checkIfFailed() {
        return this.currentRank.equals(PyramidRank.D);
    }

    private boolean isBonusMap(int id) {
        return id == 926010010 || id == 926010070;
    }

    public int getMinLevel() {
        return switch (this.difficulty) {
            case EASY -> 40;
            case NORMAL -> 46;
            case HARD -> 51;
            case HELL -> 61;
        };
    }

    public int getMaxLevel() {
        return switch (this.difficulty) {
            case EASY, NORMAL, HARD -> 60;
            case HELL -> 200;
        };
    }

    public int getTotalTime() {
        return switch (this.currentStage) {
//            case 0, 1 -> 120;
//            default -> 180;
            default -> 10;
        };
    }

    public MapleMap getMap(int id) {
        if (this.maps.containsKey(id)) {
            return this.maps.get(id);
        }

        synchronized (this.characters) {
            Character character = this.characters.get(0); // Doesn't matter which one we get

            // TODO: This is hacky... but for now it's the easiest way to get a MapManager
            Channel cs = Server.getInstance().getWorld(character.getWorld()).getChannel(character.getClient().getChannel());

            // Get a fresh map
            MapleMap map = cs.getMapFactory().getDisposableMap(id);

            // We always want a completely fresh bonus map, so don't save it
            if (!isBonusMap(id)) {
                this.maps.put(id, map);
            }

            return map;
        }
    }

    public void broadcastInfo(String info, int amount) {
        synchronized (this.characters) {
            for (Character character : this.characters) {
                character.sendPacket(PacketCreator.getEnergy("massacre_" + info, amount));
                character.sendPacket(PacketCreator.pyramidGauge(counter));
            }
        }
    }

    public void broadcastScore(Character character) {
        // TODO: Find GMS-like ranking formula
        int totalScore = (totalKills + totalCools - totalMisses);
        if (!this.checkIfFailed()) {
            if (totalScore >= 3000) this.currentRank = PyramidRank.S;
            else if (totalScore >= 2000) this.currentRank = PyramidRank.A;
            else if (totalScore >= 1000) this.currentRank = PyramidRank.B;
            else this.currentRank = PyramidRank.C;
        }

        // S: 3649 hits, 208 cool, 27 miss (86378 exp - 1 person - HELL)
        // S: 3399 hits, 208 cool, 101 miss (69378 exp - 1 person - HARD)
        // S: 3347 hits, 197 cool, 113 miss (89596 exp - 3 people - NORMAL)
        // S: 3275 hits, 186 cool, 7 miss (79910 exp - 1 person - EASY, NORMAL or HARD?)
        // A: 2491 hits, 0 cool, 15 miss (59982 - 1 person - EASY)
        // A: 2430 hits, 69 cool, 10 miss (60550 - 1 person - EASY)
        // A: 2124 hits, 146 cool, 52 miss (60709 - 1 person - EASY)

        // TODO: Find GMS-like exp formula
        int exp = (totalKills * 20) + (totalCools * 100);
        if (this.currentRank.equals(PyramidRank.S)) exp += (5500 * this.difficulty.getMode());
        if (this.currentRank.equals(PyramidRank.A)) exp += (5000 * this.difficulty.getMode());
        if (this.currentRank.equals(PyramidRank.B)) exp += (4250 * this.difficulty.getMode());
        if (this.currentRank.equals(PyramidRank.C)) exp += (2000 * this.difficulty.getMode());
        if (this.currentRank.equals(PyramidRank.D)) exp /= 5;

        character.sendPacket(PacketCreator.pyramidScore(this.currentRank.code, exp));
        character.gainExp(exp, true, true);
    }
}



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
import server.life.Monster;
import server.maps.MapleMap;
import tools.PacketCreator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Silwhoon
 */
public class Pyramid {

    private static final int ENTRANCE_MAP_ID = 926010000;

    private final PyramidDifficulty difficulty;
    private final List<Character> characters = new ArrayList<>();
    private float gaugePercent;
    private float gaugeDecreasePerSecond;
    private final float gaugeDecreasePerMiss = 3.0f; // TODO: This is purely guesswork, what is the real GMS value and does it differ based on difficulty/solo/party?
    private final float gaugeIncreasePerKill = 0.5f; // TODO: This is purely guesswork, what is the real GMS value and does it differ based on difficulty/solo/party?
    private int currentStage = 1;
    private int totalKills = 0;


    protected Pyramid(Character soloCharacter, PyramidDifficulty difficulty) {
        this.difficulty = difficulty;

        addCharacter(soloCharacter);

        switch (difficulty) {
            case EASY:
                this.gaugeDecreasePerSecond = 1; // TODO: Confirm this is correct
                break;
            case NORMAL:
                this.gaugeDecreasePerSecond = 1; // TODO: Confirm this is correct
                break;
            case HARD:
                this.gaugeDecreasePerSecond = 2; // TODO: Confirm this is correct
                break;
            case HELL:
                this.gaugeDecreasePerSecond = 2;
                break;
        }
    }

    public Pyramid(Party party, PyramidDifficulty difficulty) {
        this.difficulty = difficulty;

        for (PartyCharacter partyCharacter : party.getPartyMembersOnline()) {
            Character character = partyCharacter.getPlayer();
            addCharacter(character);
        }

        switch (difficulty) {
            case EASY:
                this.gaugeDecreasePerSecond = 1; // TODO: Confirm this is correct
                break;
            case NORMAL:
                this.gaugeDecreasePerSecond = 1;
                break;
            case HARD:
                this.gaugeDecreasePerSecond = 2; // TODO: Confirm this is correct
                break;
            case HELL:
                this.gaugeDecreasePerSecond = 3;
                break;
        }
    }

    private void addCharacter(Character character) {
        synchronized (this.characters) {
            this.characters.add(character);
        }
    }

    private void removeCharacter(Character character) {
        synchronized (this.characters) {
            this.characters.remove(character);
        }
    }

    public void start() {
        synchronized (this.characters) {
            for (Character character : this.characters) {
                PyramidProcessor.registerPyramid(character.getId(), this);
                character.changeMap(926010100, 0);
            }
        }
    }

    public void end() {
        synchronized (this.characters) {
            for (Character character : this.characters) {
                PyramidProcessor.closePyramid(character.getId());
            }
        }
    }

    public void kill(Monster monster) {
        totalKills++;
        synchronized (this.characters) {
            for (Character character : this.characters) {
                character.sendPacket(PacketCreator.getEnergy("massacre_hit", totalKills));
                // TODO: the totalKills for pyramidGauge needs to be capped based on how much time has elapsed
                character.sendPacket(PacketCreator.pyramidGauge(totalKills));
            }
        }
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
            case 1 -> 180;
            default -> 120;
        };
    }

    public MapleMap getMap(int id) {
        synchronized (this.characters) {
            Character character = this.characters.get(0);

            // TODO: This is hacky... but for now it's the easiest way to create our own MapManager
            Channel cs = Server.getInstance().getWorld(character.getWorld()).getChannel(character.getClient().getChannel());

            return cs.getMapFactory().getDisposableMap(id);
        }
    }
}



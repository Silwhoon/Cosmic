package server.quest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CustomQuestManager {

    private static final Logger log = LoggerFactory.getLogger(CustomQuestManager.class);

    public static List<Integer> getCustomQuestRewards(int characterId) {
        List<Integer> ret = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM quest_reward_custom WHERE characterId = ?;")) {
                ps.setInt(1, characterId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ret.add(rs.getInt("rewardId"));
                }
            }
        } catch (Exception e) {
            log.error("Error getting custom quest rewards for character {}", characterId, e);
        }

        return ret;
    }

    public static void addCustomQuestReward(int characterId, int rewardId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO quest_reward_custom (characterId, rewardId) VALUES (?, ?);")) {
                ps.setInt(1, characterId);
                ps.setInt(2, rewardId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            log.error("Error adding custom quest reward for character {}", characterId, e);
        }
    }
}

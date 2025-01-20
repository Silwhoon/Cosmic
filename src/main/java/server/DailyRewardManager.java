package server;

import net.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class DailyRewardManager {

    private static final Logger log = LoggerFactory.getLogger(DailyRewardManager.class);

    private static long getLastClaimedReward(int characterId) {
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM daily_rewards WHERE characterId = ?;")) {
                ps.setInt(1, characterId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return rs.getLong("lastClaimedReward");
                }

                return 0;
            }
        } catch (Exception e) {
            log.error("Error getting daily rewards for character {}", characterId, e);
        }

        return -1;
    }

    public static boolean canClaimDailyReward(int characterId) {
        long lastClaimedReward = getLastClaimedReward(characterId);

        if (lastClaimedReward == 0) {
            return true;
        }

        if (lastClaimedReward == -1) {
            return false;
        }

        // Get the current date based on the server's current time
        long currentTimeMillis = System.currentTimeMillis();
        LocalDate currentDate = Instant.ofEpochMilli(currentTimeMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // Get the last claimed reward date
        LocalDate lastClaimedDate = Instant.ofEpochMilli(lastClaimedReward)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // Check if the current date is after the last claimed date
        return currentDate.isAfter(lastClaimedDate);
    }

    public static boolean claimDailyReward(int characterId) {
        String query = "INSERT INTO daily_rewards (characterId, lastClaimedReward) VALUES (?, ?) ON DUPLICATE KEY UPDATE lastClaimedReward = VALUES(lastClaimedReward);";
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.setInt(1, characterId);
                ps.setLong(2, Server.getInstance().getCurrentTime());
                ps.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            log.error("Error adding or updating daily reward for character {}", characterId, e);
        }
        return false;
    }

    public static String getTimeUntilNextRewardString(int characterId) {
        LocalDateTime lastClaimedRewardTime = Instant.ofEpochMilli(getLastClaimedReward(characterId))
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // Calculate the next midnight after the last claimed reward
        LocalDateTime nextMidnight = lastClaimedRewardTime.toLocalDate().plusDays(1).atStartOfDay();

        // Get the current time
        LocalDateTime currentTime = Instant.ofEpochMilli(System.currentTimeMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        // If current time is past the next midnight, the reward is already claimable
        if (!currentTime.isBefore(nextMidnight)) {
            return "#gReady!#k";
        }

        // Calculate hours and minutes until the next midnight
        long hours = ChronoUnit.HOURS.between(currentTime, nextMidnight);
        long minutes = ChronoUnit.MINUTES.between(currentTime, nextMidnight) % 60;

        return String.format("%d hours #kand#r %d minutes", hours, minutes);
    }

}

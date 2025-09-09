package io.github.mcengine.common.lang.database.sqlite.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * Reads the player's language code from the {@code lang} table (SQLite).
 */
public final class getLangUtil {
    private getLangUtil() {}

    /**
     * @param conn   active SQLite {@link Connection}
     * @param plugin plugin for logging
     * @param player Bukkit player
     * @return language code or {@code "en_US"} when missing/error
     */
    public static String invoke(Connection conn, Plugin plugin, Player player) {
        if (conn == null) return "en_US";
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement("SELECT lang FROM lang WHERE player_uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite getLang failed: " + e.getMessage());
        }
        return "en_US";
    }
}

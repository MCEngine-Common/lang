package io.github.mcengine.common.lang.database.postgresql.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * Reads the player's language code from the {@code lang} table (PostgreSQL).
 */
public final class getLangUtil {
    private getLangUtil() {}

    public static String invoke(Connection conn, Plugin plugin, Player player) {
        if (conn == null) return "en_US";
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement("SELECT lang FROM lang WHERE player_uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("PostgreSQL getLang failed: " + e.getMessage());
        }
        return "en_US";
    }
}

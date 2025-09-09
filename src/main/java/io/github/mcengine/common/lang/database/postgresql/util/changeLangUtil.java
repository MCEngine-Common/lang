package io.github.mcengine.common.lang.database.postgresql.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * Changes the player's language code if different (PostgreSQL).
 */
public final class changeLangUtil {
    private changeLangUtil() {}

    public static boolean invoke(Connection conn, Plugin plugin, Player player, String newLangType) {
        if (conn == null) return false;
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE lang SET lang=? WHERE player_uuid=? AND lang<>?")) {
            ps.setString(1, newLangType);
            ps.setString(2, uuid);
            ps.setString(3, newLangType);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO lang (player_uuid, lang) VALUES (?, ?) " +
                        "ON CONFLICT (player_uuid) DO UPDATE SET lang=EXCLUDED.lang")) {
                    ins.setString(1, uuid);
                    ins.setString(2, newLangType);
                    return ins.executeUpdate() > 0;
                }
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("PostgreSQL changeLang failed: " + e.getMessage());
            return false;
        }
    }
}

package io.github.mcengine.common.lang.database.mysql.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * Upserts the player's language code in the {@code lang} table (MySQL).
 */
public final class setLangUtil {
    private setLangUtil() {}

    public static void invoke(Connection conn, Plugin plugin, Player player, String langType) {
        if (conn == null) return;
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lang (player_uuid, lang) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE lang=VALUES(lang)")) {
            ps.setString(1, uuid);
            ps.setString(2, langType);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL setLang failed: " + e.getMessage());
        }
    }
}

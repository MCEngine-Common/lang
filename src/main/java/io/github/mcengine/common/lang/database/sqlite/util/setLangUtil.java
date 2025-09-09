package io.github.mcengine.common.lang.database.sqlite.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * Upserts the player's language code in the {@code lang} table (SQLite).
 */
public final class setLangUtil {
    private setLangUtil() {}

    /**
     * @param conn     active SQLite {@link Connection}
     * @param plugin   plugin for logging
     * @param player   Bukkit player
     * @param langType language code to persist
     */
    public static void invoke(Connection conn, Plugin plugin, Player player, String langType) {
        if (conn == null) return;
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lang (player_uuid, lang) VALUES (?, ?) " +
                "ON CONFLICT(player_uuid) DO UPDATE SET lang=excluded.lang")) {
            ps.setString(1, uuid);
            ps.setString(2, langType);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("SQLite setLang failed: " + e.getMessage());
        }
    }
}

package io.github.mcengine.common.lang.database.postgresql.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * Upserts the player's language code in the {@code lang} table (PostgreSQL).
 */
public final class setLangUtil {
    private setLangUtil() {}

    public static void invoke(Connection conn, Plugin plugin, Player player, String langType) {
        if (conn == null) return;
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO lang (player_uuid, lang) VALUES (?, ?) " +
                "ON CONFLICT (player_uuid) DO UPDATE SET lang=EXCLUDED.lang")) {
            ps.setString(1, uuid);
            ps.setString(2, langType);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("PostgreSQL setLang failed: " + e.getMessage());
        }
    }
}

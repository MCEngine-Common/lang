package io.github.mcengine.common.lang.database.mysql;

import io.github.mcengine.common.lang.database.IMCEngineLangDB;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * MySQL implementation of the Lang database.
 *
 * <p>Schema (ensured on init):</p>
 * <pre>
 * CREATE TABLE IF NOT EXISTS lang (
 *   player_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
 *   lang        VARCHAR(32) NOT NULL DEFAULT 'en_US'
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
 * </pre>
 */
public final class MCEngineLangMySQL implements IMCEngineLangDB {

    /** Owning plugin for configuration and logging. */
    private final Plugin plugin;

    /** JDBC URL built from plugin config. */
    private final String jdbcUrl;

    /** JDBC credentials. */
    private final String user, pass;

    /** Shared MySQL connection for this instance. */
    private final Connection conn;

    /**
     * Builds the MySQL connection from config:
     * <ul>
     *   <li>{@code database.mysql.host} (default: {@code localhost})</li>
     *   <li>{@code database.mysql.port} (default: {@code 3306})</li>
     *   <li>{@code database.mysql.database} (default: {@code mcengine})</li>
     *   <li>{@code database.mysql.user} (default: {@code root})</li>
     *   <li>{@code database.mysql.password} (default: empty)</li>
     * </ul>
     *
     * @param plugin Bukkit plugin instance
     */
    public MCEngineLangMySQL(Plugin plugin) {
        this.plugin = plugin;

        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String db   = plugin.getConfig().getString("database.mysql.database", "mcengine");
        this.user   = plugin.getConfig().getString("database.mysql.user", "root");
        this.pass   = plugin.getConfig().getString("database.mysql.password", "");

        this.jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db +
                "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8";

        Connection tmp = null;
        try {
            tmp = DriverManager.getConnection(jdbcUrl, user, pass);
            ensureSchema(tmp);
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL (Lang) connect/ensure failed: " + e.getMessage());
            e.printStackTrace();
        }
        this.conn = tmp;
    }

    /** Ensures the {@code lang} table exists. */
    private void ensureSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS lang (" +
                "  player_uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                "  lang        VARCHAR(32) NOT NULL DEFAULT 'en_US'" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_lang_lang ON lang(lang)");
        } catch (SQLException e) {
            // Older MySQL doesn't support "IF NOT EXISTS" for index creation with this syntax; fallback:
            try (Statement st2 = c.createStatement()) {
                st2.executeUpdate("CREATE INDEX idx_lang_lang ON lang(lang)");
            } catch (SQLException ignored) { /* index may already exist */ }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Connection getConnection(Connection conn) {
        return (conn != null) ? conn : this.conn;
    }

    /** {@inheritDoc} */
    @Override
    public String getLang(Player player) {
        if (this.conn == null) return "en_US";
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement("SELECT lang FROM lang WHERE player_uuid=?")) {
            ps.setString(1, uuid);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL getLang failed: " + e.getMessage());
        }
        return "en_US";
    }

    /** {@inheritDoc} */
    @Override
    public void setLang(Player player, String langType) {
        if (this.conn == null) return;
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

    /** {@inheritDoc} */
    @Override
    public boolean changeLang(Player player, String newLangType) {
        if (this.conn == null) return false;
        final String uuid = player.getUniqueId().toString();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE lang SET lang=? WHERE player_uuid=? AND lang<>?")) {
            ps.setString(1, newLangType);
            ps.setString(2, uuid);
            ps.setString(3, newLangType);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                // If row doesn't exist, insert with newLangType
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO lang (player_uuid, lang) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE lang=VALUES(lang)")) {
                    ins.setString(1, uuid);
                    ins.setString(2, newLangType);
                    return ins.executeUpdate() > 0;
                }
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().warning("MySQL changeLang failed: " + e.getMessage());
            return false;
        }
    }
}

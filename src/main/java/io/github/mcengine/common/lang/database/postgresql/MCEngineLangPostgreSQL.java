package io.github.mcengine.common.lang.database.postgresql;

import io.github.mcengine.common.lang.database.IMCEngineLangDB;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.*;

/**
 * PostgreSQL implementation of the Lang database.
 *
 * <p>Schema (ensured on init):</p>
 * <pre>
 * CREATE TABLE IF NOT EXISTS lang (
 *   player_uuid VARCHAR(36) PRIMARY KEY,
 *   lang        VARCHAR(32) NOT NULL DEFAULT 'en_US'
 * );
 * </pre>
 */
public final class MCEngineLangPostgreSQL implements IMCEngineLangDB {

    /** Owning plugin for configuration and logging. */
    private final Plugin plugin;

    /** JDBC URL built from plugin config. */
    private final String jdbcUrl;

    /** JDBC credentials. */
    private final String user, pass;

    /** Shared PostgreSQL connection for this instance. */
    private final Connection conn;

    /**
     * Builds the PostgreSQL connection from config:
     * <ul>
     *   <li>{@code database.postgresql.host} (default: {@code localhost})</li>
     *   <li>{@code database.postgresql.port} (default: {@code 5432})</li>
     *   <li>{@code database.postgresql.database} (default: {@code mcengine})</li>
     *   <li>{@code database.postgresql.user} (default: {@code postgres})</li>
     *   <li>{@code database.postgresql.password} (default: empty)</li>
     * </ul>
     *
     * @param plugin Bukkit plugin instance
     */
    public MCEngineLangPostgreSQL(Plugin plugin) {
        this.plugin = plugin;

        String host = plugin.getConfig().getString("database.postgresql.host", "localhost");
        int port = plugin.getConfig().getInt("database.postgresql.port", 5432);
        String db   = plugin.getConfig().getString("database.postgresql.database", "mcengine");
        this.user   = plugin.getConfig().getString("database.postgresql.user", "postgres");
        this.pass   = plugin.getConfig().getString("database.postgresql.password", "");

        this.jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + db;

        Connection tmp = null;
        try {
            tmp = DriverManager.getConnection(jdbcUrl, user, pass);
            ensureSchema(tmp);
        } catch (SQLException e) {
            plugin.getLogger().warning("PostgreSQL (Lang) connect/ensure failed: " + e.getMessage());
            e.printStackTrace();
        }
        this.conn = tmp;
    }

    /** Ensures the {@code lang} table exists. */
    private void ensureSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS lang (" +
                "  player_uuid VARCHAR(36) PRIMARY KEY," +
                "  lang        VARCHAR(32) NOT NULL DEFAULT 'en_US'" +
                ")"
            );
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_lang_lang ON lang(lang)");
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
            plugin.getLogger().warning("PostgreSQL getLang failed: " + e.getMessage());
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
                "ON CONFLICT (player_uuid) DO UPDATE SET lang=EXCLUDED.lang")) {
            ps.setString(1, uuid);
            ps.setString(2, langType);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("PostgreSQL setLang failed: " + e.getMessage());
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

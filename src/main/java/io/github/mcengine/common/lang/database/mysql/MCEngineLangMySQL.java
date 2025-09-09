package io.github.mcengine.common.lang.database.mysql;

import io.github.mcengine.common.lang.database.IMCEngineLangDB;
import io.github.mcengine.common.lang.database.mysql.util.changeLangUtil;
import io.github.mcengine.common.lang.database.mysql.util.getLangUtil;
import io.github.mcengine.common.lang.database.mysql.util.setLangUtil;
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
 *
 * <p>
 * Contract methods delegate their SQL to focused util classes under
 * {@code io.github.mcengine.common.lang.database.mysql.util}.
 * </p>
 */
public final class MCEngineLangMySQL implements IMCEngineLangDB {

    /** Owning plugin for configuration and logging. */
    private final Plugin plugin;

    /** JDBC URL and credentials from config. */
    private final String jdbcUrl;
    private final String user, pass;

    /** Shared MySQL connection for this instance. */
    private final Connection conn;

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
            // Some MySQL variants may not support IF NOT EXISTS for indexes with this syntax:
            try {
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_lang_lang ON lang(lang)");
            } catch (SQLException ignored) {
                try (Statement st2 = c.createStatement()) {
                    st2.executeUpdate("CREATE INDEX idx_lang_lang ON lang(lang)");
                } catch (SQLException ignored2) { /* already exists */ }
            }
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
        return getLangUtil.invoke(this.conn, this.plugin, player);
    }

    /** {@inheritDoc} */
    @Override
    public void setLang(Player player, String langType) {
        setLangUtil.invoke(this.conn, this.plugin, player, langType);
    }

    /** {@inheritDoc} */
    @Override
    public boolean changeLang(Player player, String newLangType) {
        return changeLangUtil.invoke(this.conn, this.plugin, player, newLangType);
    }
}

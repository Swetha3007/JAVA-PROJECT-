import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** JDBC connection management. Override defaults with env vars DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD. */
public class DBConnection {
    private static String env(String key, String def) {
        String v = System.getenv(key);
        return v != null ? v : def;
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] MySQL Connector/J not on classpath: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://" + env("DB_HOST", "localhost") + ":" + env("DB_PORT", "3306") + "/"
                + env("DB_NAME", "kec_eval") + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, env("DB_USER", "root"), env("DB_PASSWORD", "root"));
    }

    public static boolean testConnection() {
        try (Connection c = getConnection()) {
            return c != null && !c.isClosed();
        } catch (SQLException e) {
            System.err.println("[ERROR] " + e.getMessage());
            return false;
        }
    }
}

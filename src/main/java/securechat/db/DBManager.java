package securechat.db;

import java.sql.*;

public class DBManager {
    private static final String URL = "jdbc:mysql://localhost:3306/securechat";
    private static final String USER = "root";
    private static final String PASS = "vardhan";

    public static Connection getConnection() throws SQLException {
        String host = System.getenv("MYSQLHOST");
        String port = System.getenv("MYSQLPORT");
        String db   = System.getenv("MYSQLDATABASE");
        String user = System.getenv("MYSQLUSER");
        String pass = System.getenv("MYSQLPASSWORD");

        String url;
        if (host != null && db != null) {
            // Running on Railway
            url = "jdbc:mysql://" + host + ":" + port + "/" + db
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&connectTimeout=10000"
                    + "&socketTimeout=10000";
            System.out.println("Connecting to Railway DB: " + url);
        } else {
            // Running locally
            url  = "jdbc:mysql://localhost:3306/securechat";
            user = "root";
            pass = "yourpassword";
            System.out.println("Connecting to local DB");
        }

        return DriverManager.getConnection(url, user, pass);
    }

    // Now stores RSA + DSA + DH public keys
    public static boolean registerUser(String username, String passwordHash,
                                       String rsaPubKey, String dsaPubKey,
                                       String dhPubKey) {
        String sql = "INSERT INTO users (username, password_hash, public_key_rsa, " +
                "public_key_dsa, public_key_dh) VALUES (?,?,?,?,?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, rsaPubKey);
            ps.setString(4, dsaPubKey);
            ps.setString(5, dhPubKey);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public static String getPasswordHash(String username) {
        return getField(username, "password_hash");
    }

    public static String getRSAPublicKey(String username) {
        return getField(username, "public_key_rsa");
    }

    public static String getDSAPublicKey(String username) {
        return getField(username, "public_key_dsa");
    }

    public static String getDHPublicKey(String username) {
        return getField(username, "public_key_dh");
    }

    private static String getField(String username, String column) {
        String sql = "SELECT " + column + " FROM users WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(column);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
    public static void updateDHKey(String username, String dhPubKey) {
        String sql = "UPDATE users SET public_key_dh = ? WHERE username = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dhPubKey);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static void saveMessage(String sender, String receiver, String ciphertext,
                                   String hash, String prevHash,
                                   String signature, String iv) {
        String sql = "INSERT INTO messages (sender, receiver, ciphertext, " +
                "message_hash, prev_hash, signature, iv) VALUES (?,?,?,?,?,?,?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, ciphertext);
            ps.setString(4, hash);
            ps.setString(5, prevHash);
            ps.setString(6, signature);
            ps.setString(7, iv);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

}
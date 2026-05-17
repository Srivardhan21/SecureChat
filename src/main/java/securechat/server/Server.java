package securechat.server;

import java.net.*;
import java.util.concurrent.*;
import java.util.*;

public class Server {
    public static Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("Starting SecureChat Server...");

        // Railway provides PORT env variable, fallback to 9090 locally
        String portEnv = System.getenv("PORT");
        int PORT = (portEnv != null) ? Integer.parseInt(portEnv) : 9090;
        System.out.println("PORT env variable: " + portEnv);
        System.out.println("Using port: " + PORT);

        // DB connection check
        try {
            java.sql.Connection con = securechat.db.DBManager.getConnection();
            System.out.println("DB Connected: " + con.getMetaData().getURL());
            con.close();
        } catch (Exception e) {
            System.out.println("DB Connection failed: " + e.getMessage());
        }

        // MySQL env variables check
        System.out.println("MYSQLHOST: " + System.getenv("MYSQLHOST"));
        System.out.println("MYSQLPORT: " + System.getenv("MYSQLPORT"));
        System.out.println("MYSQLDATABASE: " + System.getenv("MYSQLDATABASE"));
        System.out.println("MYSQLUSER: " + System.getenv("MYSQLUSER"));

        try {
            ExecutorService pool = Executors.newCachedThreadPool();
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(PORT));
            System.out.println("SecureChat Server running on port " + PORT);
            System.out.println("Waiting for clients...");

            // Keep server alive forever
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: "
                            + clientSocket.getInetAddress().getHostAddress());
                    ClientHandler handler = new ClientHandler(clientSocket);
                    pool.execute(handler);
                } catch (Exception e) {
                    System.out.println("Connection error: " + e.getMessage());
                    // Continue — don't crash the server
                }
            }
        } catch (Exception e) {
            System.out.println("Server startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
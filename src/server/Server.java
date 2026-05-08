package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import config.ServerConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Serveur principal de ChriOnline.
 * Protection anti SYN flood : maximum 10 connexions simultanées par adresse IP.
 */
public class Server {

	private static final Logger logger = LogManager.getLogger(Server.class);
    private static ServerSocket serverSocket;
    private static final int MAX_CONNECTIONS_PER_IP = 10;
    private static final ConcurrentHashMap<String, Integer> ipCount = new ConcurrentHashMap<>();

    public static void main(String[] args) {
    	logger.info("🚀 Démarrage du serveur ChriOnline...");

        try {
            // Demander les mots de passe des keystores
            ServerConfig.initialize();

            serverSocket = new ServerSocket(ServerConfig.PORT);
            logger.info("✅ Serveur démarré sur le port {}", ServerConfig.PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String ip = clientSocket.getInetAddress().getHostAddress();

                // Vérifier le nombre de connexions pour cette IP
                int count = ipCount.merge(ip, 1, Integer::sum);
                if (count > MAX_CONNECTIONS_PER_IP) {
                	logger.warn("❌ Trop de connexions depuis {} → refusée", ip);
                    clientSocket.close();
                    ipCount.merge(ip, -1, Integer::sum);
                    continue;
                }

                System.out.println("✅ Nouveau client connecté : " + ip);
                ClientHandler handler = new ClientHandler(clientSocket, ip);
                handler.start();
            }

        } catch (IOException e) {
        	logger.error("❌ Erreur critique du serveur : ", e);
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("🛑 Serveur arrêté.");
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Appelé par ClientHandler quand une connexion se termine.
     */
    public static void decrementIP(String ip) {
        ipCount.merge(ip, -1, Integer::sum);
    }
}
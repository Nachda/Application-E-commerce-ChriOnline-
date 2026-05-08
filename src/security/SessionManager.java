package security;

import java.security.SecureRandom;
import java.util.Map; 
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Gère les tokens de session pour lutter contre le détournement de session.
 * Chaque utilisateur connecté reçoit un token unique, renouvelé à chaque login.
 */
public class SessionManager {

    private static final long SESSION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    private final ConcurrentHashMap<Integer, SessionInfo> sessions = new ConcurrentHashMap<>();

    private static class SessionInfo {
        String token;
        long lastActivity;
        String ip; // facultatif : lier à l'IP

        SessionInfo(String token, String ip) {
            this.token = token;
            this.lastActivity = System.currentTimeMillis();
            this.ip = ip;
        }
    }

    /**
     * Crée une nouvelle session pour un utilisateur et retourne le token.
     */
    public String createSession(int userId, String ip) {
        String token = generateToken();
        sessions.put(userId, new SessionInfo(token, ip));
        System.out.println("🆕 Session créée pour userId=" + userId + " ip=" + ip);
        return token;
    }

    /**
     * Vérifie que le token fourni correspond bien à l'utilisateur.
     * @return true si la session est valide, false sinon
     */
    public boolean isValidSession(int userId, String token) {
        SessionInfo info = sessions.get(userId);
        if (info == null) return false;
        if (!info.token.equals(token)) return false;
        long now = System.currentTimeMillis();
        if (now - info.lastActivity > SESSION_TIMEOUT_MS) {
            sessions.remove(userId);
            System.out.println("⏰ Session expirée pour userId=" + userId);
            return false;
        }
        info.lastActivity = now; // mise à jour de l'activité
        return true;
    }

    /**
     * Détruit la session (logout).
     */
    public void destroySession(int userId) {
        sessions.remove(userId);
        System.out.println("🗑️ Session détruite pour userId=" + userId);
    }

    private String generateToken() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }
    
    /**
     * Retourne l'ID de l'utilisateur associé au token, ou -1 si introuvable.
     */
    public int getUserIdForToken(String token) {
        for (Map.Entry<Integer, SessionInfo> entry : sessions.entrySet()) {
            if (entry.getValue().token.equals(token)) {
                return entry.getKey();
            }
        }
        return -1;
    }
}
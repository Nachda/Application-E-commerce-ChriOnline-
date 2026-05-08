package security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Protection contre les attaques par force brute sur l'authentification.
 * Bloque temporairement un email ou une IP après un nombre défini d'échecs.
 */
public class BruteForceProtection {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = TimeUnit.MINUTES.toMillis(5);

    private final ConcurrentHashMap<String, FailEntry> byEmail = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FailEntry> byIP = new ConcurrentHashMap<>();

    private static class FailEntry {
        int count;
        long firstAttempt;

        FailEntry(int count, long firstAttempt) {
            this.count = count;
            this.firstAttempt = firstAttempt;
        }
    }

    /**
     * Vérifie que la tentative est autorisée.
     */
    public boolean isAllowed(String email, String ip) {
        cleanExpired();
        boolean blockedByEmail = isBlocked(byEmail, email);
        boolean blockedByIP = isBlocked(byIP, ip);
        System.out.println("🔒 BruteForce check: email=" + email + " blockedByEmail=" + blockedByEmail + ", ip=" + ip + " blockedByIP=" + blockedByIP);
        return !blockedByEmail && !blockedByIP;
    }

    /**
     * Enregistre un échec.
     */
    public void registerFailure(String email, String ip) {
        long now = System.currentTimeMillis();
        byEmail.compute(email, (k, v) -> (v == null || (now - v.firstAttempt) > BLOCK_DURATION_MS)
                ? new FailEntry(1, now) : new FailEntry(v.count + 1, v.firstAttempt));
        byIP.compute(ip, (k, v) -> (v == null || (now - v.firstAttempt) > BLOCK_DURATION_MS)
                ? new FailEntry(1, now) : new FailEntry(v.count + 1, v.firstAttempt));
        System.out.println("⚠️ Échec enregistré: email=" + email + " count=" + byEmail.get(email).count + ", ip=" + ip + " count=" + byIP.get(ip).count);
    }

    /**
     * Réinitialise après un succès.
     */
    public void reset(String email, String ip) {
        byEmail.remove(email);
        byIP.remove(ip);
        System.out.println("✅ Reset brute force pour email=" + email + " ip=" + ip);
    }

    private boolean isBlocked(ConcurrentHashMap<String, FailEntry> map, String key) {
        FailEntry e = map.get(key);
        if (e == null) return false;
        long now = System.currentTimeMillis();
        if (e.count >= MAX_ATTEMPTS && (now - e.firstAttempt) < BLOCK_DURATION_MS) {
            return true;
        }
        if ((now - e.firstAttempt) >= BLOCK_DURATION_MS) {
            map.remove(key);
        }
        return false;
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        byEmail.entrySet().removeIf(e -> (now - e.getValue().firstAttempt) >= BLOCK_DURATION_MS);
        byIP.entrySet().removeIf(e -> (now - e.getValue().firstAttempt) >= BLOCK_DURATION_MS);
    }
}
package security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Protection anti‑rejeu : chaque requête doit contenir un nonce unique.
 */
public class ReplayProtection {

    private static final long WINDOW_MS = TimeUnit.MINUTES.toMillis(10);
    private final ConcurrentHashMap<String, Long> seenNonces = new ConcurrentHashMap<>();

    /**
     * Vérifie si le nonce est nouveau et l'enregistre.
     * @return true si OK, false si déjà vu
     */
    public boolean isNonceValid(String nonce) {
        cleanExpired();
        return seenNonces.putIfAbsent(nonce, System.currentTimeMillis() + WINDOW_MS) == null;
    }

    private void cleanExpired() {
        long now = System.currentTimeMillis();
        seenNonces.entrySet().removeIf(e -> e.getValue() < now);
    }
}
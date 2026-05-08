package security;

/**
 * Valide strictement les données reçues du réseau pour prévenir les injections de commandes.
 * Chaque champ est vérifié avec une expression régulière autorisant uniquement les caractères légitimes.
 */
public class InputValidator {

    // Autorise lettres, chiffres, espaces, accents, tirets, apostrophes (noms, prénoms, villes)
    private static final String NAME_PATTERN = "[\\p{L}\\p{M} \\-']+";
    // Email standard
    private static final String EMAIL_PATTERN = "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}";
    // Hash SHA-256 (64 caractères hexadécimaux)
    private static final String HASH_PATTERN = "[a-fA-F0-9]{64}";
    // Numéro de téléphone simplifié (chiffres, +, espaces, tirets)
    private static final String PHONE_PATTERN = "[0-9+\\- ]{6,20}";
    // Adresse : autorise lettres, chiffres, espaces, virgules, points, tirets
    private static final String ADDRESS_PATTERN = "[\\p{L}\\p{M}0-9 ,.\\-]+";
    // Méthode de paiement
    private static final String PAYMENT_METHOD_PATTERN = "(card|especes)";
    // Status de commande
    private static final String STATUS_PATTERN = "(pending|validated|shipped|delivered|cancelled)";

    public static boolean isValidName(String input) {
        return input != null && input.matches(NAME_PATTERN);
    }

    public static boolean isValidEmail(String input) {
        return input != null && input.matches(EMAIL_PATTERN);
    }

    public static boolean isValidHash(String input) {
        return input != null && input.matches(HASH_PATTERN);
    }

    public static boolean isValidPhone(String input) {
        return input != null && input.matches(PHONE_PATTERN);
    }

    public static boolean isValidAddress(String input) {
        return input != null && input.matches(ADDRESS_PATTERN);
    }

    public static boolean isValidPaymentMethod(String input) {
        return input != null && input.matches(PAYMENT_METHOD_PATTERN);
    }

    public static boolean isValidStatus(String input) {
        return input != null && input.matches(STATUS_PATTERN);
    }
}
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Simple password generator + AES-GCM encrypt/decrypt helper.
 * - generatePassword(length): returns a random password.
 * - encrypt(plaintext): returns base64-encoded key/iv/ciphertext (key is included here for demo).
 * - decrypt(base64Key, base64Iv, base64Ciphertext): returns decrypted plaintext.
 *
 * Note: In production you must protect the encryption key (do NOT print/store it in plaintext).
 */
public class PasswordGenerator {
	// small container for encryption result
	public static class EncryptionResult {
		public final String base64Key; // may be null for passphrase-based encryption
		public final String base64Iv;
		public final String base64Ciphertext;
		public EncryptionResult(String k, String iv, String c) { base64Key = k; base64Iv = iv; base64Ciphertext = c; }
	}

	// secure random generator
	private static final SecureRandom SR = new SecureRandom();
	private static final String CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*()-_=+";

	// generate a random password of given length
	public static String generatePassword(int length) {
		if (length <= 0) length = 12;
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int idx = SR.nextInt(CHARSET.length());
			sb.append(CHARSET.charAt(idx));
		}
		return sb.toString();
	}

	// --- AES-GCM encrypt/decrypt with a raw key (kept for reference) ---
	public static EncryptionResult encrypt(String plaintext) throws Exception {
		// generate AES key (128-bit)
		KeyGenerator kg = KeyGenerator.getInstance("AES");
		kg.init(128, SR);
		SecretKey key = kg.generateKey();

		// generate IV (12 bytes recommended for GCM)
		byte[] iv = new byte[12];
		SR.nextBytes(iv);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec spec = new GCMParameterSpec(128, iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, spec);

		byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

		String bKey = Base64.getEncoder().encodeToString(key.getEncoded());
		String bIv = Base64.getEncoder().encodeToString(iv);
		String bCt = Base64.getEncoder().encodeToString(ct);
		return new EncryptionResult(bKey, bIv, bCt);
	}

	public static String decrypt(String base64Key, String base64Iv, String base64Ciphertext) throws Exception {
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		byte[] iv = Base64.getDecoder().decode(base64Iv);
		byte[] ct = Base64.getDecoder().decode(base64Ciphertext);

		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec spec = new GCMParameterSpec(128, iv);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

		byte[] pt = cipher.doFinal(ct);
		return new String(pt, StandardCharsets.UTF_8);
	}

	// --- PBKDF2-based key derivation (derive AES key from passphrase + salt) ---
	private static SecretKeySpec deriveKeyFromPassphrase(String passphrase, byte[] salt) throws Exception {
		final int iterations = 65536;
		final int keyLength = 128; // bits
		PBEKeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, iterations, keyLength);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		SecretKey secret = skf.generateSecret(spec);
		byte[] encoded = secret.getEncoded();
		return new SecretKeySpec(encoded, "AES");
	}

	// Encrypt using a passphrase (do NOT store the passphrase - you must provide it again to decrypt)
	// saltStr is used to vary derived key (use driverId or random salt). Returned EncryptionResult has base64Iv/base64Ciphertext, base64Key=null.
	public static EncryptionResult encryptWithPassphrase(String plaintext, String passphrase, String saltStr) throws Exception {
		byte[] salt = (saltStr != null ? saltStr : "").getBytes(StandardCharsets.UTF_8);
		SecretKeySpec keySpec = deriveKeyFromPassphrase(passphrase, salt);

		byte[] iv = new byte[12];
		SR.nextBytes(iv);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec spec = new GCMParameterSpec(128, iv);
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

		byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
		String bIv = Base64.getEncoder().encodeToString(iv);
		String bCt = Base64.getEncoder().encodeToString(ct);
		return new EncryptionResult(null, bIv, bCt);
	}

	// Decrypt using same passphrase and salt string as used for encryption
	public static String decryptWithPassphrase(String passphrase, String saltStr, String base64Iv, String base64Ciphertext) throws Exception {
		byte[] salt = (saltStr != null ? saltStr : "").getBytes(StandardCharsets.UTF_8);
		SecretKeySpec keySpec = deriveKeyFromPassphrase(passphrase, salt);

		byte[] iv = Base64.getDecoder().decode(base64Iv);
		byte[] ct = Base64.getDecoder().decode(base64Ciphertext);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		GCMParameterSpec spec = new GCMParameterSpec(128, iv);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

		byte[] pt = cipher.doFinal(ct);
		return new String(pt, StandardCharsets.UTF_8);
	}

	// demo main (optional)
	public static void main(String[] args) {
		try {
			String pwd = generatePassword(12);
			System.out.println("Generated password: " + pwd);

			// Example using passphrase-based encryption (do not store passphrase in DB)
			String passphrase = System.getenv("PASSWORD_MASTER_KEY");
			if (passphrase == null || passphrase.isEmpty()) passphrase = "fallback-secret";
			EncryptionResult res = encryptWithPassphrase(pwd, passphrase, "SALT123");
			System.out.println("Base64 IV      : " + res.base64Iv);
			System.out.println("Base64 Cipher  : " + res.base64Ciphertext);

			String decrypted = decryptWithPassphrase(passphrase, "SALT123", res.base64Iv, res.base64Ciphertext);
			System.out.println("Decrypted pwd  : " + decrypted);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

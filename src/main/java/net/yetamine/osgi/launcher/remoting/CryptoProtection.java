package net.yetamine.osgi.launcher.remoting;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Provides cryptographic protection for command transport.
 */
public final class CryptoProtection {

    /* Implementation notes:
     *
     * The secrets should be generated always, hence serve as one-time
     * passwords. For this reason it is not necessary to maintain salt,
     * initialization vectors etc. It is enough to generate a bit string
     * long enough for the chosen cipher.
     */

    private static final String SECRET_TRANSFORM = "SHA-256";
    private static final String CIPHER_ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORM = CIPHER_ALGORITHM + "/CBC/PKCS5PADDING";
    private static final IvParameterSpec CIPHER_IV = new IvParameterSpec(new byte[16]);

    private final SecretKey key;

    /**
     * Creates a new instance.
     *
     * @param secret
     *            the secret to generate the protection key. It must be a
     *            non-empty string.
     *
     * @throws GeneralSecurityException
     *             if the used cipher suite is not supported by the JVM
     */
    public CryptoProtection(String secret) throws GeneralSecurityException {
        key = createKey(secret);
        checkProcessing("data"); // Use something that does not match the cipher block size
    }

    /**
     * Encrypts a string.
     *
     * @param string
     *            the string to encrypt. It must not be {@code null}.
     *
     * @return the result
     */
    public byte[] encrypt(String string) {
        Objects.requireNonNull(string);

        try {
            final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, CIPHER_IV);
            return cipher.doFinal(string.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            // Should not happen due to the check
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypts a message into a string payload.
     *
     * @param message
     *            the message to decrypt. It must not be {@code null}.
     *
     * @return the string payload
     */
    public String decrypt(byte[] message) {
        return decrypt(ByteBuffer.wrap(message));
    }

    /**
     * Decrypts a message into a string payload.
     *
     * @param message
     *            the message to decrypt. It must not be {@code null}.
     *
     * @return the string payload
     */
    public String decrypt(ByteBuffer message) {
        Objects.requireNonNull(message);

        try {
            final Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORM);
            cipher.init(Cipher.DECRYPT_MODE, key, CIPHER_IV);
            final ByteBuffer output = ByteBuffer.allocate(message.remaining());
            cipher.doFinal(message, output);
            output.flip();
            return StandardCharsets.UTF_8.decode(output).toString();
        } catch (GeneralSecurityException e) {
            // Should not happen due to the check
            throw new RuntimeException(e);
        }
    }

    private void checkProcessing(String dummy) throws GeneralSecurityException {
        if (dummy.equals(decrypt(encrypt(dummy)))) {
            return;
        }

        throw new GeneralSecurityException("Encryption/decryption check failed.");
    }

    private static SecretKey createKey(String targetSecret) throws GeneralSecurityException {
        if (targetSecret.isEmpty()) {
            throw new IllegalArgumentException("Empty SECRET supplied.");
        }

        final MessageDigest digest = MessageDigest.getInstance(SECRET_TRANSFORM);
        return new SecretKeySpec(digest.digest(targetSecret.getBytes(StandardCharsets.UTF_8)), CIPHER_ALGORITHM);
    }
}

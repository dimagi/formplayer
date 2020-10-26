package org.commcare.formplayer.tests;

import org.commcare.formplayer.beans.EncryptStringResponseBean;
import org.commcare.formplayer.util.Constants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.commcare.formplayer.utils.TestContext;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
        classes = {TestContext.class}
)
public class EncryptStringTests extends BaseTestClass {
    public EncryptStringTests() {
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    static final int TAG_LENGTH_BIT = 128;
    static final int IV_LENGTH_BYTE = 12;
    static final int KEY_LENGTH_BIT = 256;

    private SecretKey generateSecretKey(int keyLength) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keyLength, SecureRandom.getInstanceStrong());
        return keyGen.generateKey();
    }

    private String extractAndDecodeMessage(String output, SecretKey secretKey) throws Exception {
        Base64.Decoder messageDecoder = Base64.getUrlDecoder();
        byte[] messageBytes = messageDecoder.decode(output);

        ByteBuffer bb = ByteBuffer.wrap(messageBytes);
        byte[] iv = new byte[IV_LENGTH_BYTE];
        bb.get(iv);

        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        byte[] plainText = cipher.doFinal(cipherText);
        return new String(plainText, StandardCharsets.UTF_8);
    }

    @Test
    public void testEncryptShortMessage() throws Exception {
        final String message = "A short message to be encrypted";

        SecretKey secretKey = generateSecretKey(KEY_LENGTH_BIT);
        EncryptStringResponseBean encryptStringResponse =
            this.encryptString(message,
                               Base64.getUrlEncoder().encodeToString(secretKey.getEncoded()),
                               "AES");

        assert encryptStringResponse.getStatus().equals(Constants.ENCRYPT_STRING_SUCCESS);
        String outputMessage = extractAndDecodeMessage(encryptStringResponse.getOutput(), secretKey);
        assert message.equals(outputMessage);
    }

    @Test
    public void testEncryptLongMessage() throws Exception {
        final String message = "A longer message to be encrypted by the AES GCM method, which will test that somewhat longer messages can be correctly encrypted";

        SecretKey secretKey = generateSecretKey(KEY_LENGTH_BIT);
        EncryptStringResponseBean encryptStringResponse =
            this.encryptString(message,
                               Base64.getUrlEncoder().encodeToString(secretKey.getEncoded()),
                               "AES");

        assert encryptStringResponse.getStatus().equals(Constants.ENCRYPT_STRING_SUCCESS);
        String outputMessage = extractAndDecodeMessage(encryptStringResponse.getOutput(), secretKey);
        assert message.equals(outputMessage);
    }

    @Test
    public void testEncryptWithBadAlgorithm() throws Exception {
        final String message = "A short message to be encrypted";

        SecretKey secretKey = generateSecretKey(KEY_LENGTH_BIT);
        EncryptStringResponseBean encryptStringResponse =
            this.encryptString(message,
                               Base64.getUrlEncoder().encodeToString(secretKey.getEncoded()),
                               "DES");

        assert encryptStringResponse.getStatus().equals(Constants.ENCRYPT_STRING_FAILURE);
        String output = encryptStringResponse.getOutput();
        assert encryptStringResponse.getOutput().equals("Unknown algorithm \"DES\" for encrypt_string");
    }

    @Test
    public void testEncryptWithWrongKeyLength() throws Exception {
        final String message = "A short message to be encrypted";

        SecretKey secretKey = generateSecretKey(KEY_LENGTH_BIT/2);
        EncryptStringResponseBean encryptStringResponse =
            this.encryptString(message,
                               Base64.getUrlEncoder().encodeToString(secretKey.getEncoded()),
                               "AES");

        assert encryptStringResponse.getStatus().equals(Constants.ENCRYPT_STRING_FAILURE);
        String output = encryptStringResponse.getOutput();
        assert encryptStringResponse.getOutput().equals("Key should be 256 bits long, not 128");
    }
}

package org.commcare.formplayer.beans;

import org.commcare.formplayer.beans.menus.BaseResponseBean;
import org.commcare.formplayer.util.Constants;

import io.swagger.annotations.ApiModel;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;


/**
 * Response to a string encrypt request.
 *
 */
@ApiModel("Session Response")
public class EncryptStringResponseBean extends BaseResponseBean {

    private String status;  // Success or failure of the call.
    private String output;  // Encrypted message on success or error message on failure.

    EncryptStringResponseBean() {}

    public EncryptStringResponseBean(EncryptStringRequestBean request) throws Exception {
        final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
        final int TAG_LENGTH_BIT = 128;
        final int IV_LENGTH_BYTE = 12;
        final int KEY_LENGTH_BIT = 256;

        if (!request.getAlgorithm().equals("AES")) {
            this.status = Constants.ENCRYPT_STRING_FAILURE;
            this.output = String.format("Unknown algorithm \"%s\" for encrypt_string", request.getAlgorithm());
            return;
        }

        Base64.Decoder keyDecoder = Base64.getUrlDecoder();
        byte[] keyBytes = keyDecoder.decode(request.getKey());
        if (8 * keyBytes.length != KEY_LENGTH_BIT) {
            this.status = Constants.ENCRYPT_STRING_FAILURE;
            this.output = String.format("Key should be %d bits long, not %d", KEY_LENGTH_BIT, 8 * keyBytes.length);
            return;
        }
        SecretKey secret = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[IV_LENGTH_BYTE];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        byte[] encryptedMessage = cipher.doFinal(request.getMessage().getBytes(StandardCharsets.UTF_8));
        byte[] ivPlusMessage = ByteBuffer.allocate(iv.length + encryptedMessage.length)
                .put(iv)
                .put(encryptedMessage)
                .array();
        this.status = Constants.ENCRYPT_STRING_SUCCESS;
        Base64.Encoder outputEncoder = Base64.getUrlEncoder();
        this.output = outputEncoder.encodeToString(ivPlusMessage);
    }

    @Override
    public String toString() {
        return "EncryptStringResponseBean [status=" + status + ", output=" + output + "]";
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

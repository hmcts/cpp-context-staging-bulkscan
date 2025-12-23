package uk.gov.moj.cpp.bulkscan.azure.function.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Aes256CbcService {

    public byte[] downloadAndDecryptAes256Cbc(
            String fileUrl,
            String keyString,
            String ivString,
            boolean keyIsBase64,
            boolean ivIsBase64
    ) throws Exception {
        byte[] encrypted = downloadFile(fileUrl);
        return decryptAes256Cbc(encrypted, keyString, ivString, keyIsBase64, ivIsBase64);
    }
    /**
     * Download the file at fileUrl and decrypt it with AES-256-CBC.
     *
     * @param encrypted   encrypted file byte array
     * @param keyString   encryption key as hex, Base64, or raw text (if not 32 bytes you'll get
     *                    SHA-256(keyString))
     * @param ivString    IV as hex or Base64 (must be 16 bytes after decoding)
     * @param keyIsBase64 true if keyString is Base64; false if it's hex or raw text
     * @param ivIsBase64  true if ivString is Base64; false if it's hex
     * @return decrypted bytes
     * @throws Exception on error (IO, crypto, invalid key/iv sizes)
     */
    protected byte[] decryptAes256Cbc(
            byte[] encrypted,
            String keyString,
            String ivString,
            boolean keyIsBase64,
            boolean ivIsBase64
    ) throws Exception {
        // Convert key string to bytes
        byte[] keyBytes;
        if (keyIsBase64) {
            keyBytes = Base64.getDecoder().decode(keyString);
        } else {
            // try hex first (if valid hex length), otherwise treat as raw/passphrase and hash
            if (isHexString(keyString)) {
                keyBytes = hexStringToByteArray(keyString);
            } else {
                // treat as raw/passphrase -> will be hashed to 32 bytes
                keyBytes = keyString.getBytes("UTF-8");
            }
        }

        // If key is not exactly 32 bytes, derive a 32-byte key by SHA-256(keyBytes)
        if (keyBytes.length != 32) {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            keyBytes = sha256.digest(keyBytes);
        }

        // Convert IV string to bytes
        byte[] ivBytes;
        if (ivIsBase64) {
            ivBytes = Base64.getDecoder().decode(ivString);
        } else {
            if (isHexString(ivString)) {
                ivBytes = hexStringToByteArray(ivString);
            } else {
                // If IV not hex and not base64, assume raw bytes (UTF-8) -- but IV must be 16 bytes
                ivBytes = ivString.getBytes("UTF-8");
            }
        }

        if (ivBytes.length != 16) {
            throw new IllegalArgumentException("IV must be 16 bytes long. Current length: " + ivBytes.length);
        }

        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        return cipher.doFinal(encrypted);
    }

    // Download file into byte[]
    protected byte[] downloadFile(String fileUrl) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);
        connection.setRequestMethod("GET");
        connection.setDoInput(true);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Failed to download file, HTTP response code: " + responseCode);
        }

        try (InputStream in = connection.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    // Helper: detect hex string (only 0-9 a-f A-F and even length)
    private boolean isHexString(String s) {
        if (s == null) return false;
        String trimmed = s.trim();
        if (trimmed.length() % 2 != 0) return false;
        return trimmed.matches("(?i)^[0-9a-f]+$");
    }

    // Helper: convert hex string to byte[]
    private byte[] hexStringToByteArray(String s) {
        String trimmed = s.trim();
        int len = trimmed.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(trimmed.charAt(i), 16) << 4)
                    + Character.digit(trimmed.charAt(i + 1), 16));
        }
        return data;
    }
}

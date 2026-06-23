package com.example.todolist.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * AES/GCM encryption backed by Android Keystore.
 * Keystore keys survive app updates but are wiped on app uninstall.
 */
public class CryptoUtils {

    private static final String TAG = "CryptoUtils";
    private static final String KEY_ALIAS = "todo_app_pwd_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits

    /**
     * Encrypt plaintext → Base64( IV + ciphertext ).
     * Returns null on failure (keystore unavailable, etc.).
     */
    public static String encrypt(String plaintext) {
        try {
            SecretKey key = getOrCreateKey();
            if (key == null) {
                Log.e(TAG, "encrypt: failed to get or create keystore key");
                return null;
            }
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] iv = cipher.getIV();
            byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // Prepend IV to ciphertext, then Base64-encode the whole blob
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            String result = Base64.encodeToString(combined, Base64.NO_WRAP);
            Log.d(TAG, "encrypt: success, output length=" + result.length());
            return result;
        } catch (Exception e) {
            Log.e(TAG, "encrypt failed", e);
            return null;
        }
    }

    /**
     * Decrypt Base64( IV + ciphertext ) → plaintext.
     * Returns null on failure.
     */
    public static String decrypt(String encoded) {
        try {
            SecretKey key = getKey();
            if (key == null) {
                Log.e(TAG, "decrypt: keystore key not found");
                return null;
            }

            byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);

            // First 12 bytes = IV (GCM standard)
            byte[] iv = new byte[12];
            System.arraycopy(combined, 0, iv, 0, 12);

            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            String result = new String(cipher.doFinal(encrypted), "UTF-8");
            Log.d(TAG, "decrypt: success");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "decrypt failed", e);
            return null;
        }
    }

    // ---- internal ----

    private static SecretKey getOrCreateKey() throws Exception {
        SecretKey key = getKey();
        if (key != null) {
            Log.d(TAG, "getOrCreateKey: existing key found");
            return key;
        }

        Log.d(TAG, "getOrCreateKey: generating new AES-256 key in Android Keystore");
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        keyGenerator.init(
            new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        );
        SecretKey generated = keyGenerator.generateKey();
        Log.d(TAG, "getOrCreateKey: key generated successfully");
        return generated;
    }

    private static SecretKey getKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_ALIAS, null);
            Log.d(TAG, "getKey: " + (key != null ? "found" : "not found"));
            return key;
        } catch (Exception e) {
            Log.e(TAG, "getKey failed", e);
            return null;
        }
    }
}

package com.cbc.tor_android_v1.manager;

import android.content.Context;
import android.content.SharedPreferences;

import android.util.Log;

import org.libsodium.jni.NaCl;

import org.libsodium.jni.crypto.Box;
import org.libsodium.jni.keys.KeyPair;
import org.libsodium.jni.keys.PrivateKey;
import org.libsodium.jni.keys.PublicKey;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EncryptionManager {
    private static final String TAG = "EncryptionManager";
    private static final String PREFS_NAME = "CryptoPrefs";
    private static final String PRIVATE_KEY_PREF = "private_key";
    private static final String PUBLIC_KEY_PREF = "public_key";

    private Context context;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    public EncryptionManager(Context context) {
        this.context = context;


    }

    public void loadOrGenerateKeys() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
//       String privateKeyHex = prefs.getString(PRIVATE_KEY_PREF, "cbcd46b9c762adc91474931a14f4a0138afdc0bedda3bb67ca63f4fa60f0650b");
//        String publicKeyHex = prefs.getString(PUBLIC_KEY_PREF, "11ca532c313aaaaf82306d60bcffad22597a1f2d082aa75391fb20f2b49e202c");

        String privateKeyHex = prefs.getString(PRIVATE_KEY_PREF , null);
        String publicKeyHex = prefs.getString(PUBLIC_KEY_PREF , null);
        // Make sure Sodium is initialized
        NaCl.sodium();

        if (privateKeyHex != null && publicKeyHex != null) {
            try {
                privateKey = new PrivateKey(privateKeyHex);
                publicKey = new PublicKey(publicKeyHex);
                Log.d(TAG, "Loaded existing keys successfully"+ publicKey.toString());
                Log.d(TAG, "Loaded existing keys successfully" + privateKey.toString());
                Log.d(TAG, "Loaded existing keys successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error loading saved keys, generating new ones", e);
                generateNewKeys(prefs);
            }
        } else {
            generateNewKeys(prefs);
        }
    }

    public String getPrivateKeyPref (){
        //cbcd46b9c762adc91474931a14f4a0138afdc0bedda3bb67ca63f4fa60f0650b
        SharedPreferences pref  = context.getSharedPreferences(PREFS_NAME , Context.MODE_PRIVATE);
        return  pref.getString(PRIVATE_KEY_PREF, null);
    }
    public String getStoredPublicKeyHex() {
        //11ca532c313aaaaf82306d60bcffad22597a1f2d082aa75391fb20f2b49e202c
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        return prefs.getString(PUBLIC_KEY_PREF, null);
    }
    private void generateNewKeys(SharedPreferences prefs) {
        KeyPair keyPair = new KeyPair();
        privateKey = keyPair.getPrivateKey();
        publicKey = keyPair.getPublicKey();

        // Save the keys
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PRIVATE_KEY_PREF, privateKey.toString());
        editor.putString(PUBLIC_KEY_PREF, publicKey.toString());
        editor.apply();

        Log.d(TAG, "Generated and saved new key pair");
        Log.d(TAG, "Public key: " + publicKey.toString());
    }


    public String getPublicKeyHex() {
        return publicKey.toString();
    }

    /**
     * Encrypt a message using recipient's X25519 public key
     *
     * @param message Plain text message
     * @param receiverPublicKeyHex Recipient's X25519 public key in hex format
     * @return Base64 encoded encrypted message with nonce
     */
    public String encryptMessage(String message, String receiverPublicKeyHex) {
        try {
            Log.d(TAG, "Encrypting message with recipient X25519 public key: " + receiverPublicKeyHex);

            // Validate hex string
            if (receiverPublicKeyHex == null || !receiverPublicKeyHex.matches("^[0-9a-fA-F]{64}$")) {
                throw new IllegalArgumentException("Invalid public key hex string");
            }

            // Convert hex to byte array
            byte[] receiverPublicKeyBytes = hexStringToByteArray(receiverPublicKeyHex);

            // Create PublicKey object
            PublicKey receiverPublicKey = new PublicKey(receiverPublicKeyBytes);

            // Create Box for encryption
            Box box = new Box(receiverPublicKey.toBytes(), privateKey.toBytes());

            // Generate 24-byte nonce
            byte[] nonce = new byte[24];
            NaCl.sodium().randombytes(nonce, 24);

            // Encrypt
            byte[] encrypted = box.encrypt(nonce, message.getBytes());

            // Prepend nonce to encrypted message
            byte[] encryptedWithNonce = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, encryptedWithNonce, 0, nonce.length);
            System.arraycopy(encrypted, 0, encryptedWithNonce, nonce.length, encrypted.length);

            Log.d(TAG, "Message encrypted successfully");

            // Convert to hexadecimal string
            return bytesToHex(encryptedWithNonce);

        } catch (Exception e) {
            Log.e(TAG, "Encryption failed", e);
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    // Converts a hex string to byte array
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    // Converts byte array to hex string
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public String decryptMessage(String encryptedHex, String senderPublicKeyHex) {
        try {
            Log.d(TAG, "Decrypting message...");


           // String senderPublicKeyHex = "ac029cae1e9511d84fcd27b62abd89a31fc8962b5660e430ac359c510855e81b"; //

            if (senderPublicKeyHex == null || !senderPublicKeyHex.matches("^[0-9a-fA-F]{64}$")) {
                throw new IllegalArgumentException("Invalid or missing sender public key");
            }

            // Decode keys and message
            byte[] senderPublicKeyBytes = hexStringToByteArray(senderPublicKeyHex);
            byte[] encryptedWithNonce = hexStringToByteArray(encryptedHex);

            // Extract nonce (first 24 bytes)
            byte[] nonce = Arrays.copyOfRange(encryptedWithNonce, 0, 24);

            // Extract ciphertext
            byte[] encryptedMessage = Arrays.copyOfRange(encryptedWithNonce, 24, encryptedWithNonce.length);

            // Construct sender's public key and Box
            PublicKey constructsenderPublicKey = new PublicKey(senderPublicKeyBytes);
            Box box = new Box(constructsenderPublicKey.toBytes(), privateKey.toBytes());

            // Decrypt
            byte[] decrypted = box.decrypt(nonce, encryptedMessage);


            String message = new String(decrypted, StandardCharsets.UTF_8);
            Log.d("TorMessageServer ", "Message decrypted successfully" + message );
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            Log.e(TAG, "Decryption failed", e);
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }



}
package com.redrosecps.collect.android.utilities;

import com.redrosecps.collect.android.BuildConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import java.io.*;

public class CryptoFileHandler {

    public static boolean isEncryptionEnabled(){
        return BuildConfig.IS_ENCRYPTION_ENABLED;
    }

    public static byte[] encryptData(byte[] data,SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(KeyManager.AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    private static byte[] decryptData(byte[] encryptedData, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance(KeyManager.AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(encryptedData);
    }

    public static byte[] readAndDecryptFile(String instanceName) throws IOException {
        File encryptedFile = new File(instanceName);
        if (!encryptedFile.exists()) {
            throw new IOException("File not found: " + encryptedFile.getAbsolutePath());
        }

        SecretKey secretKey = KeyManager.getKeyFromPrefs();
        if (secretKey == null) {
            throw new IOException("No encryption key found in SharedPreferences.");
        }

        byte[] encryptedData;
        try (FileInputStream fis = new FileInputStream(encryptedFile);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024]; // 1 KB'lik buffer ile okuma
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            encryptedData = bos.toByteArray(); // Tüm veriyi al

        } catch (IOException e) {
            throw new IOException("File read error: " + e.getMessage(), e);
        }

        try {
            return decryptData(encryptedData, secretKey);
        } catch (Exception e) {
            throw new IOException("Decryption error: " + e.getMessage(), e);
        }
    }
}


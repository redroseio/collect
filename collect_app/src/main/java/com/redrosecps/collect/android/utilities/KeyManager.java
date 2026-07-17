package com.redrosecps.collect.android.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.redrosecps.collect.android.application.Collect;

import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class KeyManager {
    private static final String PREF_NAME = "SecurePrefs";
    private static final String KEY_ALIAS = "AES_KEY";
    public static final String AES_ALGORITHM = "AES";

    public static SecretKey generateAndStoreKey() throws Exception {
        SecretKey secretKey = generateKey();
        saveKeyToPrefs(secretKey);
        return secretKey;
    }

    private static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(256, new SecureRandom());
        return keyGen.generateKey();
    }

    private static void saveKeyToPrefs(SecretKey secretKey) {
        SharedPreferences prefs = Collect.getInstance().getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String keyString = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
        editor.putString(KEY_ALIAS, keyString);
        editor.apply();
    }

    public static SecretKey getKeyFromPrefs() {
        SharedPreferences prefs = Collect.getInstance().getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String keyString = prefs.getString(KEY_ALIAS, null);

        if (keyString == null) {
            return null;
        }

        byte[] decodedKey = Base64.decode(keyString, Base64.DEFAULT);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, AES_ALGORITHM);
    }


}

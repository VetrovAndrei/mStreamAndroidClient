package com.example.mstream.mstreamandroidclient.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenSaver {
    private final static String JWT_KEY = "com.example.mstream.mstreamandroidclient.JWT_KEY";
    private final static String JWT = "com.example.mstream.mstreamandroidclient.JWT";

    public static String getToken(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(JWT_KEY, Context.MODE_PRIVATE);
        return prefs.getString(JWT, "");
    }

    public static void setToken(Context c, String token) {
        SharedPreferences prefs = c.getSharedPreferences(JWT_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(JWT, token);
        editor.apply();
    }
}

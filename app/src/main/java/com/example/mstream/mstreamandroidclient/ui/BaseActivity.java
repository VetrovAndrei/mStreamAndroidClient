package com.example.mstream.mstreamandroidclient.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.example.mstream.mstreamandroidclient.helpers.TokenSaver;
import java.util.Objects;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = TokenSaver.getToken(BaseActivity.this);

        if(Objects.equals(token, "")) {
            Intent loginPage = new Intent(this, LoginActivity.class);
            loginPage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            loginPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            loginPage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginPage);
            finish();
        } else {
            Intent menuPage = new Intent(this, MenuNav.class);
            menuPage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            menuPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            menuPage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(menuPage);
        }
    }
}

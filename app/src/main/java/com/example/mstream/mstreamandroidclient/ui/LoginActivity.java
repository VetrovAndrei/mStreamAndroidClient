package com.example.mstream.mstreamandroidclient.ui;

import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.mstream.mstreamandroidclient.api.async_tasks.GetWebToken;
import com.example.mstream.mstreamandroidclient.R;
import com.example.mstream.mstreamandroidclient.api.interfaces.GetWebTokenInterface;
import com.example.mstream.mstreamandroidclient.helpers.TokenSaver;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity implements GetWebTokenInterface {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        final Button loginButton = (Button) findViewById(R.id.loginButton);
        final EditText usernameText = (EditText) findViewById(R.id.usernameText);
        final EditText passwordText = (EditText) findViewById(R.id.passwordText);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onClick(View v) {
                GetWebToken getJWTTask = new GetWebToken();
                getJWTTask.delegate = LoginActivity.this;
                JSONObject data = new JSONObject();
                JSONObject obj = new JSONObject();
                try {
                    data.put("username", usernameText.getText().toString());
                    data.put("password", passwordText.getText().toString());
                    obj.put("data", data);
                    obj.put("addr", "/login");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                getJWTTask.execute(obj);
            }
        });
    }

    @Override
    public void getJWTResponse(JSONObject response) {
        try {
            if(response != null) {
                TokenSaver.setToken(this, response.getString("token"));
                Intent menu = new Intent(this, MenuActivity.class);
                menu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                menu.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                menu.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(menu);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setMessage("Incorrect login or password!")
                        .setTitle("Log in error");
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        } catch (Exception ignored) {
        }
    }
}
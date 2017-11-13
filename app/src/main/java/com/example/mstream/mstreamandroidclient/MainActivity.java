package com.example.mstream.mstreamandroidclient;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

interface AsyncResponse {
    void getJWTResponse(JSONObject response);
}

public class MainActivity extends AppCompatActivity implements  AsyncResponse {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button loginButton = (Button) findViewById(R.id.loginButton);
        final EditText usernameText = (EditText) findViewById(R.id.usernameText);
        final EditText passwordText = (EditText) findViewById(R.id.passwordText);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
            @Override
            public void onClick(View v) {
                GetWebToken getJWTTask = new GetWebToken();
                getJWTTask.delegate = MainActivity.this;
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
            Globals.jwt = response.getString("token");
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(Globals.jwt)
                    .setTitle("JWT");
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
        }
    }
}
package com.example.mstream.mstreamandroidclient.ui;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.example.mstream.mstreamandroidclient.R;
import com.example.mstream.mstreamandroidclient.api.async_tasks.GetPlaylists;
import com.example.mstream.mstreamandroidclient.api.interfaces.GetPlaylistsInterface;
import com.example.mstream.mstreamandroidclient.helpers.TokenSaver;

import org.json.JSONException;
import org.json.JSONObject;

public class PlaylistActivity extends AppCompatActivity  implements GetPlaylistsInterface {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);
        GetPlaylists getPlaylistsTask = new GetPlaylists();
        getPlaylistsTask.delegate = PlaylistActivity.this;
        JSONObject data = new JSONObject();
        JSONObject obj = new JSONObject();
        String token = TokenSaver.getToken(PlaylistActivity.this);
        try {
            obj.put("data", data);
            obj.put("addr", "/playlist/getall?token=" + token);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        getPlaylistsTask.execute(obj);
    }

    @Override
    public void getJWTResponse(JSONObject response) {
        try {
            if(response != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistActivity.this);
                builder.setTitle("Важное сообщение!")
                        .setCancelable(false)
                        .setMessage(response.toString())
                        .setNegativeButton("ОК",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {

            }
        } catch (Exception ignored) {
        }
    }
}

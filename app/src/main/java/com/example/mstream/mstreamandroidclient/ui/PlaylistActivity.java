package com.example.mstream.mstreamandroidclient.ui;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.mstream.mstreamandroidclient.R;
import com.example.mstream.mstreamandroidclient.api.async_tasks.GetPlaylists;
import com.example.mstream.mstreamandroidclient.api.interfaces.GetPlaylistsInterface;
import com.example.mstream.mstreamandroidclient.helpers.TokenSaver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PlaylistActivity extends AppCompatActivity  implements GetPlaylistsInterface {

    private ArrayAdapter<String> adapter;
    private ListView lvPlayLists;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);


        // находим список
        lvPlayLists = (ListView) findViewById(R.id.LV_playlists);

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
    public void getJWTResponse(JSONArray response) {
        try {
            if(response != null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PlaylistActivity.this);
                builder.setTitle("Важное сообщение!")
                        .setCancelable(false)
                        .setMessage(response.getString(0))
                        .setNegativeButton("ОК",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
                //TODO переписать на что-нибудь поэлегантей
                String[] playlists = new String[response.length()];
                for( int i = 0; i < response.length();i++)
                    playlists[i] = response.getJSONObject(i).getString("name");
                adapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1, playlists);

                // присваиваем адаптер списку
                lvPlayLists.setAdapter(adapter);

            } else {


            }
        } catch (Exception ignored) {
        }
    }
}

package com.example.mstream.mstreamandroidclient.api.async_tasks;

import android.os.AsyncTask;
import com.example.mstream.mstreamandroidclient.api.interfaces.GetPlaylistsInterface;

import org.json.JSONArray;
import org.json.JSONObject;

public class GetPlaylists extends AsyncTask<JSONObject, Integer, JSONArray> {
    public GetPlaylistsInterface delegate;
    private ApiRequest apiRequest = new ApiRequest();

    @Override
    protected JSONArray doInBackground(JSONObject... params) {
        return apiRequest.doGetRequest(params);
    }

    @Override
    protected void onPostExecute(JSONArray response) {
        delegate.getJWTResponse(response);
    }
}

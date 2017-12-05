package com.example.mstream.mstreamandroidclient.api.async_tasks;

import android.os.AsyncTask;
import com.example.mstream.mstreamandroidclient.api.interfaces.GetPlaylistsInterface;

import org.json.JSONObject;

public class GetPlaylists extends AsyncTask<JSONObject, Integer, JSONObject> {
    public GetPlaylistsInterface delegate;
    private ApiRequest apiRequest = new ApiRequest();

    @Override
    protected JSONObject doInBackground(JSONObject... params) {
        return apiRequest.doGetRequest(params);
    }

    @Override
    protected void onPostExecute(JSONObject response) {
        delegate.getJWTResponse(response);
    }
}

package com.example.mstream.mstreamandroidclient.api.async_tasks;

import android.os.AsyncTask;
import com.example.mstream.mstreamandroidclient.api.interfaces.GetWebTokenInterface;
import org.json.JSONObject;

public class GetWebToken extends AsyncTask<JSONObject, Integer, JSONObject> {
    public GetWebTokenInterface delegate;
    private ApiRequest apiRequest = new ApiRequest();

    @Override
    protected JSONObject doInBackground(JSONObject... params) {
        return apiRequest.doPostRequest(params);
    }

    @Override
    protected void onPostExecute(JSONObject response) {
        delegate.getJWTResponse(response);
    }
}

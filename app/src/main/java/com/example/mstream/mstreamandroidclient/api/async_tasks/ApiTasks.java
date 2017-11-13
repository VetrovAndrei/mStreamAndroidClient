package com.example.mstream.mstreamandroidclient.api.async_tasks;

import com.example.mstream.mstreamandroidclient.Globals;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

class ApiRequest {
    JSONObject doPostRequest(JSONObject... params) {
        JSONObject result = null;
        try {
            String address = Globals.address + params[0].getString("addr");
            URL object = new URL(address);
            HttpURLConnection con = (HttpURLConnection) object.openConnection();
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestMethod("POST");
            JSONObject cred = params[0].getJSONObject("data");
            OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
            wr.write(cred.toString());
            wr.flush();
            StringBuilder sb = new StringBuilder();
            int HttpResult = con.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                result = new JSONObject(sb.toString());
            } else {
                System.out.println(con.getResponseMessage());
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
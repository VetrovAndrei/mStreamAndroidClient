package com.example.mstream.mstreamandroidclient.api.async_tasks;

import com.example.mstream.mstreamandroidclient.Globals;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

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
    //TODO наподумать - а для какой цели тут вообще неопределенное количество параметров?
    JSONArray doGetRequest(JSONObject... params) {
        JSONObject result = null;
        JSONArray JSON_arr = null;
        try {

            String address = Globals.address + params[0].getString("addr");
            URL object = new URL(address);
            HttpURLConnection con = (HttpURLConnection) object.openConnection();
            con.setRequestMethod("GET");
            StringBuilder sb = new StringBuilder();
            int HttpResult = con.getResponseCode();
            if (HttpResult == HttpURLConnection.HTTP_OK) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"));
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                String text = sb.toString();
                JSON_arr = new JSONArray(text);
                //result = new JSONObject(text);
            } else {
                System.out.println(con.getResponseMessage());
            }
        } catch (Exception ignored) {


            System.out.println("Error!!! :" + ignored.getMessage());
        }
       // return result;
        return JSON_arr;
    }
}
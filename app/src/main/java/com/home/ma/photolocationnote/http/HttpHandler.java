package com.home.ma.photolocationnote.http;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpHandler {

    /**
     * refrence of HttpListener interface
     */

    private HttpListener httpListener;
    /**
     * url for example http://wwww.hemelix.com
     */
    private String urlstring = "";
    /**
     * hold the http response
     */
    private String resMessage = "No Response.";
    /**
     * response code
     */
    private int resCode = -1;


    private static final String LOG = HttpHandler.class.getSimpleName();


    public HttpHandler(String urlstring) {
        this.urlstring = urlstring;
    }

    /**
     * @return the response
     */
    public String getResponse() {
        return resMessage;
    }

    /**
     * Return Response Code
     * 
     * @return
     */
    public int getResCode() {
        return resCode;
    }

    /**
     * @param httpListener
     *            add the listener for notify the response
     */
    public void addHttpLisner(HttpListener httpListener) {
        this.httpListener = httpListener;
    }

    /**
     * send the http request
     */

    public void sendRequest() {
        HttpURLConnection con = null;
        try {
            URL url = new URL(urlstring);
            con = (HttpURLConnection) url.openConnection();
            resCode = con.getResponseCode();
            resMessage = processJSON(con.getInputStream());
        } catch (Exception e) {
            Log.i(LOG, e.getMessage());
        }
        httpListener.notifyHTTPResponse(this);
    }

    private String processJSON(InputStream in) {
        String result = "";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            in.close();
            result = sb.toString();
        } catch (Exception e) {
            Log.e("aaa", "Error converting result " + e.toString());
            Log.i("aaa", "error msg: "+e.getMessage());
        }
        return result;
    }
}
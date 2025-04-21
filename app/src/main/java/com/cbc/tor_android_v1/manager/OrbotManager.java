package com.cbc.tor_android_v1.manager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;

public class OrbotManager {

    private Context context;
    private String TAG = "OrbotManager";


    public OrbotManager(Context context) {
        this.context = context;
    }


    public String connectToOnionAddress(Context context, String onionUrl) {
        Log.d(TAG," connectToOnionAddress : ");
        try {
            Log.d(TAG," try : ");
            StrongOkHttpClientBuilder clientBuilder = StrongOkHttpClientBuilder.forMaxSecurity(context);

            Intent status = OrbotHelper.getOrbotStartIntent(context); // Retrieve actual Orbot status

// Now build the OkHttpClient with the correct Orbot status
            OkHttpClient client = clientBuilder.build(status);



            // Create a request to the onion URL
            Request request = new Request.Builder()
                    .url("http://" + onionUrl)  // Make sure to prepend "http://" to the onion URL
                    .build();

            // Execute the request and get the response
            Response response = client.newCall(request).execute();

            // Check if the request was successful
            if (response.isSuccessful()) {
                return response.body().string();  // Return the response body as a string
            } else {
                return "Request failed with status code: " + response.code();
            }
        } catch (IOException e) {
            Log.d(TAG," catch 1: " + e.toString());
            e.printStackTrace();
            return "Error connecting to the onion address: " + e.getMessage();
        } catch (Exception e) {
            Log.d(TAG," catch 2 : " + e.toString());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

}

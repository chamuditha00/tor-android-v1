package com.cbc.tor_android_v1.manager;

import static android.content.ContentValues.TAG;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkManager {

    private final OkHttpClient client;

    public NetworkManager() {
        client = new OkHttpClient();
    }

    public interface NetworkCallback {
        void onSuccess(String publicKey,String  onionAddress,String senderId);
        void onFailure(String errorMessage);
    }

    public void makeRequest(String url, NetworkCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle network failure
                Log.d("...NetworkManager","Network error: " + e.getMessage());
                callback.onFailure("Network error: " + e.getMessage());
            }


            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
                        callback.onFailure("Unexpected code: " + response.code());
                        Log.d("...NetworkManager", "Unexpected code: " + response.code());
                        return;
                    }

                    if (response.body() != null) {
                        String responseBody = response.body().string(); // Get the response body as a string

                        try {
                            // Convert response string to JSONObject
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            // Extract individual parameters
                            String publicKey = jsonResponse.optString("public_key", "");
                            String onionAddress = jsonResponse.optString("onion_address", "");
                            String senderId = jsonResponse.optString("sender_id", "");

                            // Log or pass the values to callback
                            Log.d("...NetworkManager", "Public Key: " + publicKey);
                            Log.d("...NetworkManager", "Onion Address: " + onionAddress);
                            Log.d("...NetworkManager", "Sender ID: " + senderId);

                            // You can now use these variables as needed in your application

                            // Optionally, pass the extracted values to the callback
                            callback.onSuccess( publicKey , onionAddress ,senderId);

                        } catch (Exception e) {
                            callback.onFailure("Error parsing JSON response: " + e.getMessage());
                            Log.d("...NetworkManager", "Error parsing JSON response: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Empty response body");
                    }
                } catch (IOException e) {
                    callback.onFailure("Error reading response: " + e.getMessage());
                    Log.d("...NetworkManager", "Error reading response: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
//            @Override
//            public void onResponse(Call call, Response response) {
//                try {
//                    if (!response.isSuccessful()) {
//
//                        callback.onFailure("Unexpected code: " + response.code());
//                        Log.d("...NetworkManager","Unexpected code: " + response.code());
//                        return;
//                    }
//
//                    if (response.body() != null) {
//                        callback.onSuccess(response.body());
//                    } else {
//                        callback.onFailure("Empty response body");
//                    }
//                } catch (IOException e) {
//                    callback.onFailure("Error reading response: " + e.getMessage());
//                    Log.d("...NetworkManager","Error reading response: " + e.getMessage());
//                } finally {
//                    response.close();
//                }
//            }
        });
    }
}
package com.cbc.tor_android_v1.manager;


import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Proxy;

import androidx.annotation.NonNull;

import net.freehaven.tor.control.TorControlConnection;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import info.guardianproject.netcipher.proxy.OrbotHelper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrbotHelperClass {

    private static final String ORBOT_PACKAGE_NAME = "org.torproject.android";
    // info.guardianproject.orbot
    private static final String TAG = "OrbotHelperClass";
    private static final String TOR_PROXY_HOST = "127.0.0.1";  // Orbot's local proxy
    private static final int TOR_PROXY_PORT_9050 = 9050;
    private static final int TOR_PROXY_PORT_9150 = 9150;

    private static final int OPEN_DURATION = 5000;

    private final Context context;

    private String onionAddress;

    public OrbotHelperClass(Context context) {
        this.context = context;
    }

    /**
     * Checks if Orbot is installed on the device.
     *
     * @return true if Orbot is installed, false otherwise.
     */
    public boolean isOrbotInstalled() {
        PackageManager pm = context.getApplicationContext().getPackageManager();
        try {
            pm.getPackageInfo(ORBOT_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }





    public void createHiddenService() {
        new Thread(() -> {
            final String CONTROL_HOST = "127.0.0.1";
            final int CONTROL_PORT = 9050; // Control port, not SOCKS proxy
            final String CONTROL_PASSWORD = "abc1234"; // Replace with your real password

            int maxRetries = 5;

            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try (Socket socket = new Socket(CONTROL_HOST, CONTROL_PORT)) {
                    Log.d("TorControl", "Connected to Tor control port on attempt " + attempt);

                    TorControlConnection conn = new TorControlConnection(socket);
                    conn.launchThread(true);

                    // Debug: read initial banner (optional)
                    BufferedReader preAuthReader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                    );
                    String bannerLine;
                    while ((bannerLine = preAuthReader.readLine()) != null) {
                        Log.d("TorControl", "Banner: " + bannerLine);
                        if (bannerLine.startsWith("250")) break;
                    }

                    // Authenticate with password
                    conn.authenticate(CONTROL_PASSWORD.getBytes());
                    Log.d("TorControl", "Authenticated successfully");

                    // Send ADD_ONION command
                    PrintWriter writer = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true
                    );
                    writer.print("ADD_ONION NEW:ED25519-V3 Port=80,127.0.0.1:8080\r\n");
                    writer.flush();

                    // Read response
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                    );
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Log.d("TorControl", "Reply: " + line);
                        if (line.startsWith("250-ServiceID=")) {
                            String onion = line.substring("250-ServiceID=".length()) + ".onion";
                            Log.i("TorControl", "Generated Onion Address: " + onion);
                            break;
                        }
                        if (line.equals("250 OK")) {
                            break;
                        }
                    }

                    break; // success, exit retry loop

                } catch (IOException e) {
                    Log.e("TorControl", "Attempt " + attempt + " failed: " + e.getMessage());
                    if (attempt == maxRetries) {
                        Log.e("TorControl", "Max retries reached. Giving up.");
                    } else {
                        try {
                            Thread.sleep(1000); // Wait before retry
                        } catch (InterruptedException ignored) {}
                    }

                } catch (Exception e) {
                    Log.e("TorControl", "Unexpected error creating hidden service", e);
                    break;
                }
            }
        }).start();
    }





    public void checkOrbotStatus(Consumer<Boolean> callback) {
        new Thread(() -> {
            boolean running = isOrbotRunning();
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(running));
        }).start();
    }

    public boolean isOrbotRunning() {
        return isTorPortOpen(TOR_PROXY_PORT_9050) || isTorPortOpen(TOR_PROXY_PORT_9150);
    }

    private boolean isTorPortOpen(int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(TOR_PROXY_HOST, port), OPEN_DURATION);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }



    /**
     * Launch Orbot if it's installed. Otherwise, redirects to Play Store for installation.
     */
    public void startOrbot() {
        if (isOrbotInstalled()) {
            if(requestStartTor(context)){
                Log.d(TAG, "Starting Orbot...");
                Toast.makeText(context, "Starting Orbot...", Toast.LENGTH_SHORT).show();
                requestHiddenService();
            }else{
                Toast.makeText(context, "Orbot not working", Toast.LENGTH_SHORT).show();
            }

        } else {
            redirectToOrbotInstall();
        }
    }


    public void requestHiddenService() {
        Log.d(TAG, "requestHiddenService...");
        Intent intent = new Intent("org.torproject.android.intent.action.REQUEST_HS_PORT");
        intent.setPackage("org.torproject.android");
        intent.putExtra("hs_port", 8080); // Your local server's port
        intent.putExtra("hs_name", "myHiddenService");

        // Now use the passed context to send the broadcast
        context.sendBroadcast(intent);
    }


    public boolean requestStartTor(Context context) {
        if (orbotInstalled(context)) {
            Log.d("OrbotHelper", "Requesting Orbot to start...");

            Intent intent = new Intent("org.torproject.android.START_TOR");
            intent.setPackage("org.torproject.android");

            try {
                context.sendBroadcast(intent);
                Log.d("OrbotHelper", "Success to send start request to Orbot!");
                return true;
            } catch (Exception e) {
                Log.d("OrbotHelper", "Failed to send start request to Orbot!", e);
                return false;
            }
        }
        Log.d("OrbotHelper", "orbotInstalled not installed");
        return false;
    }


    public static boolean orbotInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo("org.torproject.android", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            try {
                pm.getPackageInfo(ORBOT_PACKAGE_NAME, PackageManager.GET_ACTIVITIES);
                return true;
            } catch (PackageManager.NameNotFoundException ex) {
                return false;
            }
        }
    }



    public void startOrbotManually() {
        Intent intent = new Intent();
        intent.setClassName("org.torproject.android", "org.torproject.android.OrbotMainActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
            Log.d(TAG, "Launched Orbot successfully.");
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Orbot not found!", e);
            redirectToOrbotInstall();
        }
    }


    /**
     * Redirects the user to install Orbot from the Play Store or F-Droid.
     */
    public void redirectToOrbotInstall() {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ORBOT_PACKAGE_NAME)));
        } catch (Exception e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + ORBOT_PACKAGE_NAME)));
        }
    }

    /**
     * Creates an OkHttpClient configured to use Tor.
     *
     * @return OkHttpClient with a SOCKS5 proxy for Tor.
     */
    private OkHttpClient getTorHttpClient() {
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 9050));

        return new OkHttpClient.Builder()
                .proxy(proxy)
                .connectTimeout(60, TimeUnit.SECONDS) // Increase timeout
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Makes an HTTP GET request to a .onion URL using Tor.
     *
     * @param onionUrl The .onion address to connect to.
     * @return The response body as a string.
     */

    public String connectToOnion(@NonNull String onionUrl,@NonNull String jsonPayload) {
        OkHttpClient torClient = getTorHttpClient(); // Ensure this returns a SOCKS5-configured client

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        // Try sending an empty JSON payload if the server expects it
        //String jsonPayload = "{}"; // Empty JSON object
        RequestBody body = RequestBody.create(JSON, jsonPayload);

        Request request = new Request.Builder()
                .url(onionUrl)
                .post(body) // Sending the correct body
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Mozilla/5.0") // Mimic common user agent
                .addHeader("Connection", "close") // Force connection closure
                .build();

        Log.e(TAG, "Sending POST request to: " + onionUrl);

        try (Response response = torClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Log.e(TAG, "Response: " + responseBody);

                return responseBody;
            } else {
                Log.e(TAG, "Failed: " + response.code() + " - " + response.message());;
                if (response.body() != null) {
                    Log.e(TAG, "Response Body: " + response.body().string());
                }
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to .onion site", e);
            return null;
        }
    }

}
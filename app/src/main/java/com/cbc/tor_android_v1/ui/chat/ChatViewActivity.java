package com.cbc.tor_android_v1.ui.chat;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cbc.tor_android_v1.R;
import com.cbc.tor_android_v1.manager.EncryptionManager;
import com.cbc.tor_android_v1.manager.OrbotHelperClass;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class ChatViewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ImageButton sendButton;
    private ImageButton backIcon;
    private EditText chatInputText;
    private ArrayList<String> messageList;
    private ChatViewAdapter adapter;
    private OrbotHelperClass orbotHelper;

    private EncryptionManager encryptionManager;

    private static final String TAG = "ChatViewActivity";
    private static final String ONION_ADDRESS = "http://jpcs2dtpyovatwzl7bmukhltd3qr7254cufuc6vvqhrgqgr6zegakmqd.onion:5000/receive";

    // ✅ Already a valid Curve25519 (X25519) public key
    private static final String PUBLIC_KEY_HEX = "ac029cae1e9511d84fcd27b62abd89a31fc8962b5660e430ac359c510855e81b";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_view);

        initUIProps();
        initChatList();

        encryptionManager = new EncryptionManager(this);

        orbotHelper = new OrbotHelperClass(this);
        if (orbotHelper.isOrbotInstalled()) {
            Log.d(TAG, "Orbot is installed");
            orbotHelper.startOrbot();
        } else {
            Log.d(TAG, "Orbot is not installed");
            orbotHelper.startOrbotManually();
        }
    }

    private void initUIProps() {
        recyclerView = findViewById(R.id.chat_recycle_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        sendButton = findViewById(R.id.send_button);
        chatInputText = findViewById(R.id.chat_text_input);
        backIcon = findViewById(R.id.back_icon_image);
    }

    private void initChatList() {
        messageList = new ArrayList<>();
        messageList.add("Welcome to Just Social, most secure communication");
        adapter = new ChatViewAdapter(messageList, this);
        recyclerView.setAdapter(adapter);
        setClickListener();
    }

    private void setClickListener() {
        sendButton.setOnClickListener(v -> getInputMessage());
        backIcon.setOnClickListener(v -> finish());
    }

    private void getInputMessage() {
        String text = chatInputText.getText().toString();

        if (!text.isEmpty()) {
            adapter.addItem(text);
            chatInputText.getText().clear();
            recyclerView.smoothScrollToPosition(messageList.size());
            sendButton.setEnabled(false);

            adapter.addItem("Typing...");


            String x25519PublicKeyHex = PUBLIC_KEY_HEX;
            Log.d(TAG, "Using provided X25519 public key: " + x25519PublicKeyHex);

            // Encrypt the message
            String encryptedMessage = encryptionManager.encryptMessage(text, x25519PublicKeyHex);

            // Send the encrypted message
            sendRequest(encryptedMessage);

            new Handler().postDelayed(() -> {
                adapter.removeItemFromList();
                adapter.addItem("Message sent through Tor network!");
                recyclerView.smoothScrollToPosition(messageList.size());
                sendButton.setEnabled(true);
            }, 1500);
        }
    }

    private void sendRequest(String encryptedMessage) {
        Log.d(TAG, "Preparing to send message via Tor");

        OrbotHelperClass orbotHelperClass = new OrbotHelperClass(this);
        OrbotHelper.get(this).init();

        if (orbotHelperClass.isOrbotInstalled()) {
            orbotHelperClass.startOrbot();

            new Thread(() -> {
                JSONObject jsonPayload = new JSONObject();
                try {
                    // Get sender public key
                    String myPublicKeyHex = encryptionManager.getPublicKeyHex();

                    jsonPayload.put("sender_public_key", myPublicKeyHex);
                    jsonPayload.put("encrypted_message", encryptedMessage);
                    jsonPayload.put("sender_id", "user_" + System.currentTimeMillis());

                    Log.d(TAG, "Sending message to: " + ONION_ADDRESS);
                    Log.d(TAG, "JSON Payload: " + jsonPayload.toString());

                    String response = orbotHelperClass.connectToOnion(ONION_ADDRESS, jsonPayload.toString());

                    if (response != null) {
                        Log.d("OnionResponse", response);
                        runOnUiThread(() -> {
                            adapter.removeItemFromList();
                            adapter.addItem("Response: " + response);
                            recyclerView.smoothScrollToPosition(messageList.size());
                        });
                    } else {
                        Log.e(TAG, "No response from onion service");
                        runOnUiThread(() -> {
                            adapter.removeItemFromList();
                            adapter.addItem("No response from server");
                            recyclerView.smoothScrollToPosition(messageList.size());
                        });
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON error: " + e);
                    runOnUiThread(() -> {
                        adapter.removeItemFromList();
                        adapter.addItem("Error: " + e.getMessage());
                        recyclerView.smoothScrollToPosition(messageList.size());
                        sendButton.setEnabled(true);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "General error in sending thread: " + e);
                    runOnUiThread(() -> {
                        adapter.removeItemFromList();
                        adapter.addItem("Error: " + e.getMessage());
                        recyclerView.smoothScrollToPosition(messageList.size());
                        sendButton.setEnabled(true);
                    });
                }
            }).start();
        } else {
            Log.d(TAG, "Orbot not installed. Redirecting...");
            orbotHelperClass.redirectToOrbotInstall();
        }
    }
}

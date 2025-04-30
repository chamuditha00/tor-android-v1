package com.cbc.tor_android_v1.ui.chat;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.cbc.tor_android_v1.R;
import com.cbc.tor_android_v1.manager.EncryptionManager;
import com.cbc.tor_android_v1.manager.OrbotHelperClass;
import com.cbc.tor_android_v1.server.OnMessageReceivedListener;
import com.cbc.tor_android_v1.server.TorMessageServer;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.io.IOException;

public class ChatViewActivity extends AppCompatActivity implements OnMessageReceivedListener {

    private RecyclerView recyclerView;
    private ImageButton sendButton;
    private ImageButton backIcon;
    private EditText chatInputText;
    private ArrayList<ChatMessage> messageList;
    private ChatViewAdapter adapter;
    private OrbotHelperClass orbotHelper;
    private EncryptionManager encryptionManager;
    private TorMessageServer torMessageServer;
    private boolean isServerRunning = false;

    private static final String TAG = "ChatViewActivity";
    private static final String ONION_ADDRESS = "http://4vf5q7np5dzsm4xcpr72cgtjzklbtpjyaqwvwfgzqjl5nwcbbsbnopqd.onion:5000/receive";
    private static final String PUBLIC_KEY_HEX = "d969c998c92834a05ed479e94c5fb915ca8ef1563ce99f52d1fee34729ac4232";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat_view);

        initializeComponents();
        setupRecyclerView();
        setupClickListeners();
     //   startServer();
        orbotHelper.startOrbot();
        addMockWelcomeMessage();
    }

    private void addMockWelcomeMessage() {
        new Handler().postDelayed(() -> {
            adapter.addItem("Welcome to the chat!", true);
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        }, 1000);
    }

    private void initializeComponents() {
        recyclerView = findViewById(R.id.chat_recycle_view);
        sendButton = findViewById(R.id.send_button);
        backIcon = findViewById(R.id.back_icon_image);
        chatInputText = findViewById(R.id.chat_text_input);

        messageList = new ArrayList<>();
        orbotHelper = new OrbotHelperClass(this);
        encryptionManager = new EncryptionManager(this);
        encryptionManager.loadOrGenerateKeys();

        torMessageServer = new TorMessageServer(this, encryptionManager);
        torMessageServer.setOnMessageReceivedListener(this);
    }

    private void setupRecyclerView() {
        adapter = new ChatViewAdapter(messageList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> sendMessage());
        backIcon.setOnClickListener(v -> finish());
    }



    private void sendMessage() {
        String text = chatInputText.getText().toString().trim();
        if (text.isEmpty()) return;
        // Check if encryption keys are ready
        if (encryptionManager.getStoredPublicKeyHex() == null) {
            return;
        }

        // Add message to UI
        adapter.addItem(text, false);
        chatInputText.getText().clear();
        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);

        // Encrypt and send message
        new Thread(() -> {
            try {
                sendEncryptedOverTor(text);
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
                runOnUiThread(() -> {
                    adapter.removeLastItemIfExists();
                    adapter.addItem("Failed to send message", false);
                });
            }
        }).start();
    }

    private void sendEncryptedOverTor(String message) {
        try {
            String encryptedMessage = encryptionManager.encryptMessage(message, PUBLIC_KEY_HEX);
            String senderPublicKey = encryptionManager.getPublicKeyHex();

            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put("sender_public_key", senderPublicKey);
            jsonPayload.put("encrypted_message", encryptedMessage);

            String response = orbotHelper.connectToOnion(ONION_ADDRESS, jsonPayload.toString());
            Log.d(TAG, "Message sent response: " + response);

        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON payload", e);
        }
    }

    @Override
    public void onMessageReceived(String message) {
        runOnUiThread(() -> {
            adapter.addItem(message, true);
            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (torMessageServer != null && isServerRunning) {
            torMessageServer.stop();
            isServerRunning = false;
        }
    }
}
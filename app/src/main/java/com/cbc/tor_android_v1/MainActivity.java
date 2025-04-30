package com.cbc.tor_android_v1;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cbc.tor_android_v1.manager.EncryptionManager;
import com.cbc.tor_android_v1.server.TorMessageServer;
import com.cbc.tor_android_v1.ui.chat.ChatViewActivity;

import org.libsodium.jni.NaCl;
import org.torproject.android.binary.TorResourceInstaller;
import org.torproject.android.binary.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private TextView onionTextView, onionPublicKey;
    private Button generateButton, chatEnableButton;
    private ImageButton shareButton, copyButton;

    private File torDir;

     private String publicKey;
     private String privateKey;
     private EncryptionManager publicKeyManager;

     private TorMessageServer torMessageServer;
     private ChatViewActivity chatViewActivity;
    private static final String TAG = "TorDebug";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        torDir = new File(getFilesDir(), "tor");

        NaCl.sodium();
        initUiProps();
        initClickListeners();
        publicKeyManager = new EncryptionManager(this);
        torMessageServer = new TorMessageServer(this ,publicKeyManager);

         chatViewActivity = new ChatViewActivity();
    }

    private void initUiProps() {
        onionTextView = findViewById(R.id.onion_address);
        onionPublicKey = findViewById(R.id.onion_public_key);
        generateButton = findViewById(R.id.onion_button);
        shareButton = findViewById(R.id.share_button);
        copyButton = findViewById(R.id.copy_button);
        chatEnableButton = findViewById(R.id.chat_enable_button);
    }

    private void initClickListeners() {
        generateButton.setOnClickListener(v -> {


            publicKeyManager.loadOrGenerateKeys();
            publicKey = publicKeyManager.getStoredPublicKeyHex();
            privateKey = publicKeyManager.getPrivateKeyPref();




            Log.d(" generated key " , "Public Key: " + publicKey);
            Log.d(" generated key " , "Private key" + privateKey);
            onionTextView.setText("Starting Tor and generating .onion URL...");
       //   publicKeyManager.decryptMessage("d1ad535598aa7257fd51839e84efe12143094648e3d090ecdb71cd2f1d43099fc3ef5601bc1e78ba913fd00b7e9d388ac5efaac11bb33c421ba06fd970b5bd5bcbd12883e6648f136f","d969c998c92834a05ed479e94c5fb915ca8ef1563ce99f52d1fee34729ac4232");

            new Thread(() -> {
                try {
                    startTorAndGenerateOnion();
                } catch (Exception e) {
                    Log.d(TAG, "Tor start failed", e);
                    runOnUiThread(() -> onionTextView.setText("Error: " + e.getMessage()));
                }
            }).start();
        });

        chatEnableButton.setOnClickListener(v -> {
            String publicKey = publicKeyManager.getStoredPublicKeyHex();
          //  String privateKey = extractPrivateKey(new File(torDir, "hidden_service"));


            if (!publicKey.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, ChatViewActivity.class);
              //  intent.putExtra("publicKey", publicKey);
              //  intent.putExtra("privateKey", privateKey);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Keys are not available yet", Toast.LENGTH_SHORT).show();
            }
        });

        copyButton.setOnClickListener(v -> {


            String onion = onionTextView.getText().toString().trim();
            String publicKey = onionPublicKey.getText().toString().trim();

            if (!onion.isEmpty() && !publicKey.isEmpty()) {
                String combinedText = "Onion key: " + onion + "\n\nPublic key: " + publicKey;

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Tor Keys", combinedText);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Onion or Public Key is missing", Toast.LENGTH_SHORT).show();
            }
        });

        shareButton.setOnClickListener(v -> {
            String onion = onionTextView.getText().toString();
           // String publicKey = onionPublicKey.getText().toString().trim();

            if (!onion.isEmpty()) {
                String combinedText = "Onion Address: " + onion;

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "My Tor Keys: \n " + combinedText);
                sendIntent.setType("text/plain");
                sendIntent.setPackage("com.whatsapp");

                try {
                    startActivity(sendIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void startTorAndGenerateOnion() throws Exception {
        File hsDir = new File(torDir, "hidden_service");
        if (!hsDir.exists()) hsDir.mkdirs();

        File hostname = new File(hsDir, "hostname");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            runOnUiThread(() -> onionTextView.setText("Starting Tor via TorService..."));

            Intent intent = new Intent("org.torproject.android.intent.action.START");
            intent.setPackage("org.torproject.android"); // Orbot package
            startService(intent);

            int retries = 60;
            while (!hostname.exists() && retries-- > 0) {
                Thread.sleep(1000);
            }

        } else {

            TorResourceInstaller installer = new TorResourceInstaller(this, torDir);
            installer.installResources();

            String torrcText =
                    "DataDirectory " + torDir.getAbsolutePath() + "\n" +
                            "SOCKSPort auto\n" +
                            "HiddenServiceDir " + hsDir.getAbsolutePath() + "\n" +
                            "HiddenServicePort 80 127.0.0.1:8080\n";

            File torrc = new File(torDir, "torrc");
            Utils.saveTextFile(torrc.getAbsolutePath(), torrcText);

            File torBinary = installer.getTorFile();
            torBinary.setExecutable(true);

            Process torProcess = new ProcessBuilder(torBinary.getAbsolutePath(), "-f", torrc.getAbsolutePath())
                    .directory(torDir).redirectErrorStream(true).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(torProcess.getInputStream()));
            new Thread(() -> {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Log.d(TAG, "[Tor] " + line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading Tor logs", e);
                }
            }).start();

            int retries = 60;
            while (!hostname.exists() && retries-- > 0) {
                Thread.sleep(1000);
            }
        }

        if (!hostname.exists()) {
            runOnUiThread(() -> onionTextView.setText("Failed to retrieve .onion address."));
            return;
        }

        final String onionUrl = Utils.loadTextFile(hostname.getAbsolutePath()).trim();


        runOnUiThread(() -> {
            onionTextView.setText(onionUrl);
            onionPublicKey.setText(publicKey);
            Log.d(TAG, "Onion URL: " + onionUrl);
            Log.d(TAG, "Public Key: " + publicKey);
        });
          if (!onionUrl.isEmpty() && !publicKey.isEmpty()) {
              try {
                  torMessageServer.start();
                  Log.d("started server", "Tor message server started");
              } catch (IOException e) {
                  throw new RuntimeException(e);
              }
          }

//    private String extractPublicKey(File hsDir) {
//        File pubKeyFile = new File(hsDir, "hs_ed25519_public_key");
//        if (!pubKeyFile.exists()) return null;
//
//        try {
//            byte[] keyBytes;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                keyBytes = java.nio.file.Files.readAllBytes(pubKeyFile.toPath());
//            } else {
//                FileInputStream fis = new FileInputStream(pubKeyFile);
//                keyBytes = new byte[(int) pubKeyFile.length()];
//                fis.read(keyBytes);
//                fis.close();
//            }
//            return Base64.encodeToString(keyBytes, Base64.NO_WRAP);
//        } catch (IOException e) {
//            Log.e(TAG, "Error reading public key", e);
//            return null;
//        }
//    }

//    private String extractPrivateKey(File hsDir) {
//        File privKeyFile = new File(hsDir, "hs_ed25519_secret_key");
//        if (!privKeyFile.exists()) return null;
//
//        try {
//            byte[] keyBytes = new byte[(int) privKeyFile.length()];
//            FileInputStream fis = new FileInputStream(privKeyFile);
//            fis.read(keyBytes);
//            fis.close();
//            return Base64.encodeToString(keyBytes, Base64.NO_WRAP);
//        } catch (IOException e) {
//            Log.e(TAG, "Error reading private key", e);
//            return null;
//        }
//    }
}


}

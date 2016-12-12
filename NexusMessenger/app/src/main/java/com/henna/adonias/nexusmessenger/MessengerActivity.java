package com.henna.adonias.nexusmessenger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.Result;

import org.spongycastle.util.encoders.Base64;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static com.henna.adonias.nexusmessenger.messages.KEY_JWT;

public class MessengerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;
    public static final String MESSAGES_URL = "https://nexusmessenger.pw/Messages.php";
    public static final String KEY_TO = "to";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TOKEN = "token";

    private sendMessage mSendMessage = null;

    private KeyGenerator keyGen;
    private SecretKey encryptionKey, integrityKey;
    private Cipher cipher;
    private final int KEY_SIZE = 256;
    private final int HASH_SIZE = 32; //256 bits = 32 bytes
    private final int IV_LENGTH = 16; //128 bit IV
    private final String ALGORITHM_AES = "AES";
    private final String HMAC_ALGORITHM = "HmacSHA256";
    private final String ENCRYPTION_MODE = "CTR";
    private final String PADDING = "NoPadding";
    private Map<String, PublicKey> keyChain;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messenger);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mScannerView = new ZXingScannerView(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        FloatingActionButton QRCode = (FloatingActionButton) findViewById(R.id.qrCode);
        QRCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QrScanner(view);
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = getIntent();
                String jwt = intent.getStringExtra(KEY_JWT);
                EditText receiver = (EditText) findViewById(R.id.receiver);
                EditText message = (EditText) findViewById(R.id.message);
                String receiverString = receiver.getText().toString();
                String messageString = message.getText().toString();
                try {
                    //keyGen = KeyGenerator.getInstance(ALGORITHM_AES);
                    //cipher = Cipher.getInstance(ALGORITHM_AES + "/" + ENCRYPTION_MODE + "/" + PADDING);
                    //String encryptedMessage = encrypt(messageString, receiverString);
                    mSendMessage = new sendMessage(receiverString, messageString, jwt);
                    mSendMessage.execute((Void) null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }


        });
    }
    public void QrScanner(View view){
        setContentView(mScannerView);
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }
    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result result) {
        Log.e("handler", result.getText()); // Prints scan results
        Log.e("handler", result.getBarcodeFormat().toString()); // Prints the scan format (qrcode)
        // show the scanner result into dialog box
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scan Result");
        builder.setMessage(result.getText());
        AlertDialog alert1 = builder.create();
        alert1.show();

    }

    public class sendMessage extends AsyncTask<Void, Void, Boolean> {

        private String mReceiver;
        private String mMessage;
        private String mJwt;


        sendMessage(String receiver, String message, String jwt) {
            mReceiver = receiver;
            mMessage = message;
            mJwt = jwt;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            try {
                // Simulate network accesss
                StringRequest stringRequest = new StringRequest(Request.Method.POST, MESSAGES_URL,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                if (response.trim().contains("Message Sent")) {
                                    Toast.makeText(MessengerActivity.this, "Message was sent.", Toast.LENGTH_LONG).show();
                                    finish();

                                } else if (response.trim().contains("no message to send")) {
                                    Toast.makeText(MessengerActivity.this, "Fill out necessary information.", Toast.LENGTH_LONG).show();
                                }
                                 else {
                                        Toast.makeText(MessengerActivity.this, "Message was not sent.", Toast.LENGTH_LONG).show();
                                 }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(MessengerActivity.this, error.toString(), Toast.LENGTH_LONG).show();
                            }
                        }){
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String,String> map = new HashMap<String,String>();
                        map.put(KEY_TO, mReceiver);
                        map.put(KEY_MESSAGE,mMessage);
                        map.put(KEY_TOKEN, mJwt);
                        return map;
                    }
                };

                RequestQueue requestQueue = Volley.newRequestQueue(MessengerActivity.this);
                requestQueue.add(stringRequest);

                Thread.sleep(4000);
            } catch (InterruptedException e) {
                return false;
            }

            // TODO: register the new account here.
            return true;
        }
    }
    public String encrypt(String plainText, String friend)
            throws Exception{

        //Generate new keys and IV for every message
        encryptionKey = keyGen.generateKey();
        integrityKey = keyGen.generateKey();
        IvParameterSpec iv = generateIV();

        //encrypt the plainText
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, iv);

        /**
         * IV + ENCRYPTED + HMACtag + enc(KEY_E + KEY_I)
         */
        byte[]ivBytes = iv.getIV();
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        byte[] tag = performHMAC(encrypted, integrityKey);
        byte[] key_e = encryptionKey.getEncoded();
        byte[] key_i = integrityKey.getEncoded();

        //concatenate key_e and key_i into a single array to be encrypted with RSA
        int startPos = 0;
        byte[] bothKeys = new byte[key_e.length + key_i.length];
        System.arraycopy(key_e, 0, bothKeys, startPos, key_e.length);
        startPos += key_e.length;
        System.arraycopy(key_i, 0, bothKeys, startPos, key_i.length);

        //encrypt the keys into a single byte array
        Map<String, PublicKey> keyChain = getKeyChain();
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bothKeys);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey friendsKey = kf.generatePublic(publicKeySpec);
        byte[] encryptedKeys = RSAEncrypt(bothKeys, friendsKey);	//takes key_i, key_e, and friends publickey

        startPos = 0;
        byte[] cipherText = new byte[IV_LENGTH + encrypted.length + HASH_SIZE + encryptedKeys.length];


        //build out cipherText byte array to be sent
        // add IV
        System.arraycopy(ivBytes, 0, cipherText, startPos, ivBytes.length);
        startPos += ivBytes.length;

        // add encrypted message
        System.arraycopy(encrypted, 0, cipherText, startPos, encrypted.length);
        startPos += encrypted.length;

        // add HMAC tag
        System.arraycopy(tag, 0, cipherText, startPos, tag.length);
        startPos += tag.length;

        //add encrypted keys
        System.arraycopy(encryptedKeys, 0, cipherText, startPos, encryptedKeys.length);

        return Base64.toBase64String(cipherText);
    }
    private IvParameterSpec generateIV() {
        byte[] ivByte = new byte[IV_LENGTH];

        SecureRandom rand = new SecureRandom();
        rand.nextBytes(ivByte);

        return new IvParameterSpec(ivByte);
    }
    private byte[] performHMAC(byte[] cipherText, Key key_i) throws NoSuchAlgorithmException, InvalidKeyException {

        //create new mac and hash the cipherText
        Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
        hmac.init(key_i);

        return hmac.doFinal(cipherText);
    }
    public Map<String,PublicKey> getKeyChain() {
        File keyChainFile = new File(MessengerActivity.this.getFilesDir().getPath() + "/KeyChain.txt");
        if(keyChainFile.exists()) {
            try {
                if (keyChainFile.exists()) {
                    ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(keyChainFile));
                    Object obj = inputStream.readObject();
                    if (obj instanceof HashMap<?, ?>) {
                        return (HashMap<String, PublicKey>) obj;
                    } else {
                        return null;
                    }
                }
                else {
                    return null;
                }
            } catch (Exception e) {
                Log.d("KeyChain Exception: ", e.getMessage());
            }
        }
        //if it doesn't exist yet, create a new one
        else {
            keyChain = new HashMap<>();
        }
        return keyChain;
    }
    public byte[] RSAEncrypt(byte[] plainText, PublicKey publicKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        // Get friend's public key from key chain
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(plainText);
    }
}

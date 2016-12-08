package com.henna.adonias.nexusmessenger;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.spongycastle.util.encoders.Base64;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import static com.henna.adonias.nexusmessenger.LoginActivity.KEY_USERNAME;

public class messages extends AppCompatActivity {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private getMessages mConversation = null;
    private ListView lv;

    public final String MESSAGES_URL = "https://nexusmessenger.pw/Messages.php";
    private TextView textView;
    private String username;
    private String jwt;
    private String id;
    public static final String KEY_JWT = "jwt";

    private final String ALGORITHM = "AES";
    private final String HMAC_ALGORITHM = "HmacSHA256";
    private final String ENCRYPTION_MODE = "CTR";
    private final String PADDING = "NoPadding";
    private final int KEY_SIZE = 256;
    private final int HASH_SIZE = 32; //256 bits = 32 bytes
    private final int IV_LENGTH = 16; //128 bit IV
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private KeyPairGenerator keyPairGen;
    private KeyPair keyPair;

    private KeyGenerator keyGen;
    private SecretKey encryptionKey, integrityKey;
    private Cipher cipher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        textView = (TextView) findViewById(R.id.textViewUsername);
        textView.setText("Welcome");
        lv = (ListView) findViewById(R.id.messages);
        Intent intent = getIntent();
        String usernameAndJWT = intent.getStringExtra(KEY_USERNAME);
        username = usernameAndJWT.split(":")[0];
        jwt = usernameAndJWT.split(":")[1];
        try {
            cipher = Cipher.getInstance(ALGORITHM + "/" + ENCRYPTION_MODE + "/" + PADDING);

            keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE);
            //rsaCipher = new RSACipher(context);
        }
        catch (NoSuchAlgorithmException e) {
            Log.w("No Such Algorithm: ", e.getMessage());
        }
        catch (NoSuchPaddingException e) {
            Log.w("No Such Padding: ", e.getMessage());
        }
        FloatingActionButton qrCode = (FloatingActionButton) findViewById(R.id.qrCodeGenerator);
        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(messages.this, publicKeyQRActivity.class);
                startActivity(intent);
            }
        });
        Button compose = (Button) findViewById(R.id.newMessage);
        compose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(messages.this, MessengerActivity.class);
                intent.putExtra(KEY_JWT, jwt);
                startActivity(intent);
            }
        });
        mConversation = new getMessages(username, jwt);
        mConversation.execute((Void) null);


    }


    public class getMessages extends AsyncTask<Void, Void, Boolean> {

        private final String mJWT;
        private final String mUsername;
        private ArrayList<String> conversations;

        getMessages(String username, String jwt) {
            mUsername = username;
            mJWT = jwt;
            conversations = new ArrayList<>();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Simulate network accesss
                StringBuffer sb = new StringBuffer();
                InputStream is = null;

                try {
                    URL url = new URL(MESSAGES_URL);
                    HttpsURLConnection conn = null;
                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setDoOutput(true);
                    conn.addRequestProperty("Authorization", "Bearer " + mJWT);
                    is = new BufferedInputStream(conn.getInputStream());
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String inputLine;
                    inputLine = br.readLine();
                    sb.append(inputLine);
                    JSONArray JSONconvos = new JSONArray(inputLine);
                    id = JSONconvos.getJSONObject(0).getString("PersonalID");

                    for(int i = 1; i < JSONconvos.length(); i++) {
                        String messageEncrypted = JSONconvos.getJSONObject(i).getString("Message");
                        //String messageDecrypted = decrypt(messageEncrypted);
                        String from = JSONconvos.getJSONObject(i).getString("From");
                        String timestamp = JSONconvos.getJSONObject(i).getString("timestamp");
                        String conv = "From: " + from + "\nMessage: " + messageEncrypted + "\n" +timestamp;
                        conversations.add(conv);
                    }
                    publishProgress();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                            Log.i("NexusMessenger", "Error closing InputStream");
                        }
                    }
                }

                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            // TODO: register the new account here.
            return true;
        }

        public String decrypt(String cipherText)
                throws Exception{

            /**
             * HAVE TO GET EACH THING OUT OF CIPHERTEXT
             * IV + cipherText + HMAC + Key_e + Key_i
             */
            byte[] decodedBytes = Base64.decode(cipherText); //the WHOLE package
            byte[] iv = new byte[IV_LENGTH];
//		byte[] encryptedMessage = new byte[decodedBytes.length - IV_LENGTH - HASH_SIZE - (2*KEY_SIZE/8)];
            byte[] encryptedMessage = new byte[decodedBytes.length - IV_LENGTH - HASH_SIZE - KEY_SIZE];
            byte[] hash = new byte[HASH_SIZE];

//		byte[] encryptionKeyBytes = new byte[KEY_SIZE/8];
//		byte[] integrityKeyBytes = new byte[KEY_SIZE/8];
            byte[] encryptedKeys = new byte[KEY_SIZE];

            int startingPos = 0;

            //get IV out of decoded bytes
            System.arraycopy(decodedBytes, startingPos, iv, 0, IV_LENGTH);
            startingPos += IV_LENGTH;

            //get encrypted message out of decoded bytes
            System.arraycopy(decodedBytes, startingPos, encryptedMessage, 0, encryptedMessage.length);
            startingPos += encryptedMessage.length;

            //get HMAC tag out of decoded bytes
            System.arraycopy(decodedBytes, startingPos, hash, 0, hash.length);
            startingPos += hash.length;

            //get key_e out of decoded bytes
//		System.arraycopy(decodedBytes, startingPos, encryptionKeyBytes, 0, encryptionKeyBytes.length);
//		startingPos += encryptionKeyBytes.length;
//
//		//get key_i out of decoded bytes
//		System.arraycopy(decodedBytes, startingPos, integrityKeyBytes, 0, integrityKeyBytes.length);

            //get encrypted keys out of decoded bytes
            System.arraycopy(decodedBytes, startingPos, encryptedKeys, 0, encryptedKeys.length);

            byte[] decryptedKeys = rsaDecrypt(encryptedKeys);
            byte[] encryptionKeyBytes = new byte[KEY_SIZE/8];
            byte[] integrityKeyBytes = new byte[KEY_SIZE/8];
            //get key_e out of decryptedkeys
            System.arraycopy(decryptedKeys, 0, encryptionKeyBytes, 0, encryptionKeyBytes.length);
            System.arraycopy(decryptedKeys, encryptionKeyBytes.length, integrityKeyBytes, 0, integrityKeyBytes.length);


            //create keys out of byte arrays that were extracted
            Key key_e = new SecretKeySpec(encryptionKeyBytes, ALGORITHM);
            Key key_i = new SecretKeySpec(integrityKeyBytes, ALGORITHM);

            //Checks if the tag matches
            if (Arrays.equals(hash, performHMAC(encryptedMessage, key_i))){
                //Decrypt the cipher text
                cipher.init(Cipher.DECRYPT_MODE, key_e, new IvParameterSpec(iv));
                byte[] plainText = cipher.doFinal(encryptedMessage);

                return new String(plainText);
            }
            else {
                System.out.println("HMAC DID NOT MATCH!");
                System.out.println("Original HMAC: " + Base64.toBase64String(hash));
                System.out.println("Calculated HMAC: " + Base64.toBase64String(performHMAC(encryptedMessage, key_i)));
                return null;
            }

        }
        private byte[] performHMAC(byte[] cipherText, Key key_i) throws NoSuchAlgorithmException, InvalidKeyException {

            //create new mac and hash the cipherText
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(key_i);

            return hmac.doFinal(cipherText);
        }
        public byte[] rsaDecrypt(byte[] cipherText) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(cipherText);
        }



        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            textView.setText("Welcome " + username + " ID: " + id);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(messages.this, android.R.layout.simple_list_item_1, conversations);
            lv.setAdapter(arrayAdapter);
        }
    }

    public void sendMessage(View view){
        Intent intent = new Intent(this, MessengerActivity.class);
        intent.putExtra(KEY_JWT, jwt);
        startActivity(intent);
    }
}

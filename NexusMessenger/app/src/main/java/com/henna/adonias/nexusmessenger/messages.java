package com.henna.adonias.nexusmessenger;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import static com.henna.adonias.nexusmessenger.LoginActivity.KEY_USERNAME;

public class messages extends AppCompatActivity {

    private getMessages mConversatiion = null;
    private ListView lv;

    public static final String MESSAGES_URL = "https://nexusmessenger.pw/Messages.php";
    private TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        textView = (TextView) findViewById(R.id.textViewUsername);
        lv = (ListView) findViewById(R.id.messages);


        Intent intent = getIntent();
        String usernameAndJWT = intent.getStringExtra(KEY_USERNAME);
        String username = usernameAndJWT.split(":")[0];
        String jwt = usernameAndJWT.split(":")[1];
        textView.setText("Welcome " + username);
        mConversatiion = new getMessages(username, jwt);
        mConversatiion.execute((Void) null);


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
                String result = null;
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
                    String inputLine = "";
                    while ((inputLine = br.readLine()) != null) {
                        sb.append(inputLine);
                        JSONObject JSONconvos = new JSONObject(inputLine);
                        conversations.add(inputLine);
                    }
                    publishProgress();
                    result = sb.toString();
                    Toast.makeText(messages.this, result, Toast.LENGTH_SHORT).show();
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


        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(messages.this, android.R.layout.simple_list_item_1, conversations);
            lv.setAdapter(arrayAdapter);
        }
    }
}

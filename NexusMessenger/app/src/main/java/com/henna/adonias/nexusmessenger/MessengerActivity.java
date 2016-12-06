package com.henna.adonias.nexusmessenger;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

import java.util.HashMap;
import java.util.Map;

import static com.henna.adonias.nexusmessenger.messages.KEY_JWT;

public class MessengerActivity extends AppCompatActivity {

    public static final String MESSAGES_URL = "https://nexusmessenger.pw/Messages.php";
    public static final String KEY_TO = "to";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TOKEN = "token";

    private sendMessage mSendMessage = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messenger);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = getIntent();
                String jwt = intent.getStringExtra(KEY_JWT);
                //Snackbar.make(view, jwt, Snackbar.LENGTH_LONG).setAction("Action", null).show();
                EditText receiver = (EditText) findViewById(R.id.receiver);
                EditText message = (EditText) findViewById(R.id.message);
                String receiverString = receiver.getText().toString();
                String messageString = message.getText().toString();
                mSendMessage = new sendMessage(receiverString, messageString, jwt);
                mSendMessage.execute((Void) null);
            }


        });
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

                                } else {
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

                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            // TODO: register the new account here.
            return true;
        }
    }
}

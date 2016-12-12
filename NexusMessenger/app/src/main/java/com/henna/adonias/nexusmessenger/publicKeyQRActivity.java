package com.henna.adonias.nexusmessenger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;

import static com.henna.adonias.nexusmessenger.messages.USER_ID;

public class publicKeyQRActivity extends AppCompatActivity {
    ImageView qrCodeImageview;
    String QRcode;
    public final static int WIDTH=500;

    {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_key_qr);
        Intent intent = getIntent();
        final String id = intent.getStringExtra(USER_ID);
        getID();
        Thread t = new Thread(new Runnable() {
            public void run() {
                // this is the msg which will be encode in QRcode
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(publicKeyQRActivity.this);

                if(!prefs.contains(id + "_public")){
                    try {
                        ECGenParameterSpec ecParamSpec = new ECGenParameterSpec("secp224k1");
                        KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDH", "SC");
                        kpg.initialize(ecParamSpec);
                        KeyPair kpA = kpg.generateKeyPair();

                        String pubStr = org.spongycastle.util.encoders.Base64.toBase64String(kpA.getPublic().getEncoded());
                        String privStr = org.spongycastle.util.encoders.Base64.toBase64String(kpA.getPrivate().getEncoded());

                        SharedPreferences.Editor prefsEditor = PreferenceManager
                                .getDefaultSharedPreferences(publicKeyQRActivity.this).edit();

                        prefsEditor.putString(id + "_public", pubStr);
                        prefsEditor.putString(id + "_private", privStr);
                        prefsEditor.commit();



                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                QRcode = prefs.getString(id + "_public", null);
                try {
                    synchronized (this) {
                        wait(5000);
                        // runOnUiThread method used to do UI task in main thread.
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Bitmap bitmap = null;
                                    bitmap = encodeAsBitmap(QRcode);
                                    qrCodeImageview.setImageBitmap(bitmap);
                                } catch (WriterException e) {
                                    e.printStackTrace();
                                } // end of catch block

                            } // end of run method
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }
    private void getID() {
        qrCodeImageview=(ImageView) findViewById(R.id.img_qr_code_image);
    }
    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? getResources().getColor(R.color.black):getResources().getColor(R.color.white);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, 500, 0, 0, w, h);
        return bitmap;
    } /// end of this method
}

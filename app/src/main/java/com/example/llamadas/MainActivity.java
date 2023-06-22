package com.example.llamadas;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {
    String mensajeTexto ="";
    TextView tvtelefono;
    String phoneNumberToCall="";

    private static String TAG = "myCallReceiver";
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvtelefono=(TextView) findViewById(R.id.tvtelefono);

        //PhoneCallListener phoneListener = new PhoneCallListener();
        CallStateListener phoneListener = new CallStateListener();
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public class CallStateListener extends PhoneStateListener {
        private static final String TAG = "CallStateListener";
        //public CallReceiver() { }

        protected void onIncomingCallStarted(int ctx, String number, Date start) {
            Log.d("onIncomingCallStarted()",number);
        }

        protected void onOutgoingCallStarted(int ctx, String number, Date start) {
            Log.d(TAG, "onOutgoingCallStarted() number: " + number);
        }

        protected void onIncomingCallEnded(int ctx, String number, Date start, Date end) {
            Log.i(TAG, "onIncomingCallEnded() : (savedNumber: "+ number + ", callStartTime: " +start.toString());
            Intent dial = new Intent(Intent.ACTION_CALL);
            dial.setData(Uri.parse("tel:"+number));
            startActivity(dial);
        }

        protected void onOutgoingCallEnded(int ctx, String savedNumber, Date callStartTime, Date end) {
            Log.i(TAG, "onOutgoingCallEnded() : (savedNumber: "+ savedNumber + ", callStartTime: " +callStartTime.toString());
        }

        protected void onMissedCall(int ctx, String incomingNumber, Date start) {
            Log.i(TAG, "onMissedCall() : (savedNumber: "+ incomingNumber + ", callStartTime: " +start.toString());
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_SHORT;
            tvtelefono.setText(incomingNumber);
            Toast toast = Toast.makeText(context, "Iniciando Aplicación", duration);
            toast.show();
            if(lastState == state){
                return;
            }
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    toast = Toast.makeText(context, "Número guardado"+TelephonyManager.CALL_STATE_IDLE, duration);
                    toast.show();
                    Intent dial = new Intent(Intent.ACTION_CALL);
                    dial.setData(Uri.parse("tel:"+incomingNumber));
                    startActivity(dial);
                    // Llamada finalizada (llamada perdida)
                    if(lastState == TelephonyManager.CALL_STATE_RINGING){
                        onMissedCall(state, savedNumber, callStartTime);
                    }
                    else if(isIncoming){


                        onIncomingCallEnded(state, savedNumber, callStartTime, new Date());
                    }
                    else{
                        onOutgoingCallEnded(state, savedNumber, callStartTime, new Date());
                    }
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    toast = Toast.makeText(context, "isIncoming"+isIncoming, duration);
                    toast.show();
                    mensaje(incomingNumber);
                    Log.d(TAG, "Llamada Entrante");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // Llamada en curso
                    Log.d(TAG, "Llamada en Curso");
                    break;
            }
            lastState = state;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CallStateListener phoneListener = new CallStateListener();
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
    }

    public void mensaje(String numero) {
        String URL="http://maps.google.com/maps?&z=15&mrt=loc&t=m&q=";
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            mensajeTexto  = "NO TIENES PERMISOS";
        }
        else {
            LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location loc = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            double longitudeGPS = loc.getLongitude();
            double latitudeGPS = loc.getLatitude();
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> list = geocoder.getFromLocation(latitudeGPS, longitudeGPS, 1);
                if (!list.isEmpty()) {
                    Address DirCalle = list.get(0);
                    String direccion = DirCalle.getAddressLine(0);
                    mensajeTexto = "Mi ubicación actual es "+URL+ latitudeGPS + "+" + longitudeGPS;
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(numero, null,mensajeTexto,null,null);
    }


}

//mandar el mensaje y marcar y suspender la pantalla cuando marcas
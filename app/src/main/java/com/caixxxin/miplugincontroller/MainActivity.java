package com.caixxxin.miplugincontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.Context;
import android.content.ComponentName;
import com.caixxxin.miplugincontroller.MiPluginService.MiPluginBinder;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    String TAG = "MiPluginControllerActivity";
    boolean bBounded = false;
    Button submitButton;
    EditText ipText;
    EditText tokenText;
    MiPluginService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        submitButton = (Button) findViewById(R.id.submitButton);
        submitButton.setOnClickListener(this);

        ipText = (EditText) findViewById(R.id.ipInput);
        tokenText = (EditText) findViewById(R.id.tokenInput);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, MiPluginService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        bBounded = false;
        unbindService(connection);
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MiPluginBinder binder = (MiPluginBinder) service;
            mService = binder.getService();
            Log.i(TAG, "onServiceConnected className=" + className);
            bBounded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bBounded = false;
        }
    };

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.submitButton:
                String ip = ipText.getText().toString();
                String token = tokenText.getText().toString();
                Log.i(TAG, "submitbutton ip=" + ip + " token=" + token);
                if (bBounded) {
                    mService.setIpAndToken(ip, token);
                }

                break;
            default:
                break;
        }
    }
}
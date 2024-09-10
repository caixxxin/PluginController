package com.caixxxin.miplugincontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.content.Context;
import android.content.ComponentName;
import java.util.LinkedHashMap;
import java.util.Map;
import com.caixxxin.miplugincontroller.MiPluginService.MiPluginBinder;


public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
    String TAG = "MiPluginControllerActivity";
    boolean bBounded = false;
    Button submitButton;
    private Spinner deviceSelectSpinner;
    private ArrayAdapter<String> spinnerAdapter;
    TextView tokenText;
    TextView modelText;
    EditText ipText;
    CheckBox testCheckBox;
    Button testAcOnButton;
    Button testAcOffButton;
    Button testUsbOnButton;
    Button testUsbOffButton;
    MiPluginService mService;
    String mToken;
    LinkedHashMap<String, String> deviceMap = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceMap.put("", "");
        deviceMap.put("d4130e242964c56c3ff610140c6531ec", "chuangmi.plug.v1");
        deviceMap.put("03c8bd98935407bba7b79cfc474b99fb", "chuangmi.plug.m1");

        tokenText = (TextView) findViewById(R.id.tokenTip);
        modelText = (TextView) findViewById(R.id.ModelTip);
        ipText = (EditText) findViewById(R.id.ipInput);
        testCheckBox = (CheckBox) findViewById(R.id.testModeSelect);
        testCheckBox.setOnCheckedChangeListener(this);

        testAcOnButton = (Button) findViewById(R.id.testAcOn);
        testAcOffButton = (Button) findViewById(R.id.testAcOff);
        testUsbOnButton = (Button) findViewById(R.id.testUsbOn);
        testUsbOffButton = (Button) findViewById(R.id.testUsbOff);
        testAcOnButton.setVisibility(View.GONE);
        testAcOffButton.setVisibility(View.GONE);
        testUsbOnButton.setVisibility(View.GONE);
        testUsbOffButton.setVisibility(View.GONE);
        testAcOnButton.setOnClickListener(this);
        testAcOffButton.setOnClickListener(this);
        testUsbOnButton.setOnClickListener(this);
        testUsbOffButton.setOnClickListener(this);

        deviceSelectSpinner = findViewById(R.id.deviceSelectSpinner);
        // 创建适配器并设置数据源
        String[] data = {"不选择", "插座1", "插座2"};
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
        // 设置下拉框样式
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 设置适配器
        deviceSelectSpinner.setAdapter(spinnerAdapter);
        deviceSelectSpinner.setSelection(0);
        deviceSelectSpinner.setOnItemSelectedListener(this);

        submitButton = (Button) findViewById(R.id.submitButton);
        submitButton.setOnClickListener(this);

        Intent serviceIntent = new Intent(this, MiPluginService.class);
        this.startService(serviceIntent);
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

            // 更新当前画面的选择设备。
            deviceSelectSpinner.setSelection(getTokenIndex(mService.getToken()));
            ipText.setText(mService.getIp());
            testCheckBox.setChecked(mService.getTestMode());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bBounded = false;
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.i(TAG, "onItemSelected position=" + position);
        tokenText.setText("TOKEN:" + getToken(position));
        modelText.setText("MODEL:" + getModel(position));
        if (position == 0) {
            ipText.setText("");
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // 当没有选项被选中时执行的代码
        Log.i(TAG, "onNothingSelected");
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.submitButton:
                String ip = ipText.getText().toString();
                String token = getToken(deviceSelectSpinner.getSelectedItemPosition());
                String model = getModel(deviceSelectSpinner.getSelectedItemPosition());
                Log.i(TAG, "submitbutton ip=" + ip + " token=" + token + " model=" + model);
                if (bBounded) {
                    mService.setIpAndToken(ip, token, model);
                }

                break;
            case R.id.testAcOn:
                mService.testAcOn();
                break;
            case R.id.testAcOff:
                mService.testAcOff();
                break;
            case R.id.testUsbOn:
                mService.testUsbOn();
                break;
            case R.id.testUsbOff:
                mService.testUsbOff();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mService.setTestMode(isChecked);
        if (isChecked) {
            ipText.setText(mService.getIp());
            deviceSelectSpinner.setSelection(getTokenIndex(mService.getToken()));

            testAcOnButton.setVisibility(View.VISIBLE);
            testAcOffButton.setVisibility(View.VISIBLE);
            testUsbOnButton.setVisibility(View.VISIBLE);
            testUsbOffButton.setVisibility(View.VISIBLE);
        } else {
            testAcOnButton.setVisibility(View.GONE);
            testAcOffButton.setVisibility(View.GONE);
            testUsbOnButton.setVisibility(View.GONE);
            testUsbOffButton.setVisibility(View.GONE);
        }
    }

    private int getTokenIndex(String token) {
        int index = 0;
        for (Map.Entry<String, String> entry : deviceMap.entrySet()) {
            if (entry.getKey().equals(token)) {
                Log.i(TAG, "getTokenIndex idx=" + index);
                return index;
            }
            index++;
        }
        return 0;
    }

    private String getToken(int index) {
        String strRet = "";
        int count = 0;
        for (Map.Entry<String, String> entry : deviceMap.entrySet()) {
            if (index == count) {
                strRet = strRet + entry.getKey();
                break;
            }
            count++;
        }
        return strRet;
    }

    private String getModel(int index) {
        String strRet = "";
        int count = 0;
        for (Map.Entry<String, String> entry : deviceMap.entrySet()) {
            if (index == count) {
                strRet = strRet + entry.getValue();
                break;
            }
            count++;
        }
        return strRet;
    }
}
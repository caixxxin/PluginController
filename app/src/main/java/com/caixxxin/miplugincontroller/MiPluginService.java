package com.caixxxin.miplugincontroller;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.HandlerThread;
import android.os.Process;
import android.os.BatteryManager;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

//{"id": 1, "method": "set_power", "params": ["off"]}
//{"id": 1, "method": "set_power", "params": ["on"]}
//{"id": 1, "method": "set_usb_off", "params": []}

public class MiPluginService extends Service {
    private final IBinder binder = new MiPluginBinder();
    private static final String TAG = "MiPluginService";
    private static final String PLUG_MODEL_V1 = "chuangmi.plug.v1";
    String mIp = "";
    String mToken = "";
    String mModel = "";
    boolean mTestMode = false;
    private Object deviceInfoLock = new Object();

    int mBattLevel = 0;
    int mBattScale = 0;
    int mBattTemp = 0;
    int mBattStatus = 0;

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;
    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                //int batteryVol = mBattery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                Log.i(TAG, "battery=" + String.valueOf(mBattLevel) + " scale=" + String.valueOf(mBattScale) + " temp=" + String.valueOf(mBattTemp) + " status=" + batteryStatusToString(mBattStatus));
                
                if (!mTestMode) {
                    if (mBattLevel > 90 && (mBattStatus == BatteryManager.BATTERY_STATUS_CHARGING || mBattStatus == BatteryManager.BATTERY_STATUS_FULL)) {
                        sendAcOff();
                    } else if (mBattLevel < 40 && (mBattStatus == BatteryManager.BATTERY_STATUS_DISCHARGING || mBattStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING ||mBattStatus == BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
                        sendAcOn();
                    }
                }
                
                serviceHandler.sendEmptyMessageDelayed(0, 60000);
            } else if (msg.what == 1) {
                sendAcOn();
            } else if (msg.what == 2) {
                sendAcOff();
            } else if (msg.what == 3) {
                sendUsbOn();
            } else if (msg.what == 4) {
                sendUsbOff();
            }
        }
    }

    public class MiPluginBinder extends Binder {
        MiPluginService getService() {
            return MiPluginService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        // 初始化
        Security.addProvider(new BouncyCastleProvider());

        SharedPreferences sharedPreferences = getSharedPreferences("pluginData", Context .MODE_PRIVATE);
        synchronized(deviceInfoLock) {
            mIp = sharedPreferences.getString("ip", "");
            mToken = sharedPreferences.getString("token", "");
            mModel = sharedPreferences.getString("model", "");
            Log.i(TAG, "last ip=" + mIp + " token=" + mToken + " mModel=" + mModel);
        }

        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        serviceHandler.sendEmptyMessageDelayed(0, 5000);
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        unregisterReceiver(batteryReceiver);
    }

    public void setIpAndToken(String ip, String token, String model) {
        Log.i(TAG, "set ip=" + ip + " token=" + token);
        //步骤1：创建一个SharedPreferences对象
        SharedPreferences sharedPreferences= getSharedPreferences("pluginData", Context.MODE_PRIVATE);
        //步骤2： 实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = sharedPreferences.edit();

        synchronized(deviceInfoLock) {
            mIp = ip;
            mToken = token;
            mModel = model;

            //步骤3：将获取过来的值放入文件
            editor.putString("ip", mIp);
            editor.putString("token", mToken);
            editor.putString("model", mModel);
        }

        //步骤4：提交               
        editor.apply();
    }

    public String getToken() {
        String strRet = "";
        synchronized(deviceInfoLock) {
            strRet = mToken;
            Log.i(TAG, "get token=" + strRet);
        }
        return strRet;
    }

    public String getIp() {
        String strRet = "";
        synchronized(deviceInfoLock) {
            strRet = mIp;
            Log.i(TAG, "get ip=" + strRet);
        }
        return strRet;
    }

    public void setTestMode(boolean test) {
        mTestMode = test;
    }

    public boolean getTestMode() {
        Log.i(TAG, "getTestMode=" + mTestMode);
        return mTestMode;
    }

    public void testAcOn() {
        serviceHandler.sendEmptyMessageDelayed(1, 0);
    }

    public void testAcOff() {
        serviceHandler.sendEmptyMessageDelayed(2, 0);
    }
    
    public void testUsbOn() {
        serviceHandler.sendEmptyMessageDelayed(3, 0);
    }

    public void testUsbOff() {
        serviceHandler.sendEmptyMessageDelayed(4, 0);
    }

    private BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra("level", 0);
            int scale = intent.getIntExtra("scale", 100);
            int temperature = intent.getIntExtra("temperature", 0) / 10;
            int status = intent.getIntExtra("status", 0);
            updateBattery(level, scale, temperature, status);
        }
    };

    public void updateBattery(int level, int scale, int temp, int status) {
        mBattLevel = level;
        mBattScale = scale;
        mBattTemp = temp;
        mBattStatus = status;
    }

    public String batteryStatusToString(int status) {
        String statusStr = "unknown";
        switch (mBattStatus){
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                statusStr="unknown";
                break;
            case BatteryManager.BATTERY_STATUS_CHARGING:
                statusStr="charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                statusStr="discharging";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                statusStr="not charging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                statusStr="full";
                break;
        }
        return statusStr;
    }

    public class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //
            Log.i(TAG, "boot.");
        }
    }

    public static String commonBytesToHexStr(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        int byteCount = 0;
        for (byte b : bytes) {
            if (byteCount % 16 == 0) {
                hexString.append("\n");
            }
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                // 如果是一位的话，要补0
                hexString.append('0');
            }
            hexString.append(hex + " ");
            byteCount++;
        }
        return hexString.toString();
    }

    private static String genAcOnJson(String model) {
        if (model.equals(PLUG_MODEL_V1)) {
            return "{\"id\": 1, \"method\": \"set_on\", \"params\": []}";
        }
        return "{\"id\": 1, \"method\": \"set_power\", \"params\": [\"on\"]}";
    }

    private static String genAcOffJson(String model) {
        if (model.equals(PLUG_MODEL_V1)) {
            return "{\"id\": 1, \"method\": \"set_off\", \"params\": []}";
        }
        return "{\"id\": 1, \"method\": \"set_power\", \"params\": [\"off\"]}";
    }

    private static String genUsbOnJson(String model) {
        return "{\"id\": 1, \"method\": \"set_usb_on\", \"params\": []}";
    }

    private static String genUsbOffJson(String model) {
        return "{\"id\": 1, \"method\": \"set_usb_off\", \"params\": []}";
    }

    private static byte[] concatByteArray(byte[] byte1, byte[] byte2) {
        byte[] byteRet = new byte[byte1.length + byte2.length];
        System.arraycopy(byte1, 0, byteRet, 0, byte1.length);
        System.arraycopy(byte2 ,0, byteRet, byte1.length, byte2.length);
        return byteRet;
    }

    private static byte[] hexStringToByteArray(String hexString) {
        byte[] byteArray = new byte[hexString.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            int index = i * 2;
            int high = Integer.parseInt(hexString.substring(index, index + 1), 16);
            int low = Integer.parseInt(hexString.substring(index + 1, index + 2), 16);
            byteArray[i] = (byte) ((high << 4) + low);
        }
        return byteArray;
    }

    private static byte[] genByteArrayFromInt32(int intValue) {
        byte[] result = new byte[4];
        result[0] = (byte) ((intValue >> 24) & 0xFF);
        result[1] = (byte) ((intValue >> 16) & 0xFF);
        result[2] = (byte) ((intValue >> 8) & 0xFF);
        result[3] = (byte) (intValue & 0xFF);
        return result;
    }

    private static int getInt32FromByteArray(byte[] bytes, int startIndex) {
        if (bytes == null || startIndex < 0 || startIndex + 3 >= bytes.length) {
            Log.i(TAG, "getInt32FromByteArray error");
            return 0;
        }
 
        // 这里使用了位操作符来组成int值
        // 假设是大端顺序（高位字节在前）
        int value = ((bytes[startIndex] & 0xFF) << 24) |
                    ((bytes[startIndex + 1] & 0xFF) << 16) |
                    ((bytes[startIndex + 2] & 0xFF) << 8) |
                    (bytes[startIndex + 3] & 0xFF);
 
        return value;
    }

    private static byte[] md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find MD5 algorithm", e);
        }
    }

    private static byte[] parseDeviceIdFromReply(byte[] reply) {
        byte[] byteRet = new byte[0];
        if (reply.length >= 12) {
            byteRet = new byte[4];
            System.arraycopy(reply, 8, byteRet, 0, 4);
        }
        return byteRet;
    }

    private static byte[] encryptJson(String json, String token) {
        Log.i(TAG, "encryptJson json=" + json + " token=" + token);
        byte[] key = md5(hexStringToByteArray(token));
        byte[] iv = md5(concatByteArray(key, hexStringToByteArray(token)));
        byte[] end = new byte[1];
        end[0] = 0;
        byte[] byteRet = new byte[0];
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC"); // 创建AES加密器
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
            byteRet = cipher.doFinal(concatByteArray(json.getBytes(), end));
        } catch (Exception e) {
            Log.i(TAG, "encryptJson error=" + e);
        }
        return byteRet;
    }

    private static String decryptReply(byte[] reply, String token) {
        byte[] key = md5(hexStringToByteArray(token));
        byte[] iv = md5(concatByteArray(key, hexStringToByteArray(token)));
        int length = 0;
        if (reply.length >= 4) {
            length = getInt16FromByteArray(reply, 2);
        }
        if (length > 32) {
            byte[] byteEncrypted = new byte[length - 32];
            System.arraycopy(reply, 32, byteEncrypted, 0, length - 32);

            byte[] byteRet = new byte[0];
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC"); // 创建AES加密器
                SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
                cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
                byteRet = cipher.doFinal(byteEncrypted);
            } catch (Exception e) {
                Log.i(TAG, "encryptJson error=" + e);
                return "decryptReply error";
            }
            if (byteRet.length > 0) {
                String strRet = new String(byteRet);
                return strRet;
            } else {
                return "decryptReply length is 0";
            }
        } else {
            return "reply less than 32 bytes.";
        }
    }

    private static byte[] genFunctionCmd(String token, String dataJson, byte[] device_id, int tick) {
        byte[] encryptedJson = encryptJson(dataJson, token);
        byte[] byteRet = new byte[encryptedJson.length + 32];

        System.arraycopy(hexStringToByteArray("2131"), 0, byteRet, 0, 2);
        System.arraycopy(genByteArrayFromInt32(encryptedJson.length + 32), 2, byteRet, 2, 2);
        System.arraycopy(hexStringToByteArray("00000000"), 0, byteRet, 4, 4);
        System.arraycopy(device_id, 0, byteRet, 8, 4);
        System.arraycopy(genByteArrayFromInt32(tick), 0, byteRet, 12, 4);

        System.arraycopy(hexStringToByteArray(token), 0, byteRet, 16, 16);

        System.arraycopy(encryptedJson, 0, byteRet, 32, encryptedJson.length);

        byte[] md5Ret = md5(byteRet);
        System.arraycopy(md5Ret, 0, byteRet, 16, 16);

        return byteRet;
    }

    public void sendAcOn() {
        synchronized(deviceInfoLock) {
            byte[] helloRet = sendHelloCmd();
            if (helloRet.length == 0) {
                return;
            }
            byte[] device_id = parseDeviceIdFromReply(helloRet);
            int tick = getInt32FromByteArray(helloRet, 12);

            String json = genAcOnJson(mModel);
            byte[] byteCmd = genFunctionCmd(mToken, json, device_id, tick + 1);
            byte[] byteRet = udpSendAndReceive(byteCmd);
            Log.i(TAG, "sendAcOn return=" + decryptReply(byteRet, mToken));
        }
    }

    public void sendAcOff() {
        synchronized(deviceInfoLock) {
            byte[] helloRet = sendHelloCmd();
            if (helloRet.length == 0) {
                return;
            }
            byte[] device_id = parseDeviceIdFromReply(helloRet);
            int tick = getInt32FromByteArray(helloRet, 12);

            String json = genAcOffJson(mModel);
            byte[] byteCmd = genFunctionCmd(mToken, json, device_id, tick + 1);
            byte[] byteRet = udpSendAndReceive(byteCmd);
            Log.i(TAG, "sendAcOff return=" + decryptReply(byteRet, mToken));
        }
    }

    public void sendUsbOn() {
        synchronized(deviceInfoLock) {
            byte[] helloRet = sendHelloCmd();
            if (helloRet.length == 0) {
                return;
            }
            byte[] device_id = parseDeviceIdFromReply(helloRet);
            int tick = getInt32FromByteArray(helloRet, 12);

            String json = genUsbOnJson(mModel);
            byte[] byteCmd = genFunctionCmd(mToken, json, device_id, tick + 1);
            byte[] byteRet = udpSendAndReceive(byteCmd);
            Log.i(TAG, "sendUsbOn return=" + decryptReply(byteRet, mToken));
        }
    }

    public void sendUsbOff() {
        synchronized(deviceInfoLock) {
            byte[] helloRet = sendHelloCmd();
            if (helloRet.length == 0) {
                return;
            }
            byte[] device_id = parseDeviceIdFromReply(helloRet);
            int tick = getInt32FromByteArray(helloRet, 12);

            String json = genUsbOffJson(mModel);
            byte[] byteCmd = genFunctionCmd(mToken, json, device_id, tick + 1);
            byte[] byteRet = udpSendAndReceive(byteCmd);
            Log.i(TAG, "sendUsbOff return=" + decryptReply(byteRet, mToken));
        }
    }

    private byte[] sendHelloCmd() {
        String strHelloCmd = "21310020ffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        byte[] byteHelloCmd = hexStringToByteArray(strHelloCmd);

        byte[] byteHelloRet = udpSendAndReceive(byteHelloCmd);
        return byteHelloRet;
    }
    
    private static int getInt16FromByteArray(byte[] bytes, int startIndex) {
        if (bytes == null || startIndex < 0 || startIndex + 1 >= bytes.length) {
            Log.i(TAG, "getInt16FromByteArray error");
            return 0;
        }
 
        // 这里使用了位操作符来组成int值
        // 假设是大端顺序（高位字节在前）
        int value = ((bytes[startIndex + 0] & 0xFF) << 8) |
                    (bytes[startIndex + 1] & 0xFF);
 
        return value;
    }

    private String miioBytesToHexStr(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();

        int realMsgLength = 0;
        if (bytes.length >= 4) {
            if (bytes[0] == 0x21 && bytes[1] == 0x31) {
                realMsgLength = getInt16FromByteArray(bytes, 2);
            }
        }

        int curLen = 0;
        for (byte b : bytes) {
            if (curLen >= realMsgLength) {
                break;
            }
            if (curLen % 16 == 0) {
                hexString.append("\n");
            }
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                // 如果是一位的话，要补0
                hexString.append('0');
            }
            hexString.append(hex + " ");
            curLen++;
        }
        return hexString.toString();
    }

    private byte[] udpSendAndReceive(byte[] byteCmd) {
        byte[] byteRet = new byte[0];
        DatagramSocket ds;
        InetAddress udpAddr;

        if (byteCmd.length == 0) {
            Log.e(TAG, "input cmd empty.");
            return byteRet;
        }

        try {
            Log.i(TAG, "udp target=" + mIp);
            udpAddr = InetAddress.getByName(mIp);
        } catch(Exception e) {
            Log.i(TAG, "InetAddress.getByName e=", e);
            return byteRet;
        }

        if (mIp.isEmpty()) {
            Log.e(TAG, "udp target empty.");
            return byteRet;
        }

        try {
            ds = new DatagramSocket();
        } catch(Exception e) {
            Log.i(TAG, "udp socket e=", e);
            return byteRet;
        }

        try {
            Log.i(TAG, "send cmd=" + miioBytesToHexStr(byteCmd));
            DatagramPacket dp = new DatagramPacket(byteCmd, byteCmd.length, udpAddr, 54321);
            ds.send(dp);
            Log.i(TAG, "send cmd done.");
        } catch(Exception e) {
            Log.i(TAG, "DatagramPacket send e=", e);
            return byteRet;
        }

        try {
            ds.setReuseAddress(true);
            ds.setBroadcast(true);
        } catch(Exception e) {
            Log.i(TAG, "udp setReuseAddress and broadcase e=", e);
            return byteRet;
        }

        byte[] inBuf = new byte[1024];
        try {
            DatagramPacket inPacket = new DatagramPacket(inBuf, inBuf.length);
            ds.setSoTimeout(1000);
            Log.i(TAG, "receive cmd start.");
            ds.receive(inPacket);
            Log.i(TAG, "receive cmd done.");

            String message = miioBytesToHexStr(inPacket.getData());
            Log.i(TAG, "udp receive msg=" + message);
            ds.close();
            return inPacket.getData();
        } catch(Exception e) {
            Log.i(TAG, "udp receive e=", e);
            return byteRet;
        }
    }
}
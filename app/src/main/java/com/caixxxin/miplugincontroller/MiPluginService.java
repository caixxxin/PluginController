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


public class MiPluginService extends Service {
    private final IBinder binder = new MiPluginBinder();
    String TAG = "MiPluginService";
    String mIp = "";
    String mToken = "";

    int mBattLevel = 0;
    int mBattScale = 0;
    int mBattTemp = 0;
    int mBattStatus = 0;

    int mPort;
    boolean running;

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
                
                if (mBattLevel > 80 && (mBattStatus == BatteryManager.BATTERY_STATUS_CHARGING || mBattStatus == BatteryManager.BATTERY_STATUS_FULL)) {
                    sendUsbOff();
                } else if (mBattLevel < 20 && (mBattStatus == BatteryManager.BATTERY_STATUS_DISCHARGING || mBattStatus == BatteryManager.BATTERY_STATUS_NOT_CHARGING ||mBattStatus == BatteryManager.BATTERY_HEALTH_UNKNOWN)) {
                    sendUsbOn();
                }
                
                serviceHandler.sendEmptyMessageDelayed(0, 60000);
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
        running = true;
        mPort = -1;
        SharedPreferences sharedPreferences = getSharedPreferences("pluginData", Context .MODE_PRIVATE);
        mIp = sharedPreferences.getString("ip", "");
        mToken = sharedPreferences.getString("token", "");
        Log.i(TAG, "last ip=" + mIp + " token=" + mToken);

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
        running = false;
        super.onDestroy();
        Log.i(TAG, "onDestroy");

        unregisterReceiver(batteryReceiver);
    }

    public void setIpAndToken(String ip, String token) {
        Log.i(TAG, "set ip=" + ip + " token=" + token);
        mIp = ip;
        mToken = token;
        //步骤1：创建一个SharedPreferences对象
        SharedPreferences sharedPreferences= getSharedPreferences("pluginData", Context.MODE_PRIVATE);
        //步骤2： 实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = sharedPreferences.edit();
        //步骤3：将获取过来的值放入文件
        editor.putString("ip", mIp);
        editor.putString("token", mToken);

        //步骤4：提交               
        editor.apply();
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

    public byte[] hexStringToByteArray(String hexString) {
        byte[] byteArray = new byte[hexString.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            int index = i * 2;
            int high = Integer.parseInt(hexString.substring(index, index + 1), 16);
            int low = Integer.parseInt(hexString.substring(index + 1, index + 2), 16);
            byteArray[i] = (byte) ((high << 4) + low);
        }
        return byteArray;
    }

    public String commonBytesToHexStr(byte[] bytes) {
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

    public String miioBytesToHexStr(byte[] bytes) {
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

    public int getInt32FromByteArray(byte[] bytes, int startIndex) {
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

    public byte[] getByteArrayFromInt32(int intValue) {
        byte[] result = new byte[4];
        result[0] = (byte) ((intValue >> 24) & 0xFF);
        result[1] = (byte) ((intValue >> 16) & 0xFF);
        result[2] = (byte) ((intValue >> 8) & 0xFF);
        result[3] = (byte) (intValue & 0xFF);
        return result;
    }

    public int getInt16FromByteArray(byte[] bytes, int startIndex) {
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


    public byte[] sendHello() {
        byte[] byteRet = new byte[0];
        DatagramSocket ds;
        InetAddress udpaddr;

        try {
            Log.i(TAG, "hello target=" + mIp);
            udpaddr = InetAddress.getByName(mIp);
        } catch(Exception e) {
            Log.i(TAG, "InetAddress.getByName e=", e);
            return byteRet;
        }

        if (mIp.isEmpty()) {
            return byteRet;
        }

        try {
            ds = new DatagramSocket();
        } catch(Exception e) {
            Log.i(TAG, "udp socket e=", e);
            return byteRet;
        }
        try {
            ds.setReuseAddress(true);
            ds.setBroadcast(true);
        } catch(Exception e) {
            Log.i(TAG, "udp setReuseAddress e=", e);
            return byteRet;
        }

        String hellocmd = "21310020ffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        byte[] bstr = hexStringToByteArray(hellocmd);

        try {
            Log.i(TAG, "sendHello cmd=" + miioBytesToHexStr(bstr));
            DatagramPacket dp = new DatagramPacket(bstr, bstr.length, udpaddr, 54321);
            ds.send(dp);
            Log.i(TAG, "sendHello send done.");
        } catch(Exception e) {
            Log.i(TAG, "DatagramPacket e=", e);
            return byteRet;
        }

        byte[] inBuf = new byte[1024];
        try {
            DatagramPacket inPacket = new DatagramPacket(inBuf, inBuf.length);
            ds.setSoTimeout(1000);
            Log.i(TAG, "sendHello start receive.");
            ds.receive(inPacket);
            Log.i(TAG, "sendHello receive done.");

            String message = miioBytesToHexStr(inPacket.getData());
            Log.i(TAG, "udp receive msg=" + message);
            ds.close();
            return inPacket.getData();
        } catch(Exception e) {
            Log.i(TAG, "udp receive e=", e);
            return byteRet;
        }
    }

    public void sendUsbOn() {
        byte[] helloRet = sendHello();
        if (helloRet.length > 0) {
            int tick = 0;
            tick = getInt32FromByteArray(helloRet, 12);

            DatagramSocket ds;
            InetAddress udpaddr;

            try {
                udpaddr = InetAddress.getByName(mIp);
            } catch(Exception e) {
                Log.i(TAG, "InetAddress.getByName e=", e);
                return;
            }

            try {
                ds = new DatagramSocket();
            } catch(Exception e) {
                Log.i(TAG, "udp socket e=", e);
                return;
            }
            try {
                ds.setReuseAddress(true);
                ds.setBroadcast(true);
            } catch(Exception e) {
                Log.i(TAG, "udp setReuseAddress e=", e);
                return;
            }

            byte[] bstr = usbOnCmd(tick);
            try {
                Log.i(TAG, "sendUsbOn cmd=" + miioBytesToHexStr(bstr));
                DatagramPacket dp = new DatagramPacket(bstr, bstr.length, udpaddr, 54321);
                ds.send(dp);
                Log.i(TAG, "sendUsbOn send done.");
            } catch(Exception e) {
                Log.i(TAG, "DatagramPacket e=", e);
                return;
            }

            byte[] inBuf = new byte[1024];
            try {
                DatagramPacket inPacket = new DatagramPacket(inBuf, inBuf.length);
                ds.setSoTimeout(1000);
                Log.i(TAG, "sendUsbOn start receive.");
                ds.receive(inPacket);
                Log.i(TAG, "sendUsbOn receive done.");

                String message = commonBytesToHexStr(inPacket.getData());
                Log.i(TAG, "udp receive msg=" + message);
                
            } catch(Exception e) {
                Log.i(TAG, "udp receive e=", e);
                return;
            }
            ds.close();
        } else {
            Log.i(TAG, "hello cmd ret error.");
        }
    }

    public void sendUsbOff() {
        byte[] helloRet = sendHello();
        if (helloRet.length > 0) {
            int tick = 0;
            tick = getInt32FromByteArray(helloRet, 12);

            DatagramSocket ds;
            InetAddress udpaddr;

            try {
                udpaddr = InetAddress.getByName(mIp);
            } catch(Exception e) {
                Log.i(TAG, "InetAddress.getByName e=", e);
                return;
            }

            try {
                ds = new DatagramSocket();
            } catch(Exception e) {
                Log.i(TAG, "udp socket e=", e);
                return;
            }
            try {
                ds.setReuseAddress(true);
                ds.setBroadcast(true);
            } catch(Exception e) {
                Log.i(TAG, "udp setReuseAddress e=", e);
                return;
            }

            byte[] bstr = usbOffCmd(tick);
            try {
                Log.i(TAG, "sendUsbOff cmd=" + miioBytesToHexStr(bstr));
                DatagramPacket dp = new DatagramPacket(bstr, bstr.length, udpaddr, 54321);
                ds.send(dp);
                Log.i(TAG, "sendUsbOff send done.");
            } catch(Exception e) {
                Log.i(TAG, "DatagramPacket e=", e);
                return;
            }

            byte[] inBuf = new byte[1024];
            try {
                DatagramPacket inPacket = new DatagramPacket(inBuf, inBuf.length);
                ds.setSoTimeout(1000);
                Log.i(TAG, "sendUsbOff start receive.");
                ds.receive(inPacket);
                Log.i(TAG, "sendUsbOff receive done.");

                String message = miioBytesToHexStr(inPacket.getData());
                Log.i(TAG, "udp receive msg=" + message);
                
            } catch(Exception e) {
                Log.i(TAG, "udp receive e=", e);
                return;
            }
            ds.close();
        } else {
            Log.i(TAG, "hello cmd ret error.");
        }
    }

    public byte[] usbOnCmd(int tick) {
        String usbOnCmd = "21310060000000000003266800000b07"
                        + "d4130e242964c56c3ff610140c6531ec"
                        + "eeafb858564ea343def1840448cfe191"
                        + "d4ed434b83eb1477cdda1f5c68612d66"
                        + "93ca1a4f0579bad65eac66fa481dbc77"
                        + "68b8cb80a3f52bb273bd77892e9b5d39";
        byte[] usbOnBstr = hexStringToByteArray(usbOnCmd);
        byte[] tickBstr = getByteArrayFromInt32(tick + 1);
        System.arraycopy(tickBstr, 0, usbOnBstr, 12, 4);

        byte[] md5Ret = md5(usbOnBstr);
        System.arraycopy(md5Ret, 0, usbOnBstr, 16, 16);

        return usbOnBstr;
    }

    public byte[] usbOffCmd(int tick) {
        String usbOffCmd = "21310060000000000003266800028e39"
                         + "d4130e242964c56c3ff610140c6531ec"
                         + "eeafb858564ea343def1840448cfe191"
                         + "d9f0ca62530c40a7951c28aef4583795"
                         + "b0af52499fa840c80f2debc95af871e6"
                         + "818b3d9245487c595a8de02fd762c57c";
        byte[] usbOffBstr = hexStringToByteArray(usbOffCmd);
        byte[] tickBstr = getByteArrayFromInt32(tick + 1);
        System.arraycopy(tickBstr, 0, usbOffBstr, 12, 4);

        byte[] md5Ret = md5(usbOffBstr);
        System.arraycopy(md5Ret, 0, usbOffBstr, 16, 16);

        return usbOffBstr;
    }

    public byte[] md5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not find MD5 algorithm", e);
        }
    }
}
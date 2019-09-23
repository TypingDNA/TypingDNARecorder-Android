/**
 * TypingDNA - Typing Biometrics Recorder for Android
 * https://www.typingdna.com
 *
 * @version 3.1
 * @author Raul Popa & Stefan Endres
 * @copyright TypingDNA Inc. https://www.typingdna.com
 * @license http://www.apache.org/licenses/LICENSE-2.0
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Typical usage:
 * TypingDNARecorderMobile tdna = new TypingDNARecorderMobile(this); // creates a new TypingDNA object and starts recording
 * String typingPattern = tdna.getTypingPattern(type, length, text, textId, targetId, caseSensitive);
 * <p>
 * Optional:
 * tdna.stop(); // ends recording and clears history stack (returns recording flag: false)
 * tdna.start(); // restarts the recording after a stop (returns recording flag: true)
 * tdna.reset(); // restarts the recording anytime, clears history stack and starts from scratch (returns nothing)
 */

package com.typingdna;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TypingDNARecorderMobile extends TypingDNARecorderBase implements SensorEventListener {
    private TypingDNAOverlayService typingDNAOverlayService;
    private static Activity mActivity;
    TypingDNARecorderMobile self;
    private Boolean mServiceIsBound;
    private static KeyCharacterMap mKeyCharacterMap;
    private int lastPressTime;
    private int lastReleaseTime;
    private int last2ReleaseTime;
    private Integer lastPressX = -1;
    private Integer lastPressY = -1;
    private int ut1 = 0;
    private SensorManager mSensorManager;
    private Sensor mAccelerationSensor;
    private Sensor mGyroscope;
    private int linear_acceleration[] = new int[3];
    private static ArrayList<Integer> accelerationZVector = new ArrayList<Integer>();
    private int orientation[] = new int[3];
    private int gyroscope[] = new int[3];
    private static ArrayList<Integer> gyroscopeYVector = new ArrayList<Integer>();
    private static ArrayList<Integer> gyroscopeXVector = new ArrayList<Integer>();
    private static ArrayList<Integer> sensorEventTsVector = new ArrayList<Integer>();
    private Boolean recordSensors = true;
    private Timer sensorSampler;
    private static int sampleSize = 21;
    private ServiceConnection mServiceConnection;
    private static byte samplingInterval = 16;
    private Boolean pressCalculated = false;
    private Boolean pressWorks = false;
    private static Boolean overlayEnabled = true;
    private static final int[] keyCodes = new int[]{29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 62, 75, 120, 67, 74, 70, 124, 259, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            16};


    public TypingDNARecorderMobile(Activity activity) {
        super();
        mActivity = activity;
        mobile = true;
        self = this;
    }

    //Overrides

    @Override
    public void initialize() {
        mServiceIsBound = false;
        recordSensors = false;
        if (Build.VERSION.SDK_INT >= 11)
            mKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        else
            mKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.ALPHA);
        if (keyCodes != null) {
            setKeyCodes(keyCodes);
        }
        super.initialize();
        lastPressTime = getTime();
        lastReleaseTime = getTime();
        initServiceConnection();

    }

    @Override
    public String getDeviceId() {
        String androidId = Settings.Secure.getString(mActivity.getContentResolver(), Settings.Secure.ANDROID_ID);
        return hash32(androidId);
    }

    @Override
    public String getTypingPattern(int type, int length, String text, int textId, Integer target, boolean caseSensitive) {
        if (mActivity != null && target != null && (text == null || text.equals(""))) {
            try {
                text = ((EditText) mActivity.findViewById(target)).getText().toString();
            } catch (Exception e) {
                Log.e("TypingDNARecorder", "getTypingPattern - get text error: " + e.getMessage());
            }
        }
        setPressType(getRecorderPressType());
        return super.getTypingPattern(type, length, text, textId, target, caseSensitive);
    }

    @Override
    public String getTypingPattern(int type, int length, String text, int textId, Integer target) {
        return getTypingPattern(type, length, text, textId, target, false);
    }

    @Override
    public String getTypingPattern(int type, int length, String text, int textId) {
        return getTypingPattern(type, length, text, textId, null, false);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            linear_acceleration[0] = (int) (100 * event.values[0]);
            linear_acceleration[1] = (int) (100 * event.values[1]);
            linear_acceleration[2] = (int) (100 * event.values[2]);

            float accX = linear_acceleration[0];
            float accY = linear_acceleration[1];
            float accZ = linear_acceleration[2];

            orientation[1] = (int) ((Math.atan2(-accY, Math.sqrt(Math.pow(accX, 2) + Math.pow(accZ, 2))) * 180 / Math.PI) * 10);
            orientation[2] = (int) ((Math.atan2(-accX, Math.sqrt(Math.pow(accY, 2) + Math.pow(accZ, 2))) * 180 / Math.PI) * 10);
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroscope[0] = (int) (573 * event.values[0]);
            gyroscope[1] = (int) (573 * event.values[1]);
            gyroscope[2] = (int) (573 * event.values[2]);
        }
    }

    @Override
    public void start() {
        lastPressTime = getTime();
        startSensors();
        if (!mServiceIsBound) {
            startOverlayService();
        }
        super.start();
    }

    @Override
    public void reset() {
        lastPressTime = getTime();
        super.reset();
    }

    @Override
    public void stop() {
        if (mServiceIsBound) {
            stopOverlayService();
        }
        stopSensors();
        super.stop();
    }

    public void pause() {
        stopSensors();
        if (typingDNAOverlayService != null) {
            typingDNAOverlayService.pause();
        }
    }

    private boolean hasSensors() {
        return hasAccelerometer() && hasGyroscope();
    }

    private boolean hasAccelerometer() {
        if (mActivity != null) {
            PackageManager manager = mActivity.getPackageManager();
            return manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);

        }
        return false;
    }

    private boolean hasGyroscope() {
        if (mActivity != null) {
            PackageManager manager = mActivity.getPackageManager();
            return manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);

        }
        return false;
    }

    public void startSensors() {
        if (mActivity != null) {
            mSensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
            clearSensorData();
            boolean hasSensors = false;
            if (hasAccelerometer()) {
                mAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                mSensorManager.registerListener(this, mAccelerationSensor,
                        2000
                );
                hasSensors = true;
            }
            if (hasGyroscope()) {
                mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                mSensorManager.registerListener(this, mGyroscope,
                        2000
                );
                hasSensors = true;
            }
            if (hasSensors) {
                startSensorSampler();
            }
        }
    }

    public void stopSensors() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this, mAccelerationSensor);
            mSensorManager.unregisterListener(this, mGyroscope);
            mSensorManager = null;
            mAccelerationSensor = null;
            mGyroscope = null;
            stopSensorSampler();
            clearSensorData();
        }
    }

    private void clearSensorData() {
        gyroscopeXVector.clear();
        gyroscopeYVector.clear();
        accelerationZVector.clear();
        sensorEventTsVector.clear();
    }

    public void addTarget(int targetId) {
        EditText target = (EditText) mActivity.findViewById(targetId);
        addEditTextListeners(target);
        super.addTarget(targetId);
    }

    public void addTarget(int[] targetIds) {
        for (int i = 0; i < targetIds.length; i++) {
            addTarget(targetIds[i]);
        }
    }

    public void removeTarget(int targetId) {
        super.removeTarget(targetId);
    }

    public void removeTarget(int[] targetIds) {
        for (int i = 0; i < targetIds.length; i++) {
            removeTarget(targetIds[i]);
        }
    }

    public void mobileKeyDown(int x, int y) {
        int time = getTime();
        if (!pressWorks) {
            pressWorks = true;
        }
        if (time != lastPressTime) {
            lastPressX = x;
            lastPressY = y;
            lastPressTime = time;
        }
    }

    public void mobileKeyReleased(int keyCode, char keyChar, boolean modifiers, Integer time, Integer target, Object[] kpGet, String xy) {
        if ((!recording && !diagramRecording) || keyCode >= maxKeyCode) {
            return;
        }
        if (!isTarget(target)) {
            return;
        }
        int t0 = ut1;
        ut1 = time;
        int seekTime = (int) (ut1 - t0);
        int downTime = (int) kpGet[0];
        int pressTime = (downTime != 0) ? (int) (ut1 - downTime) : 0;
        if (recording == true && !modifiers) {
            if (keyCodesObj[keyCode] == 1) {
                int[] arr = new int[]{keyCode, seekTime, pressTime, prevKeyCode, ut1, target};
                historyAdd(arr);
                prevKeyCode = keyCode;
            }
        }
        if (diagramRecording == true) {
            String kp0 = (String) kpGet[1];
            String kp1 = (String) kpGet[2];
            String kp2 = (String) kpGet[3];
            String kp3 = (String) kpGet[4];
            Object[] arrD = new Object[]{keyCode, seekTime, pressTime, keyChar, ut1, target, kp0, kp1, kp2, kp3, xy};
            historyAddDiagram(arrD);
        }
    }

    public void addEditTextListeners(EditText et) {
        final EditText _et = et;
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                try {
                    int time = getTime();
                    if (time != lastReleaseTime) {
                        last2ReleaseTime = lastReleaseTime;
                        lastReleaseTime = time;
                        if (arg0.length() > 0 && arg0.length() > (arg1 + arg2)) {
                            Object[] kpGet = kpGetAll();
                            String currentChar = Character.toString(arg0.charAt(arg1 + arg2));
                            KeyEvent[] events = mKeyCharacterMap.getEvents(currentChar.toCharArray());
                            int keycode = 0;
                            if (events != null) {
                                if (events[0].getAction() == 0) {
                                    keycode = events[0].getKeyCode();
                                }
                            }
                            Boolean modifiers = false; // ignore modifiers for mobile (works with modifiers too but useless)
                            String xy = lastPressX.toString() + "," + lastPressY.toString();
                            mobileKeyReleased(keycode, arg0.charAt(arg1 + arg2), modifiers, time, _et.getId(), kpGet, xy);
                        }
                    }
                } catch (Exception e) {
                    Log.e("TypingDNARecorder", "onTextChanged Error:" + e.getMessage());
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });
        et.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                mobileKeyDown(0, 0);
                return false;
            }
        });

    }

    public ArrayList<Integer[]> kpADifArr(Object[] arr) {
        int length = arr.length - 1;
        ArrayList<Integer[]> ret = new ArrayList<Integer[]>();
        if (length < 2) {
            Integer[] newArr = new Integer[]{0};
            ret.add(newArr);
            ret.add(newArr.clone());
            return ret;
        }
        int[] firstArr = new int[length + 1];
        firstArr[0] = 0;
        final Integer[] newArr = new Integer[length];
        final Integer[] returnArr = new Integer[length];
        for (int i = 0; i < length; i++) {
            firstArr[i + 1] = (int) arr[i + 1] - (int) arr[i];
        }
        for (int i = 0; i < length; i++) {
            int newVal = firstArr[i + 1] - firstArr[i];
            newArr[i] = newVal;
            returnArr[i] = Math.round(Math.abs(newVal));
        }
        ret.add(newArr);
        ret.add(returnArr);
        return ret;
    }

    private ArrayList<Integer[]> kpRDifArr(Object[] arr) {
        int length = arr.length - 2;
        ArrayList<Integer[]> ret = new ArrayList<Integer[]>();
        if (length < 0) {
            Integer[] newArr = new Integer[]{0};
            ret.add(newArr);
            ret.add(newArr.clone());
            return ret;
        }
        int posMax = 0;
        int posMin = 0;
        Integer[] firstArr;
        if (length > 0) {
            firstArr = new Integer[length];
            for (int i = 0; i < length; i++) {
                firstArr[i] = (int) arr[i + 1] - (int) arr[i];
                if (firstArr[i] > firstArr[posMax]) {
                    posMax = i;
                }
                if (firstArr[i] < firstArr[posMin]) {
                    posMin = i;
                }
            }
        } else {
            int newVal = (int) arr[1] - (int) arr[0];
            firstArr = new Integer[]{newVal};
        }
        Integer[] returnArr = new Integer[]{
                posMax - 1,
                posMax,
                posMax + 1,
                posMax + 2,
                posMax + 3,
                posMin - 1,
                posMin,
                posMin + 1,
                posMin + 2,
                posMin + 3};
        ret.add(firstArr);
        ret.add(returnArr);
        return ret;
    }

    private Object[] kpGetAll() {
        if (accelerationZVector.size() < 2) {
            int returnVal = (lastPressTime >= last2ReleaseTime) ? lastPressTime : 0;
            Integer[] returnMotionArr = getDeviceMotion();
            String returnMotionStr = arrayJoin(returnMotionArr, ",");
            return new Object[]{returnVal, returnMotionStr, "0", "0", "0"};
        } else {
            ArrayList<Integer[]> kpZA2 = new ArrayList<Integer[]>();
            kpZA2 = kpADifArr((accelerationZVector.toArray()));
            ArrayList<Integer[]> kpXR2 = new ArrayList<Integer[]>();
            kpXR2 = kpRDifArr((gyroscopeXVector.toArray()));
            ArrayList<Integer[]> kpYR2 = new ArrayList<Integer[]>();
            kpYR2 = kpRDifArr((gyroscopeYVector.toArray()));
            Integer[] kpza = (Integer[]) kpZA2.get(0);
            Integer[] kpzaAbs = kpZA2.get(1);
            Integer[] kpXR = kpXR2.get(0);
            Integer[] kpxPos = kpXR2.get(1);
            Integer[] kpYR = kpYR2.get(0);
            Integer[] kpyPos = kpYR2.get(1);
            if (accelerationZVector.size() > 2) {
                accelerationZVector.remove(0);
            }
            if (gyroscopeXVector.size() > 2) {
                gyroscopeXVector.remove(0);
            }
            if (gyroscopeYVector.size() > 2) {
                gyroscopeYVector.remove(0);
            }
            if (sensorEventTsVector.size() > 2) {
                sensorEventTsVector.remove(0);
            }

            int returnVal = (lastPressTime >= last2ReleaseTime) ? lastPressTime : 0;

            if (pressWorks == false) {
                Integer[] kpPos = concat(kpxPos, kpyPos);
                Arrays.sort(kpPos);
                List<Integer> kpxyPos = new ArrayList<Integer>();
                for (int i = 1; i < kpPos.length; i++) {
                    if (kpPos[i] != kpPos[i - 1]) {
                        kpxyPos.add(kpPos[i]);
                    }
                }
                int lastKpza = 0;
                int lastKpTime = (int) sensorEventTsVector.get(sensorEventTsVector.size() - 1);
                for (int i = 0; i < kpxyPos.size(); i++) {
                    int j = kpxyPos.get(i);
                    int minj = (kpzaAbs.length > 8) ? 2 : ((kpzaAbs.length > 4) ? 1 : 0);
                    if (j > minj && j < kpzaAbs.length && kpzaAbs[j] > lastKpza) {
                        lastKpza = kpzaAbs[j];
                        lastKpTime = sensorEventTsVector.get(j);
                    }
                }
                returnVal = lastKpTime;
                pressCalculated = true;
            } else {
                lastPressTime = 0;
            }
            Integer[] deviceMotion = getDeviceMotion();
            String kp0 = arrayJoin(deviceMotion, ",");
            String kp1 = arrayJoin(kpza, ",");
            String kp2 = arrayJoin(kpXR, ",");
            String kp3 = arrayJoin(kpYR, ",");
            clearSensorData();
            return new Object[]{returnVal, kp0, kp1, kp2, kp3};
        }
    }

    private Integer[] getDeviceMotion() {
        Integer[] deviceMotion = new Integer[8];
        if (linear_acceleration != null && linear_acceleration.length > 2) {
            deviceMotion[0] = linear_acceleration[0];
            deviceMotion[1] = linear_acceleration[1];
        }
        if (accelerationZVector != null && accelerationZVector.size() > 1) {
            deviceMotion[2] = accelerationZVector.get(accelerationZVector.size() - 1);
        }
        if (gyroscopeXVector != null && gyroscopeXVector.size() > 1) {
            deviceMotion[3] = gyroscopeXVector.get(gyroscopeXVector.size() - 1);
        }
        if (gyroscopeXVector != null && gyroscopeXVector.size() > 1) {
            deviceMotion[4] = gyroscopeYVector.get(gyroscopeYVector.size() - 1);
        }
        if (gyroscope != null && gyroscope.length > 2) {
            deviceMotion[5] = gyroscope[2];
        }
        if (orientation != null && orientation.length > 2) {
            deviceMotion[6] = orientation[1];
            deviceMotion[7] = orientation[2];
        }
        return deviceMotion;
    }

    public static Integer[] concat(Integer[] a, Integer[] b) {
        int length = a.length + b.length;
        Integer[] result = new Integer[length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private void stopSensorSampler() {
        if (sensorSampler != null) {
            sensorSampler.cancel();
            sensorSampler.purge();
            sensorSampler = null;
        }
    }

    private void startSensorSampler() {
        if (sensorSampler == null) {
            sensorSampler = new Timer();
            sensorSampler.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (recordSensors) {
                        recordSensorData(gyroscopeXVector, gyroscope[0]);
                        recordSensorData(gyroscopeYVector, gyroscope[1]);
                        recordSensorData(accelerationZVector, linear_acceleration[2]);
                        recordSensorTime(sensorEventTsVector, getTime());
                    }
                }
            }, 0, samplingInterval);
        }
    }

    private byte getRecorderPressType() {
        byte ret = 0;
        if (pressWorks) {
            ret = 1;
        } else if (pressCalculated) {
            ret = 2;
        }
        if (pressWorks && pressCalculated) {
            ret = 3;
        }
        return ret;
    }

    private void recordSensorData(ArrayList<Integer> vector, Integer data) {
        if (vector.size() >= sampleSize) {
            vector.remove(0);
        }
        vector.add(data);
    }

    private void recordSensorTime(ArrayList<Integer> vector, int data) {
        if (vector.size() >= sampleSize) {
            vector.remove(0);
        }
        vector.add(data);
    }

    public boolean checkOverlayPermission() {
        if (mActivity == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(mActivity.getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + mActivity.getPackageName())
                );
                mActivity.startActivityForResult(intent, 0);
                mActivity.finish();
                return false;
            } else {
                return true;
            }
        }
        return true;
    }

    private void initServiceConnection() {
        mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                typingDNAOverlayService = ((TypingDNAOverlayService.LocalBinder) service).getService();
                typingDNAOverlayService.setmTypingDNARecorderMobile(self);
            }

            public void onServiceDisconnected(ComponentName className) {
                typingDNAOverlayService = null;
            }
        };
    }

    public void startOverlayService() {
        if (!overlayEnabled || !checkOverlayPermission()) {
            return;
        }
        try {
            if (!mServiceIsBound && mActivity != null && mServiceConnection != null) {
                mActivity.bindService(new Intent(mActivity.getApplicationContext(), TypingDNAOverlayService.class),
                        mServiceConnection,
                        Context.BIND_AUTO_CREATE);
                mServiceIsBound = true;
            }
        } catch (Exception e) {
            Log.e("TypingDNARecorder", "BindService Error:" + e.getMessage());
        }
    }

    public void stopOverlayService() {
        try {
            if (mServiceIsBound && mActivity != null && mServiceConnection != null) {
                // Detach our existing connection.
                mActivity.unbindService(mServiceConnection);
                mServiceIsBound = false;
            }
        } catch (Exception e) {
            Log.e("TypingDNARecorder", "UndindService Error" + e.getMessage());
        }
    }

}

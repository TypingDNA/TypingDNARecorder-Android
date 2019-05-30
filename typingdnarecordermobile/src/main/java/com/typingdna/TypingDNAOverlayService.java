/**
 * TypingDNA - Typing Biometrics Overlay Service
 * https://www.typingdna.com
 *
 *
 * @version 3.0
 * @author Raul Popa & Stefan Endres
 * @copyright TypingDNA Inc. https://www.typingdna.com
 * @license http://www.apache.org/licenses/LICENSE-2.0
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  This class draws a screen overlay in order to capture touch events.
 *  This is a workaround for key down event.
 */

package com.typingdna;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class TypingDNAOverlayService extends Service {

    private WindowManager.LayoutParams wParams;
    private WindowManager wm;
    private Overlay overlay;
    private boolean overlayVisible = false;
    private TypingDNARecorderMobile mTypingDNARecorderMobile;

    private class Overlay extends LinearLayout implements View.OnClickListener {

        public Overlay(Context context) {
            super(context);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if(mTypingDNARecorderMobile != null) {
                mTypingDNARecorderMobile.mobileKeyDown((int)event.getX(), (int)event.getY());
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            if(mTypingDNARecorderMobile != null) {
                mTypingDNARecorderMobile.mobileKeyDown(0,0);
            }
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

    }

    public class LocalBinder extends Binder {
        public TypingDNAOverlayService getService() {
            return TypingDNAOverlayService.this;
        }
    }

    @Override
    public void onCreate() {
        start();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID) {
        start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stop();
        super.onDestroy();
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void creteOverly(){
        overlay = new Overlay(this);
        wParams = new WindowManager.LayoutParams(
                1, 1, //Arbitrary size
                (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT :
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    public TypingDNARecorderMobile getmTypingDNARecorderMobile() {
        return mTypingDNARecorderMobile;
    }

    public void setmTypingDNARecorderMobile(TypingDNARecorderMobile mTypingDNARecorderMobile) {
        this.mTypingDNARecorderMobile = mTypingDNARecorderMobile;
    }

    public void displayOverlay(){
        try{
            wm.addView(overlay, wParams);
            overlayVisible = true;
        }catch(Exception e){
            Log.e("TypingDNARecorder", " Display Overlay Error:" + e.getMessage());
            overlayVisible = false;
        }
    }

    public void hideOverlay(){
        try{
            if (wm == null)  wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            if(overlay != null && overlayVisible) {
                wm.removeView(overlay);
                wParams = null;
                overlayVisible = false;
            }
        } catch (Exception e){
            Log.e("TypingDNARecorder", "Hide Overlay Error:" + e.getMessage());
        }
    }

    public void pause(){
        hideOverlay();
    }

    public void start(){
        if(!overlayVisible) {
            creteOverly();
            displayOverlay();
        }
    }

    public void stop(){
        hideOverlay();
    }
}

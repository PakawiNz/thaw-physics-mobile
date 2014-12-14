/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pkjm.thaw;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pkjm.thaw.analyser.Analyser;
import com.pkjm.thaw.camera2.Camera2BasicFragment;

public class ThawActivity extends Activity {

    public static ThawActivity activity;
    private Analyser analyser;
    private SharedPreferences prefs;
    private SurfaceView aBlankView;
    private SurfaceView aBlockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        activity = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Camera2BasicFragment fragment = Camera2BasicFragment.newInstance(
            Integer.parseInt((prefs.getString("text_exposure","2"))),
            prefs.getBoolean("check_ae_lock",true),
            prefs.getBoolean("check_af_lock",true));

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
        }

        aBlankView = (SurfaceView)findViewById(R.id.aBlankView);
        aBlockView = (SurfaceView)findViewById(R.id.aBlockView);

        TextView debugText = (TextView)findViewById(R.id.debug);
        Button showColorBtn = (Button)findViewById(R.id.showColor);
        View bbcontainer = findViewById(R.id.bbcontainer);
        showColorBtn.setBackgroundColor(Color.alpha(0));

        boolean check_debug = prefs.getBoolean("check_debug", false);
        if (check_debug){
            debugText.setVisibility(View.VISIBLE);
            bbcontainer.setVisibility(View.INVISIBLE);
        }else{
            debugText.setVisibility(View.INVISIBLE);
            bbcontainer.setVisibility(View.VISIBLE);
        }

        String ipv4 = prefs.getString("text_ipv4","192.168.1.0");
        int port = Integer.parseInt(prefs.getString("text_port","6437"));
        boolean check_autohost = prefs.getBoolean("check_autohost",false);

        this.analyser = new Analyser(
            this,fragment,debugText,showColorBtn,check_debug,ipv4,port);

    }


    @Override
    public void onStart() {
        super.onStart();
        analyser.start();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        analyser.stop();
    }

    public void restart(View view){
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    public void goSetting(View view){
        Intent intent = new Intent(getApplicationContext(),FragmentPreferences.class);
        startActivity(intent);
    }

    public void swapState(View view){
        if (analyser.swapBlState()){
            aBlankView.setVisibility(View.INVISIBLE);
            aBlockView.setVisibility(View.VISIBLE);
        }else{
            aBlockView.setVisibility(View.INVISIBLE);
            aBlankView.setVisibility(View.VISIBLE);
        }
    }

    public void grabWhite(View view){
        analyser.endCalibration();
        aBlankView.setVisibility(View.VISIBLE);
        findViewById(R.id.whiteCal).setVisibility(View.INVISIBLE);
    }
}

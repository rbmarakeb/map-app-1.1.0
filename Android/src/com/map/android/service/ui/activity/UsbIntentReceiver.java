package com.map.android.service.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.map.android.R;

/**
 * Created by fhuya on 11/12/14.
 */
public class UsbIntentReceiver extends ActionBarActivity {

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_intent_receiver);
        handleIntent(getIntent());
    }

    @Override
    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        finish();
    }
}

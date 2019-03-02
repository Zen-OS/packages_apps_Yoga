/*
 * Copyright (C) 2018 CypherOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zen.yoga.quickspace;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.internal.util.custom.weather.WeatherClient;
import com.android.internal.util.custom.weather.WeatherClient.WeatherInfo;
import com.android.internal.util.custom.weather.WeatherClient.WeatherObserver;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;

import java.util.List;
import java.util.ArrayList;

public class QuickEventsController {

    private static final String SETTING_DEVICE_INTRO_COMPLETED = "device_introduction_completed";
    private Context mContext;

    private String mEventTitle;
    private String mEventTitleSub;
    private OnClickListener mEventTitleSubAction = null;
    private int mEventSubIcon;

    private boolean mIsQuickEvent = false;

// Event identifiers start
     // Device Intro
    private boolean mEventIntro = false;
    private boolean mEventIntroClicked = false;
    private boolean mIsFirstTimeDone;
 // Event identifiers end

    public QuickEventsController(Context context) {
        mContext = context;
        initQuickEvents();
    }

    public void initQuickEvents() {
        mIsFirstTimeDone = Settings.System.getInt(mContext.getContentResolver(), SETTING_DEVICE_INTRO_COMPLETED, 0) != 0;
        deviceIntroEvent();
    }

    private void deviceIntroEvent() {
        if (mIsFirstTimeDone || mEventIntroClicked) {
            mEventIntro = false;
            return;
        }
        mIsQuickEvent = true;
        mEventIntro = true;
        mEventTitle = mContext.getResources().getString(R.string.quick_event_rom_intro_welcome);
        mEventTitleSub = mContext.getResources().getString(R.string.quick_event_rom_intro_learn);

        mEventTitleSubAction = new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(Settings.ACTION_DEVICE_INTRODUCTION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                try {
                    Launcher.getLauncher(mContext).startActivitySafely(view, intent, null);
                } catch (ActivityNotFoundException ex) {
                }
                mIsQuickEvent = false;
                mEventIntroClicked = true;
            }
        };
    }

    public boolean isQuickEvent() {
        return mIsQuickEvent;
    }

    public String getTitle() {
        return mEventTitle;
    }

    public String getActionTitle() {
        return mEventTitleSub;
    }

    public OnClickListener getAction() {
        return mEventTitleSubAction;
    }

    public int getActionIcon() {
        return mEventSubIcon;
    }
}

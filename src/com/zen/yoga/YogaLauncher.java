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
package com.zen.yoga;

import static com.android.launcher3.LauncherState.NORMAL;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.graphics.ColorUtils;
import android.view.View;

import com.zen.yoga.logging.PredictionsDispatcher;
import com.zen.yoga.qsb.QsbAnimationController;
import com.zen.yoga.quickspace.QuickSpaceView;
import com.zen.yoga.util.ComponentKeyMapper;

import com.android.launcher3.AppInfo;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherCallbacks;
import com.android.launcher3.Utilities;
import com.android.launcher3.R;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.uioverrides.WallpaperColorInfo.OnChangeListener;
import com.android.launcher3.util.Themes;
import com.google.android.libraries.gsa.launcherclient.ClientOptions;
import com.google.android.libraries.gsa.launcherclient.ClientService;
import com.google.android.libraries.gsa.launcherclient.LauncherClient;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class YogaLauncher extends Launcher {

    private LauncherClient mLauncherClient;
    private QsbAnimationController mQsbController;
    private final Bundle mUiInformation = new Bundle();

    public YogaLauncher() {
        setLauncherCallbacks(new YogaLauncherCallbacks(this));
    }

    public LauncherClient getClient() {
        return mLauncherClient;
    }

    public QsbAnimationController getQsbController() {
        return mQsbController;
    }

    public class YogaLauncherCallbacks implements LauncherCallbacks, OnSharedPreferenceChangeListener, OnChangeListener {

        public static final String SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";

        private final YogaLauncher mLauncher;
        private QuickSpaceView mQuickSpace;

        private SharedPreferences mPrefs;
        private OverlayCallbackImpl mOverlayCallbacks;

        private boolean mStarted;
        private boolean mResumed;
        private boolean mAlreadyOnHome;
        public Runnable mUpdatePredictionsIfResumed = new Runnable() {
            @Override
            public void run() {
                updatePredictions(false);
            }
        };

        public YogaLauncherCallbacks(YogaLauncher launcher) {
            mLauncher = launcher;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mQuickSpace = mLauncher.findViewById(R.id.reserved_container_workspace);

            mPrefs = Utilities.getPrefs(mLauncher);
            mOverlayCallbacks = new OverlayCallbackImpl(mLauncher);
            mLauncherClient = new LauncherClient(mLauncher, mOverlayCallbacks, new ClientOptions(((mPrefs.getBoolean(SettingsFragment.KEY_MINUS_ONE, true) ? 1 : 0) | 2 | 4 | 8)));
            mOverlayCallbacks.setClient(mLauncherClient);
            mQsbController = new QsbAnimationController(mLauncher);
            mUiInformation.putInt("system_ui_visibility", mLauncher.getWindow().getDecorView().getSystemUiVisibility());
            WallpaperColorInfo instance = WallpaperColorInfo.getInstance(mLauncher);
            instance.addOnChangeListener(this);
            onExtractedColorsChanged(instance);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume() {
            mResumed = true;
            if (mStarted) {
                mAlreadyOnHome = true;
            }
            mLauncherClient.onResume();

            Handler handler = mLauncher.getDragLayer().getHandler();
            if (handler != null) {
                handler.removeCallbacks(mUpdatePredictionsIfResumed);
                Utilities.postAsyncCallback(handler, mUpdatePredictionsIfResumed);
            }
        }

        @Override
        public void onStart() {
            mStarted = true;
            mLauncherClient.onStart();
        }

        @Override
        public void onStop() {
            mStarted = false;
            if (!mResumed) {
                mAlreadyOnHome = false;
            }
            mLauncherClient.onStop();
        }

        @Override
        public void onPause() {
            if (mQuickSpace != null) {
                mQuickSpace.onPause();
            }
            mResumed = false;
            mLauncherClient.onPause();
        }

        @Override
        public void onDestroy() {
            if (!mLauncherClient.isDestroyed()) {
                mLauncherClient.getActivity().unregisterReceiver(mLauncherClient.mInstallListener);
            }
            mLauncherClient.setDestroyed(true);
            mLauncherClient.getBaseService().disconnect();
            if (mLauncherClient.getOverlayCallback() != null) {
                mLauncherClient.getOverlayCallback().mClient = null;
                mLauncherClient.getOverlayCallback().mWindowManager = null;
                mLauncherClient.getOverlayCallback().mWindow = null;
                mLauncherClient.setOverlayCallback(null);
            }
            ClientService service = mLauncherClient.getClientService();
            LauncherClient client = service.getClient();
            if (client != null && client.equals(mLauncherClient)) {
                service.mWeakReference = null;
                if (!mLauncherClient.getActivity().isChangingConfigurations()) {
                    service.disconnect();
                    if (ClientService.sInstance == service) {
                        ClientService.sInstance = null;
                    }
                }
            }
            Utilities.getPrefs(mLauncher).unregisterOnSharedPreferenceChangeListener(this);
            WallpaperColorInfo.getInstance(mLauncher).removeOnChangeListener(this);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) { }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) { }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) { }

        @Override
        public void onAttachedToWindow() {
            mLauncherClient.onAttachedToWindow();
        }

        @Override
        public void onDetachedFromWindow() {
            if (!mLauncherClient.isDestroyed()) {
                mLauncherClient.getEventInfo().parse(0, "detachedFromWindow", 0.0f);
                mLauncherClient.setParams(null);
            }
        }

        @Override
        public void dump(String prefix, FileDescriptor fd, PrintWriter w, String[] args) { }

        @Override
        public void onHomeIntent(boolean internalStateHandled) {
            mLauncherClient.hideOverlay(mAlreadyOnHome);
        }

        @Override
        public boolean handleBackPressed() {
            return false;
        }

        @Override
        public void onTrimMemory(int level) { }

        @Override
        public void onLauncherProviderChange() { }

        @Override
        public void bindAllApplications(ArrayList<AppInfo> apps) { }

        @Override
        public boolean startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData) {
            View gIcon = mLauncher.findViewById(R.id.g_icon);
            while (gIcon != null && !gIcon.isClickable()) {
                if (gIcon.getParent() instanceof View) {
                    gIcon = (View)gIcon.getParent();
                } else {
                    gIcon = null;
                }
            }
            if (gIcon != null && gIcon.performClick()) {
                return true;
            }
            return false;
        }

        @Override
        public boolean hasSettings() {
            return true;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (SettingsFragment.KEY_MINUS_ONE.equals(key)) {
                ClientOptions clientOptions = new ClientOptions((prefs.getBoolean(SettingsFragment.KEY_MINUS_ONE, true) ? 1 : 0) | 2 | 4 | 8);
                if (clientOptions.options != mLauncherClient.mFlags) {
                    mLauncherClient.mFlags = clientOptions.options;
                    if (mLauncherClient.getParams() != null) {
                        mLauncherClient.updateConfiguration();
                    }
                    mLauncherClient.getEventInfo().parse("setClientOptions ", mLauncherClient.mFlags);
                }
            } else if (SettingsFragment.KEY_APP_SUGGESTIONS.equals(key)) {
                updatePredictions(true);
            }
        }

        @Override
        public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
            int alpha = mLauncher.getResources().getInteger(R.integer.extracted_color_gradient_alpha);
            mUiInformation.putInt("background_color_hint", primaryColor(wallpaperColorInfo, mLauncher, alpha));
            mUiInformation.putInt("background_secondary_color_hint", secondaryColor(wallpaperColorInfo, mLauncher, alpha));
            mUiInformation.putBoolean("is_background_dark", Themes.getAttrBoolean(mLauncher, R.attr.isMainColorDark));
            mLauncherClient.redraw(mUiInformation);
        }

        public void updatePredictions(boolean force) {
            if (hasBeenResumed() || force) {
                List<ComponentKeyMapper> apps = ((PredictionsDispatcher) getUserEventDispatcher()).getPredictedApps();
                if (apps != null) {
                    mAppsView.getFloatingHeaderView().setPredictedApps(mPrefs.getBoolean(SettingsFragment.KEY_APP_SUGGESTIONS, true), apps);
                }
            }
        }
    }

    public static int primaryColor(WallpaperColorInfo wallpaperColorInfo, Context context, int alpha) {
        return compositeAllApps(ColorUtils.setAlphaComponent(wallpaperColorInfo.getMainColor(), alpha), context);
    }

    public static int secondaryColor(WallpaperColorInfo wallpaperColorInfo, Context context, int alpha) {
        return compositeAllApps(ColorUtils.setAlphaComponent(wallpaperColorInfo.getSecondaryColor(), alpha), context);
    }

    public static int compositeAllApps(int color, Context context) {
        return ColorUtils.compositeColors(Themes.getAttrColor(context, R.attr.allAppsScrimColor), color);
    }
}

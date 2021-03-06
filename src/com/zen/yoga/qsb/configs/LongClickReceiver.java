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
package com.zen.yoga.qsb.configs;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.os.Bundle;

import com.zen.yoga.YogaLauncher;
import com.zen.yoga.search.AppSearchProvider;

import com.android.launcher3.compat.LauncherAppsCompat;
import com.android.launcher3.util.ComponentKey;

import java.lang.ref.WeakReference;

public class LongClickReceiver extends BroadcastReceiver {

    public static WeakReference<YogaLauncher> mWeakReference = new WeakReference(null);

    public static void getWeakReference(YogaLauncher launcher) {
        mWeakReference = new WeakReference(launcher);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        YogaLauncher launcher = (YogaLauncher) mWeakReference.get();
        if (launcher != null) {
            ComponentKey searchUri = AppSearchProvider.uriToComponent(intent.getData(), context);
            LauncherActivityInfo resolveActivity = LauncherAppsCompat.getInstance(context)
                .resolveActivity(new Intent("android.intent.action.MAIN")
                .setComponent(searchUri.componentName), searchUri.user);
            if (resolveActivity != null) {
                ItemDragListener onDragListener = new ItemDragListener(resolveActivity, intent.getSourceBounds());
                onDragListener.init(launcher, false);
                launcher.getDragLayer().setOnDragListener(onDragListener);
                ClipData clipData = new ClipData(new ClipDescription("", new String[]{onDragListener.getMimeType()}), new Item(""));
                Bundle bundle = new Bundle();
                bundle.putParcelable("clip_data", clipData);
                setResult(-1, null, bundle);
            }
        }
    }
}

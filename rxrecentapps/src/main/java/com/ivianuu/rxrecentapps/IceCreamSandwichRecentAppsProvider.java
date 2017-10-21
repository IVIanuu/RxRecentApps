/*
 * Copyright 2017 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.rxrecentapps;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Ice cream sandwich implementation of an recent apps provider
 */
final class IceCreamSandwichRecentAppsProvider implements RecentAppsProvider {

    private final ActivityManager activityManager;
    private final PackageManager packageManager;

    private IceCreamSandwichRecentAppsProvider(ActivityManager activityManager,
                                               PackageManager packageManager) {
        this.activityManager = activityManager;
        this.packageManager = packageManager;
    }

    /**
     * Returns a new ice cream sandwich recent apps provider
     */
    @NonNull
    static RecentAppsProvider create(@NonNull Context context) {
        return new IceCreamSandwichRecentAppsProvider(
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE), context.getPackageManager());
    }

    @NonNull
    @Override
    public List<String> getRecentApps(int limit) {
        List<String> recentApps = new ArrayList<>();

        List<ActivityManager.RunningTaskInfo> runningTasks = activityManager.getRunningTasks(limit);

        if (runningTasks != null) {
            try {
                PackageInfo packageInfo;
                for (ActivityManager.RunningTaskInfo taskInfo : runningTasks) {
                    packageInfo = packageManager.getPackageInfo(taskInfo.topActivity.getPackageName(), 0);
                    if (packageInfo != null) {
                        recentApps.add(packageInfo.applicationInfo.packageName);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                // ignore
            }
        }

        return recentApps;
    }
}

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

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lollipop implementation of an recent apps provider
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
final class RecentAppsProviderLollipopImpl implements RecentAppsProvider {

    private final UsageStatsManager usageStatsManager;
    private final PackageManager packageManager;

    private RecentAppsProviderLollipopImpl(UsageStatsManager usageStatsManager,
                                           PackageManager packageManager) {
        this.usageStatsManager = usageStatsManager;
        this.packageManager = packageManager;
    }

    /**
     * Creates a new lollipop recent apps provider
     */
    @NonNull
    static RecentAppsProvider create(@NonNull Context context) {
        return new RecentAppsProviderLollipopImpl(
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE),
                context.getPackageManager());
    }

    @NonNull
    @Override
    public List<String> getRecentApps(int limit) {
        List<String> recentApps = new ArrayList<>();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        List<ResolveInfo> installedApps = packageManager.queryIntentActivities(mainIntent, 0);
        List<String> installedPackages = new ArrayList<>();
        for (ResolveInfo resolveInfo : installedApps)
            installedPackages.add(resolveInfo.activityInfo.packageName);

        long now = System.currentTimeMillis();

        UsageEvents usageEvents = usageStatsManager.queryEvents(now - 1000 * 3600, now);
        UsageEvents.Event event = new UsageEvents.Event();
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event);
            if (event.getEventType() != UsageEvents.Event.MOVE_TO_FOREGROUND) continue;
            if (installedPackages.contains(event.getPackageName())) {
                if (recentApps.contains(event.getPackageName())) {
                    recentApps.remove(event.getPackageName());
                }
                recentApps.add(event.getPackageName());
            }
        }

        Collections.reverse(recentApps);

        if (recentApps.size() > limit) {
            recentApps.subList(limit, recentApps.size()).clear();
        }

        return recentApps;
    }
}

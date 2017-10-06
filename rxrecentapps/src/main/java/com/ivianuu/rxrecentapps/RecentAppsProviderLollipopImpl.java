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
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Lollipop implementation of an recent apps provider
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
final class RecentAppsProviderLollipopImpl implements RecentAppsProvider {

    private static final Comparator<UsageStats> COMPARATOR
            = (o1, o2) -> Long.compare(o2.getLastTimeUsed(), o1.getLastTimeUsed());
    private static final long ONE_HOUR = 1000 * 3600;

    private final UsageStatsManager usageStatsManager;

    private List<UsageStats> stats;

    private RecentAppsProviderLollipopImpl(UsageStatsManager usageStatsManager) {
        this.usageStatsManager = usageStatsManager;
    }

    /**
     * Creates a new lollipop recent apps provider
     */
    @NonNull
    static RecentAppsProvider create(@NonNull Context context) {
        return new RecentAppsProviderLollipopImpl(
                (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE));
    }

    @NonNull
    @Override
    public List<String> getRecentApps(int limit) {
        List<String> recentApps = new ArrayList<>();

        long now = System.currentTimeMillis();

        stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, now - ONE_HOUR, now);
        if (stats != null) {
            Collections.sort(stats, COMPARATOR);

            for (UsageStats usageStats : stats) {
                if (recentApps.size() >= limit) break;
                recentApps.add(usageStats.getPackageName());
            }
        }

        return recentApps;
    }
}

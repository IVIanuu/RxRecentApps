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

package com.ivianuu.rxrecentapps

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.support.annotation.RequiresApi
import java.util.*

/**
 * Provides recent apps
 */
interface RecentAppsProvider {
    /**
     * Returns the recent apps
     */
    fun getRecentApps(limit: Int): List<String>
}

/**
 * Ice cream sandwich implementation of an [RecentAppsProvider]
 */
class IceCreamSandwichRecentAppsProvider private constructor(private val activityManager: ActivityManager,
                                                             private val packageManager: PackageManager
) : RecentAppsProvider {

    override fun getRecentApps(limit: Int): List<String> {
        val recentApps = ArrayList<String>()

        val runningTasks = activityManager.getRunningTasks(limit)

        if (runningTasks != null) {
            try {
                var packageInfo: PackageInfo?
                for (taskInfo in runningTasks) {
                    packageInfo = packageManager.getPackageInfo(taskInfo.topActivity.packageName, 0)
                    if (packageInfo != null) {
                        recentApps.add(packageInfo.applicationInfo.packageName)
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                // ignore
            }

        }

        return recentApps
    }

    companion object {

        /**
         * Returns a new ice cream sandwich recent apps provider
         */
        @JvmStatic
        fun create(context: Context): RecentAppsProvider {
            return IceCreamSandwichRecentAppsProvider(
                    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager,
                    context.packageManager)
        }
    }
}

/**
 * Lollipop implementation of an [RecentAppsProvider]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class LollipopRecentAppsProvider private constructor(private val usageStatsManager: UsageStatsManager,
                                                     packageManager: PackageManager
) : RecentAppsProvider {

    private val installedPackages = ArrayList<String>()

    init {

        // get installed packages
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        val installedApps = packageManager.queryIntentActivities(mainIntent, 0)
        for (resolveInfo in installedApps) {
            installedPackages.add(resolveInfo.activityInfo.packageName)
        }
    }

    override fun getRecentApps(limit: Int): List<String> {
        val recentApps = ArrayList<String>()

        val now = System.currentTimeMillis()

        val usageEvents = usageStatsManager.queryEvents(now - 1000 * 3600, now)
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            // filter some crap out
            if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND || !installedPackages.contains(
                    event.packageName))
                continue
            // remove the older entry if the list already contains the package
            if (recentApps.contains(event.packageName)) {
                recentApps.remove(event.packageName)
            }
            recentApps.add(event.packageName)
        }

        Collections.reverse(recentApps)

        if (recentApps.size > limit) {
            recentApps.subList(limit, recentApps.size).clear()
        }

        return recentApps
    }

    companion object {

        /**
         * Creates a new lollipop recent apps provider
         */
        @JvmStatic
        fun create(context: Context): RecentAppsProvider {
            return LollipopRecentAppsProvider(
                    context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager,
                    context.packageManager)
        }
    }
}

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

    fun getRecentApps(limit: Int): List<String>

}

internal class IceCreamSandwichRecentAppsProvider private constructor(
    private val activityManager: ActivityManager,
    private val packageManager: PackageManager
) : RecentAppsProvider {

    override fun getRecentApps(limit: Int): List<String> {
        return try {
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

            recentApps
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {

        @JvmStatic
        fun create(context: Context): RecentAppsProvider {
            return IceCreamSandwichRecentAppsProvider(
                    context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager,
                    context.packageManager)
        }

    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class LollipopRecentAppsProvider private constructor(
    private val usageStatsManager: UsageStatsManager,
    private val packageManager: PackageManager
) : RecentAppsProvider {

    override fun getRecentApps(limit: Int): List<String> {
        return try {
            // get installed packages
            val mainIntent = Intent(Intent.ACTION_MAIN, null)
            val installedApps = packageManager.queryIntentActivities(mainIntent, 0)
                .map { it.activityInfo.packageName }

            val recentApps = ArrayList<String>()

            val now = System.currentTimeMillis()

            val usageEvents = usageStatsManager.queryEvents(now - 1000 * 3600, now)
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                // filter some crap out
                if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND || !installedApps.contains(
                        event.packageName))
                    continue

                // remove the older entry if the list already contains the package
                if (recentApps.contains(event.packageName)) {
                    recentApps.remove(event.packageName)
                }
                recentApps.add(event.packageName)
            }

            recentApps.reverse()

            if (recentApps.size > limit) {
                recentApps.subList(limit, recentApps.size).clear()
            }

            recentApps
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {

        fun create(context: Context): RecentAppsProvider {
            return LollipopRecentAppsProvider(
                    context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager,
                    context.packageManager)
        }
    }
}

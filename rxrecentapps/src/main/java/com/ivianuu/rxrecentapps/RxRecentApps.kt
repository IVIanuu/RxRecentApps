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

import android.Manifest
import android.content.Context
import android.os.Build
import android.support.annotation.CheckResult
import android.support.annotation.RequiresPermission
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

/**
 * Rx recent apps
 */
class RxRecentApps private constructor(private val recentAppsProvider: RecentAppsProvider) {

    /**
     * Returns the recent apps
     */
    @RequiresPermission(
            allOf = arrayOf(Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS))
    @CheckResult
    fun getRecentApps(limit: Int): Single<List<String>> =
            Single.fromCallable<List<String>> { recentAppsProvider.getRecentApps(limit) }

    /**
     * Returns a [Flowable] which emits recent apps on changes
     */
    @RequiresPermission(
            allOf = arrayOf(Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS))
    @CheckResult
    @JvmOverloads fun observeRecentApps(limit: Int = 10,
                                        period: Long = 1,
                                        timeUnit: TimeUnit = TimeUnit.SECONDS
    ): Flowable<List<String>> =
            Flowable.interval(period, timeUnit)
                    .startWith(0L)
                    .flatMapSingle {  getRecentApps(limit) }
                    .distinctUntilChanged()
                    .onBackpressureLatest()


    /**
     * Maybe returns the current app or the default package
     */
    @RequiresPermission(
            allOf = arrayOf(Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS))
    @CheckResult
    fun getCurrentApp(): Maybe<String> =
            getRecentApps(1)
                    .filter { it.isNotEmpty() }
                    .map { it.first() }

    /**
     * Maybe returns the current app or the default package
     */
    @RequiresPermission(
            allOf = arrayOf(Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS))
    @CheckResult
    fun getCurrentApp(defaultPackage: String): Single<String> =
            getCurrentApp()
                    .defaultIfEmpty(defaultPackage)
                    .toSingle()

    /**
     * Always emits the latest app
     * Checks every second
     */
    @RequiresPermission(
            allOf = arrayOf(Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS))
    @CheckResult
    @JvmOverloads
    fun observeCurrentApp(period: Long = 1,
                          timeUnit: TimeUnit = TimeUnit.SECONDS
    ): Flowable<String> =
            Flowable.interval(period, timeUnit)
                    .subscribeOn(Schedulers.io())
                    .startWith(0L)
                    .flatMapMaybe{ getCurrentApp() }
                    .distinctUntilChanged()
                    .onBackpressureLatest()

    /**
     * Maybe returns the last app
     */
    @RequiresPermission(
            allOf = arrayOf(Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS))
    @CheckResult
    fun getLastApp(): Maybe<String> =
            getRecentApps(2)
                    .filter { recentApps -> recentApps.size >= 2 }
                    .map { recentApps -> recentApps[1] }

    /**
     * Maybe returns the last app or the default package
     */
    @RequiresPermission(
            allOf = arrayOf(Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS))
    @CheckResult
    fun getLastApp(defaultPackage: String): Single<String> {
        return getLastApp()
                .defaultIfEmpty(defaultPackage)
                .toSingle()
    }

    companion object {

        /**
         * Returns a [RxRecentApps] instance
         */
        @JvmStatic
        fun create(context: Context): RxRecentApps {
            val recentAppsProvider: RecentAppsProvider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                recentAppsProvider = LollipopRecentAppsProvider.create(context)
            } else {
                recentAppsProvider = IceCreamSandwichRecentAppsProvider.create(context)
            }
            return create(recentAppsProvider)
        }

        /**
         * Returns a [RxRecentApps] instance
         */
        @JvmStatic
        fun create(recentAppsProvider: RecentAppsProvider): RxRecentApps {
            return RxRecentApps(recentAppsProvider)
        }
    }
}
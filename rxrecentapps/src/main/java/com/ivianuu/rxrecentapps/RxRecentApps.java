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

import android.Manifest;
import android.content.Context;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import static com.ivianuu.rxrecentapps.Preconditions.checkNotNull;

/**
 * Rx recent apps
 */
public final class RxRecentApps {

    private final RecentAppsProvider recentAppsProvider;

    private RxRecentApps(RecentAppsProvider recentAppsProvider) {
        this.recentAppsProvider = recentAppsProvider;
    }

    /**
     * Returns a new rx recent apps instance
     */
    @NonNull
    public static RxRecentApps create(@NonNull Context context) {
        checkNotNull(context, "context == null");
        RecentAppsProvider recentAppsProvider;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            recentAppsProvider = LollipopRecentAppsProvider.create(context);
        } else {
            recentAppsProvider = IceCreamSandwichRecentAppsProvider.create(context);
        }
        return new RxRecentApps(recentAppsProvider);
    }

    /**
     * Returns the 10 recent apps
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Single<List<String>> getRecentApps() {
        return getRecentApps(10);
    }

    /**
     * Returns the recent apps
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Single<List<String>> getRecentApps(int limit) {
        return Single.fromCallable(() -> recentAppsProvider.getRecentApps(limit))
                .subscribeOn(Schedulers.io());
    }

    /**
     * Observes the recent apps and returns the last 10 on changes
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Flowable<List<String>> observeRecentApps() {
        return observeRecentApps(10, 1, TimeUnit.SECONDS);
    }

    /**
     * Observes the recent apps and returns them on changes
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Flowable<List<String>> observeRecentApps(int limit) {
        return observeRecentApps(limit, 1, TimeUnit.SECONDS);
    }

    /**
     * Observes the recent apps and emits on changes
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Flowable<List<String>> observeRecentApps(int limit,
                                                    long period,
                                                    @NonNull TimeUnit timeUnit) {
        checkNotNull(timeUnit, "timeUnit == null");
        return Flowable.interval(period, timeUnit)
                .subscribeOn(Schedulers.io())
                .startWith(0L)
                .flatMapSingle(__ -> getRecentApps(limit))
                .distinctUntilChanged()
                .onBackpressureLatest();
    }

    /**
     * Maybe returns the current app
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Maybe<String> getCurrentApp() {
        return getRecentApps(1)
                .filter(recentApps -> !recentApps.isEmpty())
                .map(recentApps -> recentApps.get(0));
    }

    /**
     * Maybe returns the current app or the default package
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Single<String> getCurrentApp(@NonNull String defaultPackage) {
        checkNotNull(defaultPackage, "defaultPackage == null");
        return getCurrentApp()
                .defaultIfEmpty(defaultPackage)
                .toSingle();
    }

    /**
     * Always emits the latest app
     * Checks every second
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Flowable<String> observeCurrentApp() {
        return observeCurrentApp(1, TimeUnit.SECONDS);
    }

    /**
     * Always emits the latest app
     * Checks every second
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Flowable<String> observeCurrentApp(long period,
                                              @NonNull TimeUnit timeUnit) {
        checkNotNull(timeUnit, "timeUnit == null");
        return Flowable.interval(period, timeUnit)
                .subscribeOn(Schedulers.io())
                .startWith(0L)
                .flatMapMaybe(__ -> getCurrentApp())
                .distinctUntilChanged()
                .onBackpressureLatest();
    }

    /**
     * Maybe returns the last app
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Maybe<String> getLastApp() {
        return getRecentApps(2)
                .filter(recentApps -> recentApps.size() >= 2)
                .map(recentApps -> recentApps.get(1));
    }

    /**
     * Maybe returns the last app or the default package
     */
    @RequiresPermission(allOf = {Manifest.permission.GET_TASKS, Manifest.permission.PACKAGE_USAGE_STATS})
    @CheckResult
    @NonNull
    public Single<String> getLastApp(@NonNull String defaultPackage) {
        checkNotNull(defaultPackage, "defaultPackage == null");
        return getLastApp()
                .defaultIfEmpty(defaultPackage)
                .toSingle();
    }
}

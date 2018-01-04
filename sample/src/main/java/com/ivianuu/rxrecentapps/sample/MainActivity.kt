package com.ivianuu.rxrecentapps.sample

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.ivianuu.rxrecentapps.RxRecentApps
import io.reactivex.disposables.CompositeDisposable

class MainActivity : AppCompatActivity() {

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rxRecentApps = RxRecentApps.create(this)

        compositeDisposable.add(
                rxRecentApps.observeRecentApps(10)
                        .map { strings ->
                            removePackages(strings.toMutableList())
                            strings
                        }
                        .distinctUntilChanged()
                        .subscribe { s -> Log.d("testtt", s.toString()) })
    }

    private fun removePackages(packages: MutableList<String>) {
        if (packages.contains("com.android.systemui")) {
            packages.remove("com.android.systemui")
        }
        var homeApp = ""
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo.activityInfo.packageName != null) {
            homeApp = resolveInfo.activityInfo.packageName
        }
        if (packages.contains(homeApp)) {
            packages.remove(homeApp)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}

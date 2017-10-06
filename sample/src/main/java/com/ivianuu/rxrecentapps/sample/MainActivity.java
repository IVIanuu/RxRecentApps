package com.ivianuu.rxrecentapps.sample;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.ivianuu.rxrecentapps.RxRecentApps;

import java.util.List;

import io.reactivex.disposables.CompositeDisposable;

public class MainActivity extends AppCompatActivity {

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RxRecentApps rxRecentApps = RxRecentApps.create(this);

        compositeDisposable.add(
                rxRecentApps.observeRecentApps(10)
                        .map(strings -> {
                            removePackages(strings);
                            return strings;
                        })
                        .distinctUntilChanged()
                        .subscribe(s -> Log.d("testtt", s.toString())));
    }

    private void removePackages(List<String> packages) {
        if (packages.contains("com.android.systemui")) {
            packages.remove("com.android.systemui");
        }
        String homeApp = "";
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.activityInfo.packageName != null) {
            homeApp = resolveInfo.activityInfo.packageName;
        }
        if (packages.contains(homeApp)) {
            packages.remove(homeApp);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}

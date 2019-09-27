package com.maning.hotfixdemo;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import com.maning.hotfix.HotFixManager;

/**
 * @author : maning
 * @desc :
 */
public class DemoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //初始化
        HotFixManager.init(this);
        //hotfix
        String patchPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/patch.jar";
        System.out.println(">>>>>>installPatch-patchPath:" + patchPath);
        HotFixManager.installPatch(this, patchPath);
    }
}

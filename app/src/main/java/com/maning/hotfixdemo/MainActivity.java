package com.maning.hotfixdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.maning.hotfixdemo.utils.TestFix;
import com.maning.testjar.TestClass;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPerm();
    }

    public void requestPerm() {
        //判断权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10010);
            }
        }
    }

    public void testClass(View view) {
        TestFix.test(this);
    }

    public void testJar(View view) {
        String test = TestClass.test();
        Toast.makeText(this, test, Toast.LENGTH_SHORT).show();
    }
}

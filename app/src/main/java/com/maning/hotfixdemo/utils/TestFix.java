package com.maning.hotfixdemo.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * @author : maning
 * @desc :
 */
public class TestFix {

    public static void test(Context context) {
//        Toast.makeText(context, ">>>>>>>>>> bug >>>>>>>>>> ", Toast.LENGTH_SHORT).show();
        Toast.makeText(context, ">>>>>>>>>> bug fix fix fix >>>>>>>>>>", Toast.LENGTH_SHORT).show();
    }

}

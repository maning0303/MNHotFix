package com.maning.hotfix.plugin;

/**
 * @author : maning
 * @desc : 工具类
 */
public class Utils {

    public static String capitalize(String self) {
        return self.length() == 0 ? "" : "" + Character.toUpperCase(self.charAt(0)) + self.subSequence(1, self.length());
    }

    public static boolean isAndroidClass(String filePath) {
        return filePath.startsWith("android") || filePath.startsWith("androidx");
    }

}

package com.maning.hotfix.plugin;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : maning
 * @desc : 工具类
 */
public class Utils {

    public static boolean isNoProcessClass(String filePath) {
        return
                filePath.startsWith("android/support")
                        || filePath.startsWith("android")
                        || filePath.startsWith("androidx")
                        || filePath.contains("/R$")
                        || filePath.endsWith("/R.class")
                        || filePath.startsWith("com/maning/hotfix/")
                        || filePath.endsWith("/BuildConfig.class");
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isEmpty(Map map) {
        return map == null || map.isEmpty();
    }


    public static String hex(byte[] byteCode) {
        try {
            return DigestUtils.md5Hex(byteCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Map<String, String> readHex(File hexFile) {
        Map<String, String> hashMap = new HashMap<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(hexFile)));
            String line;
            while ((line = br.readLine()) != null) {
                String[] list = line.split(":");
                if (list.length == 2) {
                    hashMap.put(list[0], list[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return hashMap;
    }

    public static void writeHex(Map<String, String> hexs, File hexFile) {
        try {
            FileOutputStream os = new FileOutputStream(hexFile);
            for (String key : hexs.keySet()) {
                String value = hexs.get(key);
                String line = key + ":" + value + "\n";
                os.write(line.getBytes());
            }
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

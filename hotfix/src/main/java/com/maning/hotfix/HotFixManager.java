package com.maning.hotfix;

import android.content.Context;
import android.os.Build;

import com.maning.hotfix.utils.AssetsUtils;
import com.maning.hotfix.utils.ReflectUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : maning
 * @desc : 热修复工具
 * java -jar dx.jar --dex --output=xxx(生成的文件地址) /Users/maning/Downloads/hotfix_jar(内部包含包名下的.class)
 */
public class HotFixManager {

    public static void init(Context context) {
        //加入一个hack.dex ：插桩->防止类被打上标签
        File hackFile = getHackFile(context);
        if (hackFile != null && hackFile.exists()) {
            installPatch(context, hackFile.getAbsolutePath());
        }
    }

    private static File getHackFile(Context context) {
        try {
            File hackDir = context.getDir("hack", Context.MODE_PRIVATE);
            File hackFile = new File(hackDir, "hack.jar");
            AssetsUtils.copyAssets(context, "hotfix_hack.jar", hackFile.getAbsolutePath());
            return new File(hackFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 修复
     *
     * @param context   上下文
     * @param patchPath 文件路径
     */
    public static void installPatch(Context context, String patchPath) {
        //PathClassLoader
        ClassLoader classLoader = context.getClassLoader();
        try {
            //反射获取BaseDexClassLoad属性：DexPathList pathList
            Field pathListField = ReflectUtils.getField(classLoader, "pathList");
            //DexPathList
            Object pathListObj = pathListField.get(classLoader);

            //反射获取 DexPathList 的属性：dexElements
            Field dexElementsField = ReflectUtils.getField(pathListObj, "dexElements");
            Object[] oldDexElements = (Object[]) dexElementsField.get(pathListObj);

            //TODO:兼容问题补丁包路径转换为dexElements,利用DexPathList里面的静态方法
            //4.0.3-15      :private static Element[] makeDexElements(ArrayList<File> files,File optimizedDirectory)
            //4.1-16        :private static Element[] makeDexElements(ArrayList<File> files,File optimizedDirectory)
            //4.2-17        :private static Element[] makeDexElements(ArrayList<File> files,File optimizedDirectory)
            //4.3-18        :private static Element[] makeDexElements(ArrayList<File> files,File optimizedDirectory)
            //4.4-19        :private static Element[] makeDexElements(ArrayList<File> files, File optimizedDirectory, ArrayList<IOException> suppressedExceptions)
            //5.0-21        :private static Element[] makeDexElements(ArrayList<File> files, File optimizedDirectory,ArrayList<IOException> suppressedExceptions)
            //6.0-23        :private static Element[] makePathElements(List<File> files, File optimizedDirectory,List<IOException> suppressedExceptions)
            //7.0-24        :private static Element[] makePathElements(List<File> files, File optimizedDirectory, List<IOException> suppressedExceptions)
            //8.0-25        :private static Element[] makePathElements(List<File> files, File optimizedDirectory,List<IOException> suppressedExceptions)
            //9.0-26        :private static Element[] makePathElements(List<File> files, File optimizedDirectory,List<IOException> suppressedExceptions)
            Object[] newDexElements;
            //参数：dex路径集合
            ArrayList<File> dexFiels = new ArrayList<>();
            File patchFile = new File(patchPath);
            if (patchFile.exists()) {
                dexFiels.add(patchFile);
            }
//            //加入一个hack.dex ：插桩->防止类被打上标签
//            File hackFile = initHack(context);
//            if (hackFile != null && hackFile.exists()) {
//                dexFiels.add(hackFile);
//            }
            //参数：dex优化目录-odex保存地址
            File optimizedDirectory = context.getCacheDir();
            //参数：空集合
            ArrayList<IOException> superessedException = new ArrayList<>();

            //版本适配
            if (Build.VERSION.SDK_INT >= 23) {
                Method makePathElementsMethod = ReflectUtils.getMethed(pathListObj, "makePathElements", List.class, File.class, List.class);
                newDexElements = (Object[]) makePathElementsMethod.invoke(null, dexFiels, optimizedDirectory, superessedException);
            } else if (Build.VERSION.SDK_INT >= 19) {
                Method makeDexElementsMethod = ReflectUtils.getMethed(pathListObj, "makeDexElements", ArrayList.class, File.class, ArrayList.class);
                newDexElements = (Object[]) makeDexElementsMethod.invoke(null, dexFiels, optimizedDirectory, superessedException);
            } else {
                Method makeDexElementsMethod = ReflectUtils.getMethed(pathListObj, "makeDexElements", ArrayList.class, File.class);
                newDexElements = (Object[]) makeDexElementsMethod.invoke(null, dexFiels, optimizedDirectory);
            }

            //合并dex
            Object[] replaceDexElements = (Object[]) Array.newInstance(
                    oldDexElements.getClass().getComponentType()
                    , oldDexElements.length + newDexElements.length);
            System.arraycopy(newDexElements, 0, replaceDexElements, 0, newDexElements.length);
            System.arraycopy(oldDexElements, 0, replaceDexElements, newDexElements.length, oldDexElements.length);

            //替换原来的dex集合
            dexElementsField.set(pathListObj, replaceDexElements);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(">>>>>install patch exception>>>>>:" + e.toString());
        }
    }
}

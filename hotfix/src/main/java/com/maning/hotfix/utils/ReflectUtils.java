package com.maning.hotfix.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author : maning
 * @desc :  反射工具类
 */
public class ReflectUtils {

    /**
     * 反射获取属性
     *
     * @param instance PathClassLoad
     * @param name
     * @return
     */
    public static Field getField(Object instance, String name) {
        for (Class<?> cls = instance.getClass(); cls != null; cls = cls.getSuperclass()) {
            try {
                Field declaredField = cls.getDeclaredField(name);
                //设置权限
                declaredField.setAccessible(true);
                return declaredField;
            } catch (NoSuchFieldException e) {
            }
        }
        return null;
    }

    /**
     * 反射获取一个方法
     *
     * @param instance
     * @param name
     * @return
     */
    public static Method getMethed(Object instance, String name, Class<?>... parameterType) {
        for (Class<?> cls = instance.getClass(); cls != null; cls = cls.getSuperclass()) {
            try {
                Method declaredMethod = cls.getDeclaredMethod(name, parameterType);
                //设置权限
                declaredMethod.setAccessible(true);
                return declaredMethod;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}

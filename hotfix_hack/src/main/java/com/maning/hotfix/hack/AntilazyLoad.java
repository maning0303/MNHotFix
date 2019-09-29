package com.maning.hotfix.hack;

/**
 * 空实现，主要是单独打一个dex包，防止类被打上CLASS_ISPREVERIFIED 标记5.0以下会异常
 * 生成jar包后一定要用dex工具转换
 */
public class AntilazyLoad {
}

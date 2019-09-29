package com.maning.hotfix.plugin;


import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.build.gradle.internal.pipeline.TransformTask;
import com.android.build.gradle.internal.transforms.ProGuardTransform;
import com.android.utils.FileUtils;

import org.apache.commons.compress.utils.IOUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskOutputs;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;

public class HotFixPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getLogger().error(">>>>>>HotFixPlugin>>>>>>");

        //插件只能在android application 下面使用
        if (!project.getPlugins().hasPlugin(AppPlugin.class)) {
            throw new GradleException("无法在非android application插件中使用热修复插件");
        }
        //patch配置参数
        //就和引入了 apply plugin: 'com.android.application' 一样，可以配置android{}
        project.getExtensions().create("patch", PatchExtension.class);

        //gradle执行会解析build.gradle文件，afterEvaluate表示在解析完成之后再执行我们的代码
        project.afterEvaluate(new Action<Project>() {
            @Override
            public void execute(Project project) {
                //获取patch{}配置文件
                PatchExtension patchExtension = project.getExtensions().findByType(PatchExtension.class);
                if (patchExtension == null) {
                    throw new GradleException("HotFix patch{}配置不能为空");
                }
                project.getLogger().error(">>>>>>patchExtension:" + patchExtension.toString());
                //获取android{}配置
                AppExtension android = project.getExtensions().findByType(AppExtension.class);
                if (android == null) {
                    project.getLogger().error(">>>>>>AppExtension android==null>>>>>>");
                    return;
                }
                //getApplicationVariants就是包含了debug和release的集合，all表示对集合进行遍历
                android.getApplicationVariants().all(new Action<ApplicationVariant>() {
                    @Override
                    public void execute(@NotNull ApplicationVariant applicationVariant) {
                        //当前用户是debug模式
                        if (patchExtension.debugOn) {
                            project.getLogger().error(">>>>>>debug模式>>>>>>over");
                            return;
                        }
                        //配置热修复插件生成补丁的一系列任务
                        configTasks(project, applicationVariant, patchExtension);
                    }
                });

            }
        });
    }

    private void configTasks(Project project, ApplicationVariant variant, PatchExtension patchExtension) {
        //debug-release
        String variantName = variant.getName();

        //热修复的输出目录
        File outputDir;
        //如果没有指名输出目录，默认输出到 build/patch/debug(release) 下
        if (!Utils.isEmpty(patchExtension.output)) {
            outputDir = new File(patchExtension.output, variantName);
        } else {
            outputDir = new File(project.getBuildDir(), "patch/" + variantName);
        }
        outputDir.mkdirs();
        
        Task dexTask = null;
        Task proguardTask = null;
        if ("debug".equals(variantName)) {
            dexTask = project.getTasks().findByName("transformClassesWithDexForDebug");
            proguardTask = project.getTasks().findByName("transformClassesAndResourcesWithProguardForDebug");
        } else if ("release".equals(variantName)) {
            dexTask = project.getTasks().findByName("transformClassesWithDexForRelease");
            proguardTask = project.getTasks().findByName("transformClassesAndResourcesWithProguardForRelease");
        }

        //获得android的混淆任务
        handlerProguardTask(project, outputDir, proguardTask);

        //在混淆后 记录类的hash值，并生成补丁包
        final File hexFile = new File(outputDir, "hex.txt");
        // 需要打包补丁的类的jar包
        final File patchClassFile = new File(outputDir, "patchClass.jar");
        // 用dx打包后的jar包
        final File patchFile = new File(outputDir, "patch.jar");
        //插桩 记录md5并对比
        PatchGenerator patchGenerator = new PatchGenerator(project, patchFile, patchClassFile, hexFile);
        //记录类的md5
        Map<String, String> newHexs = new HashMap<>();


        //把class打包成dex任务
        handlerDexTask(project, variant, patchExtension, hexFile, patchGenerator, newHexs, dexTask);
    }

    private void handlerProguardTask(Project project, File outputDir, Task proguardTask) {
        //备份本次的mapping文件
        final File mappingBak = new File(outputDir, "mapping.txt");
        //如果没开启混淆，则为null，不需要备份mapping
        if (proguardTask != null) {
            // 在混淆后备份mapping
            proguardTask.doLast(new Action<Task>() {
                @Override
                public void execute(Task task) {
                    //混淆任务输出的所有文件
                    TaskOutputs outputs = proguardTask.getOutputs();
                    Set<File> files = outputs.getFiles().getFiles();
                    for (File file : files) {
                        //把mapping文件备份
                        if (file.getName().endsWith("mapping.txt")) {
                            try {
                                FileUtils.copyFile(file, mappingBak);
                                project.getLogger().error("备份混淆mapping文件:" + mappingBak.getCanonicalPath());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            });

            //将上次混淆的mapping应用到本次,如果没有上次的混淆文件就没操作
            //上一次混淆的mapping文件存在并且 也开启了混淆任务
            if (mappingBak.exists()) {
                //将上次混淆的mapping应用到本次
                TransformTask task = (TransformTask) proguardTask;
                ProGuardTransform transform = (ProGuardTransform) task.getTransform();
                transform.applyTestedMapping(mappingBak);
            }

        } else {
            project.getLogger().error(">>>>>>proguardTask == null>>>>>>");
        }
    }

    private void handlerDexTask(Project project, ApplicationVariant variant, PatchExtension patchExtension, File hexFile, PatchGenerator patchGenerator, Map<String, String> newHexs, Task dexTask) {
        //debug-release
        String variantName = variant.getName();
        String dirName = variant.getDirName();
        //用户配置的application，实际上可以解析manifest自动获取，但是java实现太麻烦了，干脆让用户自己配置
        final String[] applicationName = {patchExtension.applicationName};

        if (dexTask != null) {
            //打包之前执行插桩
            dexTask.doFirst(new Action<Task>() {
                @Override
                public void execute(Task task) {
                    project.getLogger().error(">>>>>>dexTask.doFirst start>>>>>>");
                    //打包过滤文件
                    Set<File> files = dexTask.getInputs().getFiles().getFiles();
                    for (File file : files) {
                        //windows下 目录输出是  xx\xx\  ,linux下是  /xx/xx ,把 . 替换成平台相关的斜杠
                        applicationName[0] = applicationName[0].replaceAll("\\.", Matcher.quoteReplacement(File.separator));
                        String filePath = file.getAbsolutePath();
                        //插桩，防止类被打上标签
                        if (filePath.endsWith(".jar")) {
                            processJar(applicationName[0], file, newHexs, patchGenerator);
                        } else if (filePath.endsWith(".class")) {
                            processClass(applicationName[0], dirName, file, newHexs, patchGenerator);
                        }
                    }
                    project.getLogger().error(">>>>>>dexTask.doFirst end>>>>>>");
                    //类的md5集合 写入到文件
                    Utils.writeHex(newHexs, hexFile);
                    try {
                        //生成补丁
                        patchGenerator.generate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            project.getLogger().error(">>>>>>dexTask == null>>>>>>");
        }
    }


    private static void processJar(String applicationName, File file, Map<String, String> hexs, PatchGenerator patchGenerator) {
        try {
            //无论是windows还是linux jar包都是 /
            applicationName = applicationName.replaceAll(Matcher.quoteReplacement(File.separator), "/");

            //jar包解析
            File bakJar = new File(file.getParent(), file.getName() + ".bak");
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(bakJar));
            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();

                jarOutputStream.putNextEntry(new JarEntry(jarEntry.getName()));
                InputStream is = jarFile.getInputStream(jarEntry);

                String className = jarEntry.getName();
                if (className.endsWith(".class")
                        && !className.startsWith(applicationName)
                        && !Utils.isAndroidClass(className)
                        && !className.startsWith("com/maning/hotfix")) {
                    //插桩处理
                    byte[] byteCode = ClassUtils.referHackWhenInit(is);
                    //生成MD5值
                    String hex = Utils.hex(byteCode);
                    hexs.put(className, hex);
                    System.out.println(">>>>>>processJar-className：" + className + ",hex:" + hex);
                    is.close();
                    jarOutputStream.write(byteCode);
                    //对比缓存的md5，不一致则放入补丁
                    patchGenerator.checkClass(className, hex, byteCode);
                } else {
                    //输出到临时文件
                    jarOutputStream.write(IOUtils.toByteArray(is));
                }
                jarOutputStream.closeEntry();
            }
            jarOutputStream.close();
            jarFile.close();
            file.delete();
            bakJar.renameTo(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processClass(String applicationName, String dirName, File file, Map<String, String> hexs, PatchGenerator patchGenerator) {
        String filePath = file.getAbsolutePath();
        //注意这里的filePath 目录结构
        String className = filePath;
        if (filePath.contains("classes")) {
            className = className.split("classes")[1].substring(1);
        }
        if (className.startsWith(dirName)) {
            className = className.split(dirName)[1].substring(1);
        }
        //application或者android support我们不管
        if (className.startsWith(applicationName) || Utils.isAndroidClass(className)) {
            return;
        }
        //开始插桩-插入代码
        try {
            FileInputStream is = new FileInputStream(filePath);
            //执行插桩
            byte[] byteCode = ClassUtils.referHackWhenInit(is);
            //生成MD5值
            String hex = Utils.hex(byteCode);
            hexs.put(filePath, hex);
            System.out.println(">>>>>>processClass-className：" + className + ",hex:" + hex);
            is.close();

            FileOutputStream os = new FileOutputStream(filePath);
            os.write(byteCode);
            os.close();

            //对比缓存的md5，不一致则放入补丁
            patchGenerator.checkClass(filePath, hex, byteCode);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

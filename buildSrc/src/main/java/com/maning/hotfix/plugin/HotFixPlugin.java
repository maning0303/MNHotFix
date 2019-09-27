package com.maning.hotfix.plugin;


import com.android.build.gradle.AppExtension;
import com.android.build.gradle.AppPlugin;
import com.android.build.gradle.api.ApplicationVariant;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Set;
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
        //首字母大写
        String capitalizeName = Utils.capitalize(variantName);
        //把class打包成dex任务
        Task dexTask = project.getTasks().findByName("transformClassesWithDexBuilderFor" + capitalizeName);
        //打包之前执行插桩
        dexTask.doFirst(new Action<Task>() {
            @Override
            public void execute(Task task) {
                project.getLogger().error(">>>>>>dexTask.doFirst start>>>>>>");
                Set<File> files = dexTask.getInputs().getFiles().getFiles();
                for (File file : files) {
                    project.getLogger().error(">>>>>>file:" + file.getAbsolutePath());
                    //用户配置的application，实际上可以解析manifest自动获取，但是java实现太麻烦了，干脆让用户自己配置
                    String applicationName = patchExtension.applicationName;
                    //windows下 目录输出是  xx\xx\  ,linux下是  /xx/xx ,把 . 替换成平台相关的斜杠
                    applicationName = applicationName.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
                    String filePath = file.getAbsolutePath();
                    //插桩，防止类被打上标签
                    if (filePath.endsWith(".jar")) {
                        processJar(applicationName, variant.getDirName(), file);
                    } else if (filePath.endsWith(".class")) {
                        processClass(applicationName, variant.getDirName(), file);
                    }
                }
                project.getLogger().error(">>>>>>dexTask.doFirst end>>>>>>");
            }
        });
    }

    private static void processJar(String applicationName, String dirName, File file) {

    }

    private static void processClass(String applicationName, String dirName, File file) {
        String filePath = file.getAbsolutePath();
        //注意这里的filePath 目录结构
        String className = filePath;
        if (filePath.contains("classes")) {
            className = className.split("classes")[1].substring(1);
        }
        if (className.startsWith(dirName)) {
            className = className.split(dirName)[1].substring(1);
        }
        System.out.println(">>>>>>className：" + className);
        //application或者android support我们不管
        if (className.startsWith(applicationName) || Utils.isAndroidClass(className)) {
            System.out.println(">>>>>>android support 或者 Application >>>>>>> 跳过");
            return;
        }
        //开始插桩-插入代码
        try {
            FileInputStream is = new FileInputStream(filePath);
            //执行插桩
            byte[] byteCode = ClassUtils.referHackWhenInit(is);
            is.close();

            FileOutputStream os = new FileOutputStream(filePath);
            os.write(byteCode);
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

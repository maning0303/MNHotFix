package com.maning.hotfix.plugin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;


/**
 * 插桩代码
 */
public class ClassUtils {

    /**
     * 差需要插入代码的全路径
     */
    private final static String HACK_CLASS_PATH = "Lcom/maning/hotfix/hack/AntilazyLoad;";
    private final static String NAME_INIT = "<init>";


    public static byte[] referHackWhenInit(InputStream inputStream) throws IOException {
        ClassReader cr = new ClassReader(inputStream);
        ClassWriter cw = new ClassWriter(cr, 0);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
                methodVisitor = new MethodVisitor(api, methodVisitor) {
                    @Override
                    public void visitInsn(int opcode) {
                        //在构造方法中插入AntilazyLoad引用
                        if (NAME_INIT.equals(name) && opcode == Opcodes.RETURN) {
                            super.visitLdcInsn(Type.getType(HACK_CLASS_PATH));
                        }
                        super.visitInsn(opcode);
                    }
                };
                return methodVisitor;
            }
        };
        cr.accept(cv, 0);
        return cw.toByteArray();
    }
}

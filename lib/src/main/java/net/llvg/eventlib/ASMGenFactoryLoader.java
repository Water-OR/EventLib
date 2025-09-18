package net.llvg.eventlib;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

final class ASMGenFactoryLoader
  extends AbstractFactoryLoader
{
    private static final MethodHandles.Lookup defaultLookup = MethodHandles.lookup();
    
    private static final String classObjectName = Type.getInternalName(Object.class);
    private static final String classObjectDesc = Type.getDescriptor(Object.class);
    
    private static final String classUncheckedListenerName = Type.getInternalName(UncheckedListener.class);
    private static final String classUncheckedListenerSAMName;
    private static final String classUncheckedListenerSAMDesc;
    
    static {
        final Method m = Utility.findSAM(UncheckedListener.class);
        classUncheckedListenerSAMName = m.getName();
        classUncheckedListenerSAMDesc = Type.getMethodDescriptor(m);
    }
    
    private static final String classObjectsName = Type.getInternalName(Objects.class);
    private static final String classObjects$requireNonNullName;
    private static final String classObjects$requireNonNullDesc;
    
    static {
        final Method m;
        try {
            m = Objects.class.getMethod("requireNonNull", Object.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("Failed to find requireNonNull", e);
        }
        
        assert Modifier.isStatic(m.getModifiers());
        assert !m.getDeclaringClass().isInterface();
        
        classObjects$requireNonNullName = m.getName();
        classObjects$requireNonNullDesc = Type.getMethodDescriptor(m);
    }
    
    private static final String classGenned$delegateeName = "delegatee";
    private final MethodHandles.Lookup lookup;
    
    private final String prefix;
    
    {
        final String value = getClass().getName();
        
        prefix = String.format(
          "%sASMGen%08x",
          value.substring(0, value.lastIndexOf('.') + 1),
          System.identityHashCode(this)
        );
    }
    
    private final Definer definer;
    
    ASMGenFactoryLoader(final MethodHandles.Lookup lookup) {
        this.lookup = lookup;
        this.definer = new Definer(lookup.lookupClass().getClassLoader());
    }
    
    ASMGenFactoryLoader() {
        this(defaultLookup);
    }
    
    private String buildName(final Method method) {
        return String.format("%s_%08x", prefix, System.identityHashCode(method));
    }
    
    @Override
    protected MethodHandle buildStatic(final Method method) {
        final String classGennedPath = buildName(method);
        final String classGennedName = classGennedPath.replace('.', '/');
        
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource("GenerateClass", null);
        { // visit class
            cw.visit(
              V1_8,
              ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
              classGennedName,
              null,
              classObjectName,
              new String[] { classUncheckedListenerName }
            );
        }
        { // visit constructor
            final MethodVisitor mv = cw.visitMethod(
              ACC_PUBLIC,
              "<init>",
              "()V",
              null,
              null
            );
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(
              INVOKESPECIAL,
              classObjectName,
              "<init>",
              "()V",
              false
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        { // visit Consumer SAM
            final MethodVisitor mv = cw.visitMethod(
              ACC_PUBLIC | ACC_FINAL,
              classUncheckedListenerSAMName,
              classUncheckedListenerSAMDesc,
              null,
              null
            );
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(method.getParameterTypes()[0]));
            mv.visitMethodInsn(
              INVOKESTATIC,
              Type.getInternalName(method.getDeclaringClass()),
              method.getName(),
              Type.getMethodDescriptor(method),
              method.getDeclaringClass().isInterface()
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        
        final Class<?> classGenned = definer.define(classGennedPath, cw.toByteArray());
        
        final Constructor<?> classGennedConstructor;
        try {
            classGennedConstructor = classGenned.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to find constructor", e);
        }
        
        return deflectConstructor(classGennedConstructor);
    }
    
    @Override
    protected MethodHandle buildObject(final Method method) {
        final String classGennedPath = buildName(method);
        final String classGennedName = classGennedPath.replace('.', '/');
        
        final Class<?> classGenned$delegateeType = method.getDeclaringClass();
        final String classGenned$delegateeDesc = Type.getDescriptor(classGenned$delegateeType);
        
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource("GenerateClass", null);
        { // visit class
            cw.visit(
              V1_8,
              ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
              classGennedName,
              null,
              classObjectName,
              new String[] { classUncheckedListenerName }
            );
        }
        { // visit delegatee
            cw.visitField(
              ACC_PRIVATE | ACC_FINAL,
              classGenned$delegateeName,
              classGenned$delegateeDesc,
              null,
              null
            ).visitEnd();
        }
        { // visit constructor
            final MethodVisitor mv = cw.visitMethod(
              ACC_PUBLIC,
              "<init>",
              "(" + classObjectDesc + ")V",
              null,
              null
            );
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(
              INVOKESPECIAL,
              classObjectName,
              "<init>",
              "()V",
              false
            );
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn("[" + classGenned$delegateeName + "] must not be null");
            mv.visitMethodInsn(
              INVOKESTATIC,
              classObjectsName,
              classObjects$requireNonNullName,
              classObjects$requireNonNullDesc,
              false
            );
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(classGenned$delegateeType));
            mv.visitFieldInsn(
              PUTFIELD,
              classGennedName,
              classGenned$delegateeName,
              classGenned$delegateeDesc
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        { // visit Consumer SAM
            final MethodVisitor mv = cw.visitMethod(
              ACC_PUBLIC | ACC_FINAL,
              classUncheckedListenerSAMName,
              classUncheckedListenerSAMDesc,
              null,
              null
            );
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(
              GETFIELD,
              classGennedName,
              classGenned$delegateeName,
              classGenned$delegateeDesc
            );
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(method.getParameterTypes()[0]));
            final boolean isInterface = classGenned$delegateeType.isInterface();
            mv.visitMethodInsn(
              isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL,
              Type.getInternalName(classGenned$delegateeType),
              method.getName(),
              Type.getMethodDescriptor(method),
              isInterface
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        
        final Class<?> classGenned = definer.define(classGennedPath, cw.toByteArray());
        
        final Constructor<?> classGennedConstructor;
        try {
            classGennedConstructor = classGenned.getConstructor(Object.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to find constructor", e);
        }
        
        return deflectConstructor(classGennedConstructor);
    }
    
    private MethodHandle deflectConstructor(final Constructor<?> constructor) {
        constructor.setAccessible(true);
        try {
            return lookup.unreflectConstructor(constructor);
        } catch (IllegalAccessException e) {
            throw new AssertionError("[constructor] should be accessible", e);
        }
    }
    
    private static final class Definer
      extends ClassLoader
    {
        Definer(final ClassLoader parent) {
            super(parent);
        }
        
        private static final @Nullable File root;
        
        static {
            root = Optional.ofNullable(Configuration.asmGenExportLocation.get())
              .map(File::new)
              .orElse(null);
            
            if (root != null) {
                @SuppressWarnings ("UnstableApiUsage")
                final Iterable<File> files = Files.fileTraverser().depthFirstPostOrder(root);
                for (final File it : files) {
                    Preconditions.checkState(
                      !it.exists() || it.delete(),
                      "Failed to delete file '%s'",
                      it.getAbsolutePath()
                    );
                }
            }
        }
        
        Class<?> define(final String name, byte[] bytes) {
            if (root != null) {
                final File file = new File(root, name.replace('.', '/') + ".class");
                try {
                    Preconditions.checkState(
                      (file.getParentFile().exists() || file.getParentFile().mkdirs()) && file.createNewFile(),
                      "Failed to create file '%s'", file.getAbsolutePath()
                    );
                    Files.write(bytes, file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}

package net.neoforged.bus;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import java.util.Arrays;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventListener;

import static org.objectweb.asm.Opcodes.*;

public class ClassLoaderFactory implements IEventListenerFactory {
    private static final String HANDLER_DESC = Type.getInternalName(IEventListener.class);
    private static final String HANDLER_FUNC_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Event.class));
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final LockHelper<Method, Class<?>> cache = LockHelper.withHashMap();

    private static final ConstantDynamic METHOD_CONSTANT = new ConstantDynamic(ConstantDescs.DEFAULT_NAME, MethodHandle.class.descriptorString(), new Handle(
        H_INVOKESTATIC, Type.getInternalName(MethodHandles.class), "classData",
        MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class).descriptorString(), false
    ));

    @Override
    public IEventListener create(Method method, Object target) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        Class<?> cls = createWrapper(method);
        if (Modifier.isStatic(method.getModifiers()))
            return (IEventListener)cls.getDeclaredConstructor().newInstance();
        else
            return (IEventListener)cls.getConstructor(Object.class).newInstance(target);
    }

    protected Class<?> createWrapper(Method callback) {
        return cache.computeIfAbsent(callback, () -> null, discard -> createWrapper0(callback));
    }

    private static Class<?> createWrapper0(Method callback) {
        try {
            callback.setAccessible(true);

            var node = new ClassWriter(0);
            var handle = LOOKUP.unreflect(callback);

            var argTypes = handle.type().parameterArray();
            Arrays.fill(argTypes, Object.class);
            var boxedHandle = handle.asType(MethodType.methodType(handle.type().returnType(), argTypes));

            transformNode(ClassLoaderFactory.class.getName() + "$" + callback.getName(), callback, node);
            return LOOKUP.defineHiddenClassWithClassData(node.toByteArray(), boxedHandle, true).lookupClass();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create listener", e);
        }
    }

    protected static void transformNode(String name, Method callback, ClassVisitor target) {
        MethodVisitor mv;

        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        String desc = name.replace('.',  '/');

        /*
        System.out.println("Name:     " + name);
        System.out.println("Desc:     " + desc);
        System.out.println("InstType: " + instType);
        System.out.println("Callback: " + callback.getName() + Type.getMethodDescriptor(callback));
        System.out.println("Event:    " + eventType);
        */

        target.visit(V16, ACC_PUBLIC | ACC_SUPER, desc, null, "java/lang/Object", new String[]{ HANDLER_DESC });

        target.visitSource(".dynamic", null);
        {
            if (!isStatic)
                target.visitField(ACC_PUBLIC, "instance", "Ljava/lang/Object;", null, null).visitEnd();
        }
        {
            mv = target.visitMethod(ACC_PUBLIC, "<init>", isStatic ? "()V" : "(Ljava/lang/Object;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            if (!isStatic) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, desc, "instance", "Ljava/lang/Object;");
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = target.visitMethod(ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC, null, null);
            mv.visitCode();
            mv.visitLdcInsn(METHOD_CONSTANT);
            if (!isStatic) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, desc, "instance", "Ljava/lang/Object;");
            }
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(
                INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                isStatic ? "(Ljava/lang/Object;)V" : "(Ljava/lang/Object;Ljava/lang/Object;)V", false
            );
            mv.visitInsn(RETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }
        target.visitEnd();
    }
}

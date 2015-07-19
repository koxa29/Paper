package io.paperdb;

import android.util.Log;

import javassist.util.proxy.ProxyFactory;

import java.lang.reflect.*;
import java.util.Arrays;

import static io.paperdb.Paper.TAG;

class PaperTable<T> {

    // Serialized content
    private T mContent;

    @SuppressWarnings("UnusedDeclaration") PaperTable() {
    }

    PaperTable(T content) {
        mContent = content;
    }

    public T getContent() {
        return mContent;
    }

    public void removeReferences() {
        try {
            doRemoveReferences(mContent);
        }
        catch (Exception e) {
            Log.e(TAG, "Error while replacing cycle references", e);
        }
    }

    public void restoreReferences() {
        try {
            doRestoreReferences(mContent);
        }
        catch (Exception e) {
            Log.e(TAG, "Error while restoring references", e);
        }
    }

    private void doRemoveReferences(Object entry) throws Exception {
        Field[] fields = entry.getClass().getDeclaredFields();
        for(Field field : fields) {
            field.setAccessible(true);
            Object fieldValue = field.get(entry);
            if (fieldValue==null)
                continue;
            if (entry == fieldValue) {
                replaceCyclicReference(entry, field);
                continue;
            }
            if (isNotSystemReference(fieldValue)) {
                doRemoveReferences(fieldValue);
            }
        }
    }

    private boolean isNotSystemReference(Object entry) throws IllegalAccessException {
        return !entry.getClass().getCanonicalName().startsWith("java");
    }

    private void replaceCyclicReference(Object entry, Field field) throws Exception {
        Object proxy = makeProxy(field.get(entry));
        Method setterMethod = getSetterMethod(entry, field);
        setterMethod.invoke(entry, proxy);
    }

    private void doRestoreReferences(Object entry) throws Exception {
        Field[] fields = entry.getClass().getDeclaredFields();
        for(Field field : fields) {
            field.setAccessible(true);
            Object fieldValue = field.get(entry);
            if (fieldValue==null)
                continue;
            if (field.get(entry) instanceof CyclicReference) {
                restoreCyclicReference(entry, field);
                continue;
            }
            if (isNotSystemReference(fieldValue)) {
                doRestoreReferences(fieldValue);
            }
        }
    }

    private void restoreCyclicReference(Object entry, Field field) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method setterMethod = getSetterMethod(entry, field);
        setterMethod.invoke(entry, entry);
    }

    private Method getSetterMethod(Object entry, Field field) throws NoSuchMethodException {
        String fieldName = field.getName();
        String fieldNameWithFirstCharacterToUpperCase = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        return entry.getClass().getDeclaredMethod("set" + fieldNameWithFirstCharacterToUpperCase, field.getType());
    }

    private Object makeProxy(final Object fieldValue) throws Exception {
        final Class<?>[] interfaces = fieldValue.getClass().getInterfaces();
        final Class<?>[] classes = Arrays.copyOf(interfaces, interfaces.length + 1);
        classes[classes.length - 1] = CyclicReference.class;
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(fieldValue.getClass());
        factory.setInterfaces(classes);
        Object proxyInstance = factory.create(new Class<?>[0], new Object[0]);
        return proxyInstance;
    }
}

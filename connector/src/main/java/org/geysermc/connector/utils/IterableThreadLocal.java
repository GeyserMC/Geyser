package org.geysermc.connector.utils;

import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public abstract class IterableThreadLocal<T> extends ThreadLocal<T> implements Iterable<T> {
    private ThreadLocal<T> flag;
    private ConcurrentLinkedDeque<T> allValues = new ConcurrentLinkedDeque<>();

    public IterableThreadLocal() {
    }

    @Override
    protected final T initialValue() {
        T value = init();
        if (value != null) {
            allValues.add(value);
        }
        return value;
    }

    @Override
    public final Iterator<T> iterator() {
        return getAll().iterator();
    }

    public T init() {
        return null;
    }

    public void clean() {
        IterableThreadLocal.clean(this);
    }

    public static void clean(ThreadLocal instance) {
        try {
            ThreadGroup rootGroup = Thread.currentThread( ).getThreadGroup( );
            ThreadGroup parentGroup;
            while ( ( parentGroup = rootGroup.getParent() ) != null ) {
                rootGroup = parentGroup;
            }
            Thread[] threads = new Thread[ rootGroup.activeCount() ];
            if (threads.length != 0) {
                while (rootGroup.enumerate(threads, true) == threads.length) {
                    threads = new Thread[threads.length * 2];
                }
            }
            Field tl = Thread.class.getDeclaredField("threadLocals");
            tl.setAccessible(true);
            Method methodRemove = null;
            for (Thread thread : threads) {
                if (thread != null) {
                    Object tlm = tl.get(thread);
                    if (tlm != null) {
                        if (methodRemove == null) {
                            methodRemove = tlm.getClass().getDeclaredMethod("remove", ThreadLocal.class);
                            methodRemove.setAccessible(true);
                        }
                        if (methodRemove != null) {
                            try {
                                methodRemove.invoke(tlm, instance);
                            } catch (Throwable ignore) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cleanAll() {
        try {
            // Get a reference to the thread locals table of the current thread
            Thread thread = Thread.currentThread();
            Field threadLocalsField = Thread.class.getDeclaredField("threadLocals");
            threadLocalsField.setAccessible(true);
            Object threadLocalTable = threadLocalsField.get(thread);

            // Get a reference to the array holding the thread local variables inside the
            // ThreadLocalMap of the current thread
            Class threadLocalMapClass = Class.forName("java.lang.ThreadLocal$ThreadLocalMap");
            Field tableField = threadLocalMapClass.getDeclaredField("table");
            tableField.setAccessible(true);
            Object table = tableField.get(threadLocalTable);

            // The key to the ThreadLocalMap is a WeakReference object. The referent field of this object
            // is a reference to the actual ThreadLocal variable
            Field referentField = Reference.class.getDeclaredField("referent");
            referentField.setAccessible(true);

            for (int i = 0; i < Array.getLength(table); i++) {
                // Each entry in the table array of ThreadLocalMap is an Entry object
                // representing the thread local reference and its value
                Object entry = Array.get(table, i);
                if (entry != null) {
                    // Get a reference to the thread local object and remove it from the table
                    ThreadLocal threadLocal = (ThreadLocal)referentField.get(entry);
                    clean(threadLocal);
                }
            }
        } catch(Exception e) {
            // We will tolerate an exception here and just log it
            throw new IllegalStateException(e);
        }
    }

    public final Collection<T> getAll() {
        return Collections.unmodifiableCollection(allValues);
    }

    @Override
    protected void finalize() throws Throwable {
        clean(this);
        super.finalize();
    }
}
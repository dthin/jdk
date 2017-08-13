//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package sun.nio.ch;

import sun.misc.JavaIOFileDescriptorAccess;
import sun.misc.SharedSecrets;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;

class FileDispatcherImpl extends FileDispatcher {
    private static final boolean fastFileTransfer;
    private final boolean append;

    FileDispatcherImpl(boolean var1) {
        this.append = var1;
    }

    FileDispatcherImpl() {
        this(false);
    }

    boolean needsPositionLock() {
        return true;
    }

    int read(FileDescriptor var1, long var2, int var4) throws IOException {
        return read0(var1, var2, var4);
    }

    int pread(FileDescriptor var1, long var2, int var4, long var5) throws IOException {
        return pread0(var1, var2, var4, var5);
    }

    long readv(FileDescriptor var1, long var2, int var4) throws IOException {
        return readv0(var1, var2, var4);
    }

    int write(FileDescriptor var1, long var2, int var4) throws IOException {
        return write0(var1, var2, var4, this.append);
    }

    int pwrite(FileDescriptor var1, long var2, int var4, long var5) throws IOException {
        return pwrite0(var1, var2, var4, var5);
    }

    long writev(FileDescriptor var1, long var2, int var4) throws IOException {
        return writev0(var1, var2, var4, this.append);
    }

    int force(FileDescriptor var1, boolean var2) throws IOException {
        return force0(var1, var2);
    }

    int truncate(FileDescriptor var1, long var2) throws IOException {
        return truncate0(var1, var2);
    }

    long size(FileDescriptor var1) throws IOException {
        return size0(var1);
    }

    int lock(FileDescriptor var1, boolean var2, long var3, long var5, boolean var7) throws IOException {
        return lock0(var1, var2, var3, var5, var7);
    }

    void release(FileDescriptor var1, long var2, long var4) throws IOException {
        release0(var1, var2, var4);
    }

    void close(FileDescriptor var1) throws IOException {
        close0(var1);
    }

    FileDescriptor duplicateForMapping(FileDescriptor var1) throws IOException {
        JavaIOFileDescriptorAccess var2 = SharedSecrets.getJavaIOFileDescriptorAccess();
        FileDescriptor var3 = new FileDescriptor();
        long var4 = duplicateHandle(var2.getHandle(var1));
        var2.setHandle(var3, var4);
        return var3;
    }

    boolean canTransferToDirectly(SelectableChannel var1) {
        return fastFileTransfer && var1.isBlocking();
    }

    boolean transferToDirectlyNeedsPositionLock() {
        return true;
    }

    static boolean isFastFileTransferRequested() {
        String var0 = (String)AccessController.doPrivileged(new PrivilegedAction() {
            public String run() {
                return System.getProperty("jdk.nio.enableFastFileTransfer");
            }
        });
        boolean var1;
        if("".equals(var0)) {
            var1 = true;
        } else {
            var1 = Boolean.parseBoolean(var0);
        }

        return var1;
    }

    static native int read0(FileDescriptor var0, long var1, int var3) throws IOException;

    static native int pread0(FileDescriptor var0, long var1, int var3, long var4) throws IOException;

    static native long readv0(FileDescriptor var0, long var1, int var3) throws IOException;

    static native int write0(FileDescriptor var0, long var1, int var3, boolean var4) throws IOException;

    static native int pwrite0(FileDescriptor var0, long var1, int var3, long var4) throws IOException;

    static native long writev0(FileDescriptor var0, long var1, int var3, boolean var4) throws IOException;

    static native int force0(FileDescriptor var0, boolean var1) throws IOException;

    static native int truncate0(FileDescriptor var0, long var1) throws IOException;

    static native long size0(FileDescriptor var0) throws IOException;

    static native int lock0(FileDescriptor var0, boolean var1, long var2, long var4, boolean var6) throws IOException;

    static native void release0(FileDescriptor var0, long var1, long var3) throws IOException;

    static native void close0(FileDescriptor var0) throws IOException;

    static native void closeByHandle(long var0) throws IOException;

    static native long duplicateHandle(long var0) throws IOException;

    static {
        IOUtil.load();
        fastFileTransfer = isFastFileTransferRequested();
    }
}

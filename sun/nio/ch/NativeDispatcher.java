//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;

abstract class NativeDispatcher {
    NativeDispatcher() {
    }

    abstract int read(FileDescriptor var1, long var2, int var4) throws IOException;

    boolean needsPositionLock() {
        return false;
    }

    int pread(FileDescriptor var1, long var2, int var4, long var5) throws IOException {
        throw new IOException("Operation Unsupported");
    }

    abstract long readv(FileDescriptor var1, long var2, int var4) throws IOException;

    abstract int write(FileDescriptor var1, long var2, int var4) throws IOException;

    int pwrite(FileDescriptor var1, long var2, int var4, long var5) throws IOException {
        throw new IOException("Operation Unsupported");
    }

    abstract long writev(FileDescriptor var1, long var2, int var4) throws IOException;

    abstract void close(FileDescriptor var1) throws IOException;

    void preClose(FileDescriptor var1) throws IOException {
    }
}

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package sun.nio.ch;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.OverlappingFileLockException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import sun.misc.Cleaner;
import sun.misc.JavaNioAccess.BufferPool;
import sun.nio.ch.DirectBuffer;
import sun.nio.ch.FileDispatcher;
import sun.nio.ch.FileDispatcherImpl;
import sun.nio.ch.FileLockImpl;
import sun.nio.ch.FileLockTable;
import sun.nio.ch.IOStatus;
import sun.nio.ch.IOUtil;
import sun.nio.ch.NativeDispatcher;
import sun.nio.ch.NativeThreadSet;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SinkChannelImpl;
import sun.nio.ch.Util;
import sun.security.action.GetPropertyAction;

public class FileChannelImpl extends FileChannel {
    private static final long allocationGranularity;
    private final FileDispatcher nd;
    private final FileDescriptor fd;
    private final boolean writable;
    private final boolean readable;
    private final boolean append;
    private final Object parent;
    private final String path;
    private final NativeThreadSet threads = new NativeThreadSet(2);
    private final Object positionLock = new Object();
    private static volatile boolean transferSupported = true;
    private static volatile boolean pipeSupported = true;
    private static volatile boolean fileSupported = true;
    private static final long MAPPED_TRANSFER_SIZE = 8388608L;
    private static final int TRANSFER_SIZE = 8192;
    private static final int MAP_RO = 0;
    private static final int MAP_RW = 1;
    private static final int MAP_PV = 2;
    private volatile FileLockTable fileLockTable;
    private static boolean isSharedFileLockTable;
    private static volatile boolean propertyChecked;

    private FileChannelImpl(FileDescriptor var1, String var2, boolean var3, boolean var4, boolean var5, Object var6) {
        this.fd = var1;
        this.readable = var3;
        this.writable = var4;
        this.append = var5;
        this.parent = var6;
        this.path = var2;
        this.nd = new FileDispatcherImpl(var5);
    }

    public static FileChannel open(FileDescriptor var0, String var1, boolean var2, boolean var3, Object var4) {
        return new FileChannelImpl(var0, var1, var2, var3, false, var4);
    }

    public static FileChannel open(FileDescriptor var0, String var1, boolean var2, boolean var3, boolean var4, Object var5) {
        return new FileChannelImpl(var0, var1, var2, var3, var4, var5);
    }

    private void ensureOpen() throws IOException {
        if(!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    protected void implCloseChannel() throws IOException {
        if(this.fileLockTable != null) {
            Iterator var1 = this.fileLockTable.removeAll().iterator();

            while(var1.hasNext()) {
                FileLock var2 = (FileLock)var1.next();
                synchronized(var2) {
                    if(var2.isValid()) {
                        this.nd.release(this.fd, var2.position(), var2.size());
                        ((FileLockImpl)var2).invalidate();
                    }
                }
            }
        }

        this.threads.signalAndWait();
        if(this.parent != null) {
            ((Closeable)this.parent).close();
        } else {
            this.nd.close(this.fd);
        }

    }
    /**从文件中读取字节写入到ByteBuffer*/
    public int read(ByteBuffer var1) throws IOException {
        this.ensureOpen();
        if(!this.readable) {
            throw new NonReadableChannelException();
        } else {
            Object var2 = this.positionLock;
            synchronized(this.positionLock) {
                int var3 = 0;
                int var4 = -1;

                byte var5;
                try {
                    this.begin();
                    var4 = this.threads.add();
                    if(this.isOpen()) {
                        do {
                            var3 = IOUtil.read(this.fd, var1, -1L, this.nd);
                        } while(var3 == -3 && this.isOpen());

                        int var12 = IOStatus.normalize(var3);
                        return var12;
                    }

                    var5 = 0;
                } finally {
                    this.threads.remove(var4);
                    this.end(var3 > 0);

                    assert IOStatus.check(var3);

                }

                return var5;
            }
        }
    }

    public long read(ByteBuffer[] var1, int var2, int var3) throws IOException {
        if(var2 >= 0 && var3 >= 0 && var2 <= var1.length - var3) {
            this.ensureOpen();
            if(!this.readable) {
                throw new NonReadableChannelException();
            } else {
                Object var4 = this.positionLock;
                synchronized(this.positionLock) {
                    long var5 = 0L;
                    int var7 = -1;

                    try {
                        this.begin();
                        var7 = this.threads.add();
                        long var8;
                        if(!this.isOpen()) {
                            var8 = 0L;
                            return var8;
                        } else {
                            do {
                                var5 = IOUtil.read(this.fd, var1, var2, var3, this.nd);
                            } while(var5 == -3L && this.isOpen());

                            var8 = IOStatus.normalize(var5);
                            return var8;
                        }
                    } finally {
                        this.threads.remove(var7);
                        this.end(var5 > 0L);

                        assert IOStatus.check(var5);

                    }
                }
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public int write(ByteBuffer var1) throws IOException {
        this.ensureOpen();
        if(!this.writable) {
            throw new NonWritableChannelException();
        } else {
            Object var2 = this.positionLock;
            synchronized(this.positionLock) {
                int var3 = 0;
                int var4 = -1;

                byte var5;
                try {
                    this.begin();
                    var4 = this.threads.add();
                    if(this.isOpen()) {
                        do {
                            var3 = IOUtil.write(this.fd, var1, -1L, this.nd);
                        } while(var3 == -3 && this.isOpen());

                        int var12 = IOStatus.normalize(var3);
                        return var12;
                    }

                    var5 = 0;
                } finally {
                    this.threads.remove(var4);
                    this.end(var3 > 0);

                    assert IOStatus.check(var3);

                }

                return var5;
            }
        }
    }

    public long write(ByteBuffer[] var1, int var2, int var3) throws IOException {
        if(var2 >= 0 && var3 >= 0 && var2 <= var1.length - var3) {
            this.ensureOpen();
            if(!this.writable) {
                throw new NonWritableChannelException();
            } else {
                Object var4 = this.positionLock;
                synchronized(this.positionLock) {
                    long var5 = 0L;
                    int var7 = -1;

                    try {
                        this.begin();
                        var7 = this.threads.add();
                        long var8;
                        if(!this.isOpen()) {
                            var8 = 0L;
                            return var8;
                        } else {
                            do {
                                var5 = IOUtil.write(this.fd, var1, var2, var3, this.nd);
                            } while(var5 == -3L && this.isOpen());

                            var8 = IOStatus.normalize(var5);
                            return var8;
                        }
                    } finally {
                        this.threads.remove(var7);
                        this.end(var5 > 0L);

                        assert IOStatus.check(var5);

                    }
                }
            }
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    public long position() throws IOException {
        this.ensureOpen();
        Object var1 = this.positionLock;
        synchronized(this.positionLock) {
            long var2 = -1L;
            int var4 = -1;

            try {
                this.begin();
                var4 = this.threads.add();
                long var5;
                if(!this.isOpen()) {
                    var5 = 0L;
                    return var5;
                } else {
                    do {
                        var2 = this.append?this.nd.size(this.fd):this.position0(this.fd, -1L);
                    } while(var2 == -3L && this.isOpen());

                    var5 = IOStatus.normalize(var2);
                    return var5;
                }
            } finally {
                this.threads.remove(var4);
                this.end(var2 > -1L);

                assert IOStatus.check(var2);

            }
        }
    }

    public FileChannel position(long var1) throws IOException {
        this.ensureOpen();
        if(var1 < 0L) {
            throw new IllegalArgumentException();
        } else {
            Object var3 = this.positionLock;
            synchronized(this.positionLock) {
                long var4 = -1L;
                int var6 = -1;

                try {
                    this.begin();
                    var6 = this.threads.add();
                    FileChannelImpl var7;
                    if(!this.isOpen()) {
                        var7 = null;
                        return var7;
                    } else {
                        do {
                            var4 = this.position0(this.fd, var1);
                        } while(var4 == -3L && this.isOpen());

                        var7 = this;
                        return var7;
                    }
                } finally {
                    this.threads.remove(var6);
                    this.end(var4 > -1L);

                    assert IOStatus.check(var4);

                }
            }
        }
    }

    public long size() throws IOException {
        this.ensureOpen();
        Object var1 = this.positionLock;
        synchronized(this.positionLock) {
            long var2 = -1L;
            int var4 = -1;

            try {
                this.begin();
                var4 = this.threads.add();
                long var5;
                if(!this.isOpen()) {
                    var5 = -1L;
                    return var5;
                } else {
                    do {
                        var2 = this.nd.size(this.fd);
                    } while(var2 == -3L && this.isOpen());

                    var5 = IOStatus.normalize(var2);
                    return var5;
                }
            } finally {
                this.threads.remove(var4);
                this.end(var2 > -1L);

                assert IOStatus.check(var2);

            }
        }
    }

    public FileChannel truncate(long var1) throws IOException {
        this.ensureOpen();
        if(var1 < 0L) {
            throw new IllegalArgumentException("Negative size");
        } else if(!this.writable) {
            throw new NonWritableChannelException();
        } else {
            Object var3 = this.positionLock;
            synchronized(this.positionLock) {
                int var4 = -1;
                long var5 = -1L;
                int var7 = -1;
                long var8 = -1L;

                try {
                    this.begin();
                    var7 = this.threads.add();
                    if(!this.isOpen()) {
                        Object var19 = null;
                        return (FileChannel)var19;
                    } else {
                        long var10;
                        do {
                            var10 = this.nd.size(this.fd);
                        } while(var10 == -3L && this.isOpen());

                        FileChannelImpl var12;
                        if(!this.isOpen()) {
                            var12 = null;
                            return var12;
                        } else {
                            do {
                                var5 = this.position0(this.fd, -1L);
                            } while(var5 == -3L && this.isOpen());

                            if(!this.isOpen()) {
                                var12 = null;
                                return var12;
                            } else {
                                assert var5 >= 0L;

                                if(var1 < var10) {
                                    while(true) {
                                        var4 = this.nd.truncate(this.fd, var1);
                                        if(var4 != -3 || !this.isOpen()) {
                                            if(!this.isOpen()) {
                                                var12 = null;
                                                return var12;
                                            }
                                            break;
                                        }
                                    }
                                }

                                if(var5 > var1) {
                                    var5 = var1;
                                }

                                do {
                                    var8 = this.position0(this.fd, var5);
                                } while(var8 == -3L && this.isOpen());

                                var12 = this;
                                return var12;
                            }
                        }
                    }
                } finally {
                    this.threads.remove(var7);
                    this.end(var4 > -1);

                    assert IOStatus.check(var4);

                }
            }
        }
    }

    public void force(boolean var1) throws IOException {
        this.ensureOpen();
        int var2 = -1;
        int var3 = -1;

        try {
            this.begin();
            var3 = this.threads.add();
            if(this.isOpen()) {
                do {
                    var2 = this.nd.force(this.fd, var1);
                } while(var2 == -3 && this.isOpen());

                return;
            }
        } finally {
            this.threads.remove(var3);
            this.end(var2 > -1);

            assert IOStatus.check(var2);

        }

    }

    private long transferToDirectlyInternal(long var1, int var3, WritableByteChannel var4, FileDescriptor var5) throws IOException {
        assert !this.nd.transferToDirectlyNeedsPositionLock() || Thread.holdsLock(this.positionLock);

        long var6 = -1L;
        int var8 = -1;

        long var9;
        try {
            this.begin();
            var8 = this.threads.add();
            if(!this.isOpen()) {
                var9 = -1L;
                return var9;
            }

            do {
                var6 = this.transferTo0(this.fd, var1, (long)var3, var5);
            } while(var6 == -3L && this.isOpen());

            if(var6 == -6L) {
                if(var4 instanceof SinkChannelImpl) {
                    pipeSupported = false;
                }

                if(var4 instanceof FileChannelImpl) {
                    fileSupported = false;
                }

                var9 = -6L;
                return var9;
            }

            if(var6 != -4L) {
                var9 = IOStatus.normalize(var6);
                return var9;
            }

            transferSupported = false;
            var9 = -4L;
        } finally {
            this.threads.remove(var8);
            this.end(var6 > -1L);
        }

        return var9;
    }

    private long transferToDirectly(long var1, int var3, WritableByteChannel var4) throws IOException {
        if(!transferSupported) {
            return -4L;
        } else {
            FileDescriptor var5 = null;
            if(var4 instanceof FileChannelImpl) {
                if(!fileSupported) {
                    return -6L;
                }

                var5 = ((FileChannelImpl)var4).fd;
            } else if(var4 instanceof SelChImpl) {
                if(var4 instanceof SinkChannelImpl && !pipeSupported) {
                    return -6L;
                }

                SelectableChannel var6 = (SelectableChannel)var4;
                if(!this.nd.canTransferToDirectly(var6)) {
                    return -6L;
                }

                var5 = ((SelChImpl)var4).getFD();
            }

            if(var5 == null) {
                return -4L;
            } else {
                int var19 = IOUtil.fdVal(this.fd);
                int var7 = IOUtil.fdVal(var5);
                if(var19 == var7) {
                    return -4L;
                } else if(this.nd.transferToDirectlyNeedsPositionLock()) {
                    Object var8 = this.positionLock;
                    synchronized(this.positionLock) {
                        long var9 = this.position();

                        long var11;
                        try {
                            var11 = this.transferToDirectlyInternal(var1, var3, var4, var5);
                        } finally {
                            this.position(var9);
                        }

                        return var11;
                    }
                } else {
                    return this.transferToDirectlyInternal(var1, var3, var4, var5);
                }
            }
        }
    }

    private long transferToTrustedChannel(long var1, long var3, WritableByteChannel var5) throws IOException {
        boolean var6 = var5 instanceof SelChImpl;
        if(!(var5 instanceof FileChannelImpl) && !var6) {
            return -4L;
        } else {
            long var7 = var3;

            while(var7 > 0L) {
                long var9 = Math.min(var7, 8388608L);

                try {
                    MappedByteBuffer var11 = this.map(MapMode.READ_ONLY, var1, var9);

                    try {
                        int var12 = var5.write(var11);

                        assert var12 >= 0;

                        var7 -= (long)var12;
                        if(var6) {
                            break;
                        }

                        assert var12 > 0;

                        var1 += (long)var12;
                    } finally {
                        unmap(var11);
                    }
                } catch (ClosedByInterruptException var20) {
                    assert !var5.isOpen();

                    try {
                        this.close();
                    } catch (Throwable var18) {
                        var20.addSuppressed(var18);
                    }

                    throw var20;
                } catch (IOException var21) {
                    if(var7 != var3) {
                        break;
                    }

                    throw var21;
                }
            }

            return var3 - var7;
        }
    }

    private long transferToArbitraryChannel(long var1, int var3, WritableByteChannel var4) throws IOException {
        int var5 = Math.min(var3, 8192);
        ByteBuffer var6 = Util.getTemporaryDirectBuffer(var5);
        long var7 = 0L;
        long var9 = var1;

        long var12;
        try {
            Util.erase(var6);

            while(true) {
                if(var7 < (long)var3) {
                    var6.limit(Math.min((int)((long)var3 - var7), 8192));
                    int var11 = this.read(var6, var9);
                    if(var11 > 0) {
                        var6.flip();
                        int var20 = var4.write(var6);
                        var7 += (long)var20;
                        if(var20 == var11) {
                            var9 += (long)var20;
                            var6.clear();
                            continue;
                        }
                    }
                }

                long var19 = var7;
                return var19;
            }
        } catch (IOException var17) {
            if(var7 <= 0L) {
                throw var17;
            }

            var12 = var7;
        } finally {
            Util.releaseTemporaryDirectBuffer(var6);
        }

        return var12;
    }

    public long transferTo(long var1, long var3, WritableByteChannel var5) throws IOException {
        this.ensureOpen();
        if(!var5.isOpen()) {
            throw new ClosedChannelException();
        } else if(!this.readable) {
            throw new NonReadableChannelException();
        } else if(var5 instanceof FileChannelImpl && !((FileChannelImpl)var5).writable) {
            throw new NonWritableChannelException();
        } else if(var1 >= 0L && var3 >= 0L) {
            long var6 = this.size();
            if(var1 > var6) {
                return 0L;
            } else {
                int var8 = (int)Math.min(var3, 2147483647L);
                if(var6 - var1 < (long)var8) {
                    var8 = (int)(var6 - var1);
                }

                long var9;
                return (var9 = this.transferToDirectly(var1, var8, var5)) >= 0L?var9:((var9 = this.transferToTrustedChannel(var1, (long)var8, var5)) >= 0L?var9:this.transferToArbitraryChannel(var1, var8, var5));
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private long transferFromFileChannel(FileChannelImpl var1, long var2, long var4) throws IOException {
        if(!var1.readable) {
            throw new NonReadableChannelException();
        } else {
            Object var6 = var1.positionLock;
            synchronized(var1.positionLock) {
                long var7 = var1.position();
                long var9 = Math.min(var4, var1.size() - var7);
                long var11 = var9;
                long var13 = var7;

                long var15;
                while(var11 > 0L) {
                    var15 = Math.min(var11, 8388608L);
                    MappedByteBuffer var17 = var1.map(MapMode.READ_ONLY, var13, var15);

                    try {
                        long var18 = (long)this.write(var17, var2);

                        assert var18 > 0L;

                        var13 += var18;
                        var2 += var18;
                        var11 -= var18;
                    } catch (IOException var25) {
                        if(var11 != var9) {
                            break;
                        }

                        throw var25;
                    } finally {
                        unmap(var17);
                    }
                }

                var15 = var9 - var11;
                var1.position(var7 + var15);
                return var15;
            }
        }
    }

    private long transferFromArbitraryChannel(ReadableByteChannel var1, long var2, long var4) throws IOException {
        int var6 = (int)Math.min(var4, 8192L);
        ByteBuffer var7 = Util.getTemporaryDirectBuffer(var6);
        long var8 = 0L;
        long var10 = var2;

        long var13;
        try {
            Util.erase(var7);

            while(true) {
                if(var8 < var4) {
                    var7.limit((int)Math.min(var4 - var8, 8192L));
                    int var12 = var1.read(var7);
                    if(var12 > 0) {
                        var7.flip();
                        int var21 = this.write(var7, var10);
                        var8 += (long)var21;
                        if(var21 == var12) {
                            var10 += (long)var21;
                            var7.clear();
                            continue;
                        }
                    }
                }

                long var20 = var8;
                return var20;
            }
        } catch (IOException var18) {
            if(var8 <= 0L) {
                throw var18;
            }

            var13 = var8;
        } finally {
            Util.releaseTemporaryDirectBuffer(var7);
        }

        return var13;
    }

    public long transferFrom(ReadableByteChannel var1, long var2, long var4) throws IOException {
        this.ensureOpen();
        if(!var1.isOpen()) {
            throw new ClosedChannelException();
        } else if(!this.writable) {
            throw new NonWritableChannelException();
        } else if(var2 >= 0L && var4 >= 0L) {
            return var2 > this.size()?0L:(var1 instanceof FileChannelImpl?this.transferFromFileChannel((FileChannelImpl)var1, var2, var4):this.transferFromArbitraryChannel(var1, var2, var4));
        } else {
            throw new IllegalArgumentException();
        }
    }

    public int read(ByteBuffer var1, long var2) throws IOException {
        if(var1 == null) {
            throw new NullPointerException();
        } else if(var2 < 0L) {
            throw new IllegalArgumentException("Negative position");
        } else if(!this.readable) {
            throw new NonReadableChannelException();
        } else {
            this.ensureOpen();
            if(this.nd.needsPositionLock()) {
                Object var4 = this.positionLock;
                synchronized(this.positionLock) {
                    return this.readInternal(var1, var2);
                }
            } else {
                return this.readInternal(var1, var2);
            }
        }
    }

    private int readInternal(ByteBuffer var1, long var2) throws IOException {
        assert !this.nd.needsPositionLock() || Thread.holdsLock(this.positionLock);

        int var4 = 0;
        int var5 = -1;

        byte var6;
        try {
            this.begin();
            var5 = this.threads.add();
            if(this.isOpen()) {
                do {
                    var4 = IOUtil.read(this.fd, var1, var2, this.nd);
                } while(var4 == -3 && this.isOpen());

                int var10 = IOStatus.normalize(var4);
                return var10;
            }

            var6 = -1;
        } finally {
            this.threads.remove(var5);
            this.end(var4 > 0);

            assert IOStatus.check(var4);

        }

        return var6;
    }

    public int write(ByteBuffer var1, long var2) throws IOException {
        if(var1 == null) {
            throw new NullPointerException();
        } else if(var2 < 0L) {
            throw new IllegalArgumentException("Negative position");
        } else if(!this.writable) {
            throw new NonWritableChannelException();
        } else {
            this.ensureOpen();
            if(this.nd.needsPositionLock()) {
                Object var4 = this.positionLock;
                synchronized(this.positionLock) {
                    return this.writeInternal(var1, var2);
                }
            } else {
                return this.writeInternal(var1, var2);
            }
        }
    }

    private int writeInternal(ByteBuffer var1, long var2) throws IOException {
        assert !this.nd.needsPositionLock() || Thread.holdsLock(this.positionLock);

        int var4 = 0;
        int var5 = -1;

        byte var6;
        try {
            this.begin();
            var5 = this.threads.add();
            if(this.isOpen()) {
                do {
                    var4 = IOUtil.write(this.fd, var1, var2, this.nd);
                } while(var4 == -3 && this.isOpen());

                int var10 = IOStatus.normalize(var4);
                return var10;
            }

            var6 = -1;
        } finally {
            this.threads.remove(var5);
            this.end(var4 > 0);

            assert IOStatus.check(var4);

        }

        return var6;
    }

    private static void unmap(MappedByteBuffer var0) {
        Cleaner var1 = ((DirectBuffer)var0).cleaner();
        if(var1 != null) {
            var1.clean();
        }

    }
    /**
     * @param var2 position
     * @param var4 size
     * */
    public MappedByteBuffer map(MapMode var1, long var2, long var4) throws IOException {
        this.ensureOpen();
        if(var1 == null) {
            throw new NullPointerException("Mode is null");
        } else if(var2 < 0L) {
            throw new IllegalArgumentException("Negative position");
        } else if(var4 < 0L) {
            throw new IllegalArgumentException("Negative size");
        } else if(var2 + var4 < 0L) {
            throw new IllegalArgumentException("Position + size overflow");
        } else if(var4 > 2147483647L) {
            throw new IllegalArgumentException("Size exceeds Integer.MAX_VALUE");
        } else {
            byte var6 = -1;
            if(var1 == MapMode.READ_ONLY) {
                var6 = 0;
            } else if(var1 == MapMode.READ_WRITE) {
                var6 = 1;
            } else if(var1 == MapMode.PRIVATE) {
                var6 = 2;
            }

            assert var6 >= 0;

            if(var1 != MapMode.READ_ONLY && !this.writable) {
                throw new NonWritableChannelException();
            } else if(!this.readable) {
                throw new NonReadableChannelException();
            } else {

                long var7 = -1L;
                int var9 = -1;

                MappedByteBuffer var20;
                try {
                    this.begin();
                    var9 = this.threads.add();
                    /**如果此通道关上，返回null*/
                    if(!this.isOpen()) {
                        Object var32 = null;
                        return (MappedByteBuffer)var32;
                    }

                    long var10;
                    do {
                        /**应该是返回文件大小*/
                        var10 = this.nd.size(this.fd);
                    } while(var10 == -3L && this.isOpen());

                    FileDescriptor var33;
                    if(!this.isOpen()) {
                        var33 = null;
                        return var33;
                    }

                    MappedByteBuffer var34;
                    int var12;
                    /**如果文件和实际大小<position+size*/
                    if(var10 < var2 + var4) {
                        if(!this.writable) {
                            throw new IOException("Channel not open for writing - cannot extend file to required size");
                        }

                        while(true) {
                            var12 = this.nd.truncate(this.fd, var2 + var4);
                            if(var12 != -3 || !this.isOpen()) {
                                if(!this.isOpen()) {
                                    var34 = null;
                                    return var34;
                                }
                                break;
                            }
                        }
                    }

                    if(var4 == 0L) {
                        var7 = 0L;
                        var33 = new FileDescriptor();
                        if(this.writable && var6 != 0) {
                            var34 = Util.newMappedByteBuffer(0, 0L, var33, (Runnable)null);
                            return var34;
                        }

                        var34 = Util.newMappedByteBufferR(0, 0L, var33, (Runnable)null);
                        return var34;
                    }

                    var12 = (int)(var2 % allocationGranularity);
                    long var13 = var2 - (long)var12;
                    long var15 = var4 + (long)var12;

                    try {
                        var7 = this.map0(var6, var13, var15);
                    } catch (OutOfMemoryError var30) {
                        System.gc();

                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException var29) {
                            Thread.currentThread().interrupt();
                        }

                        try {
                            var7 = this.map0(var6, var13, var15);
                        } catch (OutOfMemoryError var28) {
                            throw new IOException("Map failed", var28);
                        }
                    }

                    FileDescriptor var17;
                    try {
                        var17 = this.nd.duplicateForMapping(this.fd);
                    } catch (IOException var27) {
                        unmap0(var7, var15);
                        throw var27;
                    }

                    assert IOStatus.checkAll(var7);

                    assert var7 % allocationGranularity == 0L;

                    int var18 = (int)var4;
                    FileChannelImpl.Unmapper var19 = new FileChannelImpl.Unmapper(var7, var15, var18, var17, null);
                    if(this.writable && var6 != 0) {
                        var20 = Util.newMappedByteBuffer(var18, var7 + (long)var12, var17, var19);
                        return var20;
                    }

                    var20 = Util.newMappedByteBufferR(var18, var7 + (long)var12, var17, var19);
                } finally {
                    this.threads.remove(var9);
                    this.end(IOStatus.checkAll(var7));
                }

                return var20;
            }
        }
    }

    public static BufferPool getMappedBufferPool() {
        return new BufferPool() {
            public String getName() {
                return "mapped";
            }

            public long getCount() {
                return (long)FileChannelImpl.Unmapper.count;
            }

            public long getTotalCapacity() {
                return FileChannelImpl.Unmapper.totalCapacity;
            }

            public long getMemoryUsed() {
                return FileChannelImpl.Unmapper.totalSize;
            }
        };
    }

    private static boolean isSharedFileLockTable() {
        if(!propertyChecked) {
            Class var0 = FileChannelImpl.class;
            synchronized(FileChannelImpl.class) {
                if(!propertyChecked) {
                    String var1 = (String)AccessController.doPrivileged(new GetPropertyAction("sun.nio.ch.disableSystemWideOverlappingFileLockCheck"));
                    isSharedFileLockTable = var1 == null || var1.equals("false");
                    propertyChecked = true;
                }
            }
        }

        return isSharedFileLockTable;
    }

    private FileLockTable fileLockTable() throws IOException {
        if(this.fileLockTable == null) {
            synchronized(this) {
                if(this.fileLockTable == null) {
                    if(isSharedFileLockTable()) {
                        int var2 = this.threads.add();

                        try {
                            this.ensureOpen();
                            this.fileLockTable = FileLockTable.newSharedFileLockTable(this, this.fd);
                        } finally {
                            this.threads.remove(var2);
                        }
                    } else {
                        this.fileLockTable = new FileChannelImpl.SimpleFileLockTable();
                    }
                }
            }
        }

        return this.fileLockTable;
    }

    public FileLock lock(long var1, long var3, boolean var5) throws IOException {
        this.ensureOpen();
        if(var5 && !this.readable) {
            throw new NonReadableChannelException();
        } else if(!var5 && !this.writable) {
            throw new NonWritableChannelException();
        } else {
            /**new lock*/
            FileLockImpl var6 = new FileLockImpl(this, var1, var3, var5);
            /**locktable添加lock*/
            FileLockTable var7 = this.fileLockTable();
            var7.add(var6);
            boolean var8 = false;
            int var9 = -1;

            try {
                this.begin();
                var9 = this.threads.add();
                if(!this.isOpen()) {
                    Object var20 = null;
                    return (FileLock)var20;
                }

                int var10;
                do {
                    var10 = this.nd.lock(this.fd, true, var1, var3, var5);
                } while(var10 == 2 && this.isOpen());

                if(this.isOpen()) {
                    if(var10 == 1) {
                        assert var5;

                        FileLockImpl var11 = new FileLockImpl(this, var1, var3, false);
                        var7.replace(var6, var11);
                        var6 = var11;
                    }

                    var8 = true;
                }
            } finally {
                if(!var8) {
                    var7.remove(var6);
                }

                this.threads.remove(var9);

                try {
                    this.end(var8);
                } catch (ClosedByInterruptException var18) {
                    throw new FileLockInterruptionException();
                }
            }

            return var6;
        }
    }

    public FileLock tryLock(long var1, long var3, boolean var5) throws IOException {
        this.ensureOpen();
        if(var5 && !this.readable) {
            throw new NonReadableChannelException();
        } else if(!var5 && !this.writable) {
            throw new NonWritableChannelException();
        } else {
            FileLockImpl var6 = new FileLockImpl(this, var1, var3, var5);
            FileLockTable var7 = this.fileLockTable();
            var7.add(var6);
            int var9 = this.threads.add();

            FileLockImpl var10;
            try {
                int var8;
                try {
                    this.ensureOpen();
                    var8 = this.nd.lock(this.fd, false, var1, var3, var5);
                } catch (IOException var15) {
                    var7.remove(var6);
                    throw var15;
                }

                if(var8 != -1) {
                    if(var8 == 1) {
                        assert var5;

                        var10 = new FileLockImpl(this, var1, var3, false);
                        var7.replace(var6, var10);
                        FileLockImpl var11 = var10;
                        return var11;
                    }

                    var10 = var6;
                    return var10;
                }

                var7.remove(var6);
                var10 = null;
            } finally {
                this.threads.remove(var9);
            }

            return var10;
        }
    }

    void release(FileLockImpl var1) throws IOException {
        int var2 = this.threads.add();

        try {
            this.ensureOpen();
            this.nd.release(this.fd, var1.position(), var1.size());
        } finally {
            this.threads.remove(var2);
        }

        assert this.fileLockTable != null;

        this.fileLockTable.remove(var1);
    }

    private native long map0(int var1, long var2, long var4) throws IOException;

    private static native int unmap0(long var0, long var2);

    private native long transferTo0(FileDescriptor var1, long var2, long var4, FileDescriptor var6);

    private native long position0(FileDescriptor var1, long var2);

    private static native long initIDs();

    static {
        IOUtil.load();
        allocationGranularity = initIDs();
    }

    private static class SimpleFileLockTable extends FileLockTable {
        private final List<FileLock> lockList = new ArrayList(2);

        public SimpleFileLockTable() {
        }

        private void checkList(long var1, long var3) throws OverlappingFileLockException {
            assert Thread.holdsLock(this.lockList);

            Iterator var5 = this.lockList.iterator();

            FileLock var6;
            do {
                if(!var5.hasNext()) {
                    return;
                }

                var6 = (FileLock)var5.next();
            } while(!var6.overlaps(var1, var3));

            throw new OverlappingFileLockException();
        }

        public void add(FileLock var1) throws OverlappingFileLockException {
            List var2 = this.lockList;
            synchronized(this.lockList) {
                this.checkList(var1.position(), var1.size());
                this.lockList.add(var1);
            }
        }

        public void remove(FileLock var1) {
            List var2 = this.lockList;
            synchronized(this.lockList) {
                this.lockList.remove(var1);
            }
        }

        public List<FileLock> removeAll() {
            List var1 = this.lockList;
            synchronized(this.lockList) {
                ArrayList var2 = new ArrayList(this.lockList);
                this.lockList.clear();
                return var2;
            }
        }

        public void replace(FileLock var1, FileLock var2) {
            List var3 = this.lockList;
            synchronized(this.lockList) {
                this.lockList.remove(var1);
                this.lockList.add(var2);
            }
        }
    }

    private static class Unmapper implements Runnable {
        private static final NativeDispatcher nd = new FileDispatcherImpl();
        static volatile int count;
        static volatile long totalSize;
        static volatile long totalCapacity;
        private volatile long address;
        private final long size;
        private final int cap;
        private final FileDescriptor fd;

        private Unmapper(long var1, long var3, int var5, FileDescriptor var6) {
            assert var1 != 0L;

            this.address = var1;
            this.size = var3;
            this.cap = var5;
            this.fd = var6;
            Class var7 = FileChannelImpl.Unmapper.class;
            synchronized(FileChannelImpl.Unmapper.class) {
                ++count;
                totalSize += var3;
                totalCapacity += (long)var5;
            }
        }

        public void run() {
            if(this.address != 0L) {
                FileChannelImpl.access$000(this.address, this.size);
                this.address = 0L;
                if(this.fd.valid()) {
                    try {
                        nd.close(this.fd);
                    } catch (IOException var4) {
                        ;
                    }
                }

                Class var1 = FileChannelImpl.Unmapper.class;
                synchronized(FileChannelImpl.Unmapper.class) {
                    --count;
                    totalSize -= this.size;
                    totalCapacity -= (long)this.cap;
                }
            }
        }
    }
}

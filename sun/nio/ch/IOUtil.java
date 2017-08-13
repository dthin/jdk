//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class IOUtil {
    static final int IOV_MAX;

    private IOUtil() {
    }

    static int write(FileDescriptor var0, ByteBuffer var1, long var2, NativeDispatcher var4) throws IOException {
        if(var1 instanceof DirectBuffer) {
            return writeFromNativeBuffer(var0, var1, var2, var4);
        } else {
            int var5 = var1.position();
            int var6 = var1.limit();

            assert var5 <= var6;

            int var7 = var5 <= var6?var6 - var5:0;
            ByteBuffer var8 = Util.getTemporaryDirectBuffer(var7);

            int var10;
            try {
                var8.put(var1);
                var8.flip();
                var1.position(var5);
                int var9 = writeFromNativeBuffer(var0, var8, var2, var4);
                if(var9 > 0) {
                    var1.position(var5 + var9);
                }

                var10 = var9;
            } finally {
                Util.offerFirstTemporaryDirectBuffer(var8);
            }

            return var10;
        }
    }

    private static int writeFromNativeBuffer(FileDescriptor var0, ByteBuffer var1, long var2, NativeDispatcher var4) throws IOException {
        int var5 = var1.position();
        int var6 = var1.limit();

        assert var5 <= var6;

        int var7 = var5 <= var6?var6 - var5:0;
        boolean var8 = false;
        if(var7 == 0) {
            return 0;
        } else {
            int var9;
            if(var2 != -1L) {
                var9 = var4.pwrite(var0, ((DirectBuffer)var1).address() + (long)var5, var7, var2);
            } else {
                var9 = var4.write(var0, ((DirectBuffer)var1).address() + (long)var5, var7);
            }

            if(var9 > 0) {
                var1.position(var5 + var9);
            }

            return var9;
        }
    }

    static long write(FileDescriptor var0, ByteBuffer[] var1, NativeDispatcher var2) throws IOException {
        return write(var0, var1, 0, var1.length, var2);
    }

    static long write(FileDescriptor var0, ByteBuffer[] var1, int var2, int var3, NativeDispatcher var4) throws IOException {
        IOVecWrapper var5 = IOVecWrapper.get(var3);
        boolean var6 = false;
        int var7 = 0;
        boolean var23 = false;

        int var16;
        long var29;
        label255: {
            int var12;
            long var25;
            try {
                var23 = true;
                int var8 = var2 + var3;

                for(int var9 = var2; var9 < var8 && var7 < IOV_MAX; ++var9) {
                    ByteBuffer var10 = var1[var9];
                    int var11 = var10.position();
                    var12 = var10.limit();

                    assert var11 <= var12;

                    int var13 = var11 <= var12?var12 - var11:0;
                    if(var13 > 0) {
                        var5.setBuffer(var7, var10, var11, var13);
                        if(!(var10 instanceof DirectBuffer)) {
                            ByteBuffer var14 = Util.getTemporaryDirectBuffer(var13);
                            var14.put(var10);
                            var14.flip();
                            var5.setShadow(var7, var14);
                            var10.position(var11);
                            var10 = var14;
                            var11 = var14.position();
                        }

                        var5.putBase(var7, ((DirectBuffer)var10).address() + (long)var11);
                        var5.putLen(var7, (long)var13);
                        ++var7;
                    }
                }

                if(var7 != 0) {
                    var25 = var4.writev(var0, var5.address, var7);
                    long var26 = var25;

                    for(int var28 = 0; var28 < var7; ++var28) {
                        ByteBuffer var15;
                        if(var26 > 0L) {
                            var15 = var5.getBuffer(var28);
                            var16 = var5.getPosition(var28);
                            int var17 = var5.getRemaining(var28);
                            int var18 = var26 > (long)var17?var17:(int)var26;
                            var15.position(var16 + var18);
                            var26 -= (long)var18;
                        }

                        var15 = var5.getShadow(var28);
                        if(var15 != null) {
                            Util.offerLastTemporaryDirectBuffer(var15);
                        }

                        var5.clearRefs(var28);
                    }

                    var6 = true;
                    var29 = var25;
                    var23 = false;
                    break label255;
                }

                var25 = 0L;
                var23 = false;
            } finally {
                if(var23) {
                    if(!var6) {
                        for(int var20 = 0; var20 < var7; ++var20) {
                            ByteBuffer var21 = var5.getShadow(var20);
                            if(var21 != null) {
                                Util.offerLastTemporaryDirectBuffer(var21);
                            }

                            var5.clearRefs(var20);
                        }
                    }

                }
            }

            if(!var6) {
                for(var12 = 0; var12 < var7; ++var12) {
                    ByteBuffer var27 = var5.getShadow(var12);
                    if(var27 != null) {
                        Util.offerLastTemporaryDirectBuffer(var27);
                    }

                    var5.clearRefs(var12);
                }
            }

            return var25;
        }

        if(!var6) {
            for(var16 = 0; var16 < var7; ++var16) {
                ByteBuffer var30 = var5.getShadow(var16);
                if(var30 != null) {
                    Util.offerLastTemporaryDirectBuffer(var30);
                }

                var5.clearRefs(var16);
            }
        }

        return var29;
    }
    /**不管ByteBuffer是heap还是direct最后都是用DirectBuffer读取*/
    static int read(FileDescriptor var0, ByteBuffer var1, long var2, NativeDispatcher var4) throws IOException {
        if(var1.isReadOnly()) {
            throw new IllegalArgumentException("Read-only buffer");
        } else if(var1 instanceof DirectBuffer) {
            /**如果是DirectBuffer，直接去读*/
            return readIntoNativeBuffer(var0, var1, var2, var4);
        } else {
            /**否则，获取缓存的DirectBuffer*/
            ByteBuffer var5 = Util.getTemporaryDirectBuffer(var1.remaining());

            int var7;
            try {
                /**读取*/
                int var6 = readIntoNativeBuffer(var0, var5, var2, var4);
                var5.flip();
                if(var6 > 0) {
                    /**var5 的position-limit put到var1*/
                    var1.put(var5);
                }

                var7 = var6;
            } finally {
                /**释放掉DirectBufferCache中的第一个'*/
                Util.offerFirstTemporaryDirectBuffer(var5);
            }

            return var7;
        }
    }
    /**@param var2 判断-1 or 其他*/
    private static int readIntoNativeBuffer(FileDescriptor var0, ByteBuffer var1, long var2, NativeDispatcher var4) throws IOException {
        int var5 = var1.position();
        int var6 = var1.limit();

        assert var5 <= var6;
        /**ByteBuffer剩余的数量*/
        int var7 = var5 <= var6?var6 - var5:0;
        if(var7 == 0) {
            return 0;
        } else {
            boolean var8 = false;
            int var9;
            if(var2 != -1L) {
                var9 = var4.pread(var0, ((DirectBuffer)var1).address() + (long)var5, var7, var2);
            } else {
                var9 = var4.read(var0, ((DirectBuffer)var1).address() + (long)var5, var7);
            }

            if(var9 > 0) {
                /**设置var1的position*/
                var1.position(var5 + var9);
            }

            return var9;
        }
    }

    static long read(FileDescriptor var0, ByteBuffer[] var1, NativeDispatcher var2) throws IOException {
        return read(var0, var1, 0, var1.length, var2);
    }

    static long read(FileDescriptor var0, ByteBuffer[] var1, int var2, int var3, NativeDispatcher var4) throws IOException {
        IOVecWrapper var5 = IOVecWrapper.get(var3);
        boolean var6 = false;
        int var7 = 0;
        boolean var24 = false;

        long var30;
        label271: {
            int var12;
            long var26;
            try {
                var24 = true;
                int var8 = var2 + var3;

                for(int var9 = var2; var9 < var8 && var7 < IOV_MAX; ++var9) {
                    ByteBuffer var10 = var1[var9];
                    if(var10.isReadOnly()) {
                        throw new IllegalArgumentException("Read-only buffer");
                    }

                    int var11 = var10.position();
                    var12 = var10.limit();

                    assert var11 <= var12;

                    int var13 = var11 <= var12?var12 - var11:0;
                    if(var13 > 0) {
                        var5.setBuffer(var7, var10, var11, var13);
                        if(!(var10 instanceof DirectBuffer)) {
                            ByteBuffer var14 = Util.getTemporaryDirectBuffer(var13);
                            var5.setShadow(var7, var14);
                            var10 = var14;
                            var11 = var14.position();
                        }

                        var5.putBase(var7, ((DirectBuffer)var10).address() + (long)var11);
                        var5.putLen(var7, (long)var13);
                        ++var7;
                    }
                }

                if(var7 != 0) {
                    var26 = var4.readv(var0, var5.address, var7);
                    long var27 = var26;

                    for(int var29 = 0; var29 < var7; ++var29) {
                        ByteBuffer var15 = var5.getShadow(var29);
                        if(var27 > 0L) {
                            ByteBuffer var16 = var5.getBuffer(var29);
                            int var17 = var5.getRemaining(var29);
                            int var18 = var27 > (long)var17?var17:(int)var27;
                            if(var15 == null) {
                                int var19 = var5.getPosition(var29);
                                var16.position(var19 + var18);
                            } else {
                                var15.limit(var15.position() + var18);
                                var16.put(var15);
                            }

                            var27 -= (long)var18;
                        }

                        if(var15 != null) {
                            Util.offerLastTemporaryDirectBuffer(var15);
                        }

                        var5.clearRefs(var29);
                    }

                    var6 = true;
                    var30 = var26;
                    var24 = false;
                    break label271;
                }

                var26 = 0L;
                var24 = false;
            } finally {
                if(var24) {
                    if(!var6) {
                        for(int var21 = 0; var21 < var7; ++var21) {
                            ByteBuffer var22 = var5.getShadow(var21);
                            if(var22 != null) {
                                Util.offerLastTemporaryDirectBuffer(var22);
                            }

                            var5.clearRefs(var21);
                        }
                    }

                }
            }

            if(!var6) {
                for(var12 = 0; var12 < var7; ++var12) {
                    ByteBuffer var28 = var5.getShadow(var12);
                    if(var28 != null) {
                        Util.offerLastTemporaryDirectBuffer(var28);
                    }

                    var5.clearRefs(var12);
                }
            }

            return var26;
        }

        if(!var6) {
            for(int var31 = 0; var31 < var7; ++var31) {
                ByteBuffer var32 = var5.getShadow(var31);
                if(var32 != null) {
                    Util.offerLastTemporaryDirectBuffer(var32);
                }

                var5.clearRefs(var31);
            }
        }

        return var30;
    }

    public static FileDescriptor newFD(int var0) {
        FileDescriptor var1 = new FileDescriptor();
        setfdVal(var1, var0);
        return var1;
    }

    static native boolean randomBytes(byte[] var0);

    static native long makePipe(boolean var0);

    static native boolean drain(int var0) throws IOException;

    public static native void configureBlocking(FileDescriptor var0, boolean var1) throws IOException;

    public static native int fdVal(FileDescriptor var0);

    static native void setfdVal(FileDescriptor var0, int var1);

    static native int fdLimit();

    static native int iovMax();

    static native void initIDs();

    public static void load() {
    }

    static {
        AccessController.doPrivileged(new PrivilegedAction() {
            public Void run() {
                System.loadLibrary("net");
                System.loadLibrary("nio");
                return null;
            }
        });
        initIDs();
        IOV_MAX = iovMax();
    }
}

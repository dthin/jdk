/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.io;

/**
 * A piped input stream should be connected
 * to a piped output stream; the piped  input
 * stream then provides whatever data bytes
 * are written to the piped output  stream.
 * Typically, data is read from a <code>PipedInputStream</code>
 * object by one thread  and data is written
 * to the corresponding <code>PipedOutputStream</code>
 * by some  other thread. Attempting to use
 * both objects from a single thread is not
 * recommended, as it may deadlock the thread.
 * The piped input stream contains a buffer,
 * decoupling read operations from write operations,
 * within limits.
 * A pipe is said to be <a name="BROKEN"> <i>broken</i> </a> if a
 * thread that was providing data bytes to the connected
 * piped output stream is no longer alive.
 *
 * @author James Gosling
 * @see java.io.PipedOutputStream
 * @since JDK1.0
 * <p>
 *
 *                 PipedInputStream
 *
 *                 最好不要在单线程环境中使用，会发生死锁
 *                 内部采用循环队列缓存
 *                 读时，写线程挂掉，则报错，同理，写时，读线程挂掉则报错
 *                 缓存为空时，2秒内没有写入，报错
 *
 *
 */
public class PipedInputStream extends InputStream {
    /** */
    boolean closedByWriter = false;

    volatile boolean closedByReader = false;
    /**
     * 是否连接
     */
    boolean connected = false;

        /* REMIND: identification of the read and write sides needs to be
           more sophisticated.  Either using thread groups (but what about
           pipes within a thread?) or using finalization (but it may be a
           long time until the next GC). */
    /**
     * 读线程
     */
    Thread readSide;
    /**
     * 写线程
     */
    Thread writeSide;
    /**
     * 缓冲区默认大小
     */
    private static final int DEFAULT_PIPE_SIZE = 1024;

    /**
     * The default size of the pipe's circular input buffer.
     *
     * @since JDK1.1
     */
    // This used to be a constant before the pipe size was allowed
    // to change. This field will continue to be maintained
    // for backward compatibility.
    protected static final int PIPE_SIZE = DEFAULT_PIPE_SIZE;

    /**
     * The circular buffer into which incoming data is placed.
     *
     * @since JDK1.1
     *
     * 缓冲区，循环队列
     */
    protected byte buffer[];

    /**
     * The index of the position in the circular buffer at which the
     * next byte of data will be stored when received from the connected
     * piped output stream. <code>in&lt;0</code> implies the buffer is empty,
     * <code>in==out</code> implies the buffer is full
     *
     * @since JDK1.1
     *
     * 写的位置，为-1表示缓冲区读完（缓冲区为空）
     * in=out表示缓冲区已满
     */
    protected int in = -1;

    /**
     * The index of the position in the circular buffer at which the next
     * byte of data will be read by this piped input stream.
     *
     * @since JDK1.1
     *
     * 读的位置
     */
    protected int out = 0;

    /**
     * Creates a <code>PipedInputStream</code> so
     * that it is connected to the piped output
     * stream <code>src</code>. Data bytes written
     * to <code>src</code> will then be  available
     * as input from this stream.
     *
     * @param src the stream to connect to.
     * @throws IOException if an I/O error occurs.
     */
    public PipedInputStream(PipedOutputStream src) throws IOException {
        this(src, DEFAULT_PIPE_SIZE);
    }

    /**
     * Creates a <code>PipedInputStream</code> so that it is
     * connected to the piped output stream
     * <code>src</code> and uses the specified pipe size for
     * the pipe's buffer.
     * Data bytes written to <code>src</code> will then
     * be available as input from this stream.
     *
     * @param src      the stream to connect to.
     * @param pipeSize the size of the pipe's buffer.
     * @throws IOException              if an I/O error occurs.
     * @throws IllegalArgumentException if {@code pipeSize <= 0}.
     * @since 1.6
     */
    public PipedInputStream(PipedOutputStream src, int pipeSize)
            throws IOException {
        initPipe(pipeSize);
        connect(src);
    }

    /**
     * Creates a <code>PipedInputStream</code> so
     * that it is not yet {@linkplain #connect(java.io.PipedOutputStream)
     * connected}.
     * It must be {@linkplain java.io.PipedOutputStream#connect(
     *java.io.PipedInputStream) connected} to a
     * <code>PipedOutputStream</code> before being used.
     */
    public PipedInputStream() {
        initPipe(DEFAULT_PIPE_SIZE);
    }

    /**
     * Creates a <code>PipedInputStream</code> so that it is not yet
     * {@linkplain #connect(java.io.PipedOutputStream) connected} and
     * uses the specified pipe size for the pipe's buffer.
     * It must be {@linkplain java.io.PipedOutputStream#connect(
     *java.io.PipedInputStream)
     * connected} to a <code>PipedOutputStream</code> before being used.
     *
     * @param pipeSize the size of the pipe's buffer.
     * @throws IllegalArgumentException if {@code pipeSize <= 0}.
     * @since 1.6
     */
    public PipedInputStream(int pipeSize) {
        initPipe(pipeSize);
    }

    private void initPipe(int pipeSize) {
        if (pipeSize <= 0) {
            throw new IllegalArgumentException("Pipe Size <= 0");
        }
        buffer = new byte[pipeSize];
    }

    /**
     * Causes this piped input stream to be connected
     * to the piped  output stream <code>src</code>.
     * If this object is already connected to some
     * other piped output  stream, an <code>IOException</code>
     * is thrown.
     * <p>
     * If <code>src</code> is an
     * unconnected piped output stream and <code>snk</code>
     * is an unconnected piped input stream, they
     * may be connected by either the call:
     * <p>
     * <pre><code>snk.connect(src)</code> </pre>
     *
     * or the call:
     *
     * <pre><code>src.connect(snk)</code> </pre>
     *
     * The two calls have the same effect.
     *
     * @param src The piped output stream to connect to.
     * @throws IOException if an I/O error occurs.
     */
    public void connect(PipedOutputStream src) throws IOException {
        src.connect(this);
    }

    /**
     * Receives a byte of data.  This method will block if no input is
     * available.
     *
     * @param b the byte being received
     * @throws IOException If the pipe is <a href="#BROKEN"> <code>broken</code></a>,
     *                     {@link #connect(java.io.PipedOutputStream) unconnected},
     *                     closed, or if an I/O error occurs.
     * @since JDK1.1
     *
     * 接收单个byte，PipedOutputStream  write(int)方法调用
     * @see PipedOutputStream#write(int)
     */
    protected synchronized void receive(int b) throws IOException {
        /**例行检查*/
        checkStateForReceive();
        /**设置写线程*/
        writeSide = Thread.currentThread();
        if (in == out)
            /**表示缓冲区已经满了，唤醒所有线程，直到有线程读取缓冲区内容*/
            awaitSpace();
        if (in < 0) {
            in = 0;
            out = 0;
        }
        buffer[in++] = (byte) (b & 0xFF);
        if (in >= buffer.length) {
            in = 0;
        }
    }

    /**
     * Receives data into an array of bytes.  This method will
     * block until some input is available.
     *
     * @param b   the buffer into which the data is received
     * @param off the start offset of the data
     * @param len the maximum number of bytes received
     * @throws IOException If the pipe is <a href="#BROKEN"> broken</a>,
     *                     {@link #connect(java.io.PipedOutputStream) unconnected},
     *                     closed,or if an I/O error occurs.
     * 接收byte数组
     */
    synchronized void receive(byte b[], int off, int len) throws IOException {
        checkStateForReceive();
        writeSide = Thread.currentThread();
        int bytesToTransfer = len;
        /**
         * 1、缓冲区剩余可写的数量>要写入的数量
         *    写入（要写入的数量）
         * 2、缓冲区剩余可写的数量<要写入的数量
         *    写入（可写的数量）
         *    阻塞此写线程，唤醒其他线程，直到有读线程读取后，在写入
         *    循环上述步骤，直到全部写入
         * */
        while (bytesToTransfer > 0) {
            if (in == out)
                awaitSpace();
            /**缓冲区剩余可写入的数量*/
            int nextTransferAmount = 0;
            if (out < in) {
                nextTransferAmount = buffer.length - in;
            } else if (in < out) {
                if (in == -1) {
                    in = out = 0;
                    nextTransferAmount = buffer.length - in;
                } else {
                    nextTransferAmount = out - in;
                }
            }
            if (nextTransferAmount > bytesToTransfer)
                nextTransferAmount = bytesToTransfer;

            assert (nextTransferAmount > 0);
            System.arraycopy(b, off, buffer, in, nextTransferAmount);
            bytesToTransfer -= nextTransferAmount;
            off += nextTransferAmount;
            in += nextTransferAmount;
            if (in >= buffer.length) {
                in = 0;
            }
        }
    }
    /**检查是否连接，是否close，是否读线程死亡*/
    private void checkStateForReceive() throws IOException {
        if (!connected) {
            throw new IOException("Pipe not connected");
        } else if (closedByWriter || closedByReader) {
            throw new IOException("Pipe closed");
        } else if (readSide != null && !readSide.isAlive()) {
            throw new IOException("Read end dead");
        }
    }
    /**唤醒其他线程（读，写），阻塞自己1s，直到有线程读取缓冲区内容，*/
    private void awaitSpace() throws IOException {
        while (in == out) {
            checkStateForReceive();

            /* full: kick any waiting readers */
            notifyAll();
            try {
                wait(1000);
            } catch (InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
    }

    /**
     * Notifies all waiting threads that the last byte of data has been
     * received.
     *
     * 关闭写流
     * @see PipedOutputStream#close()
     */
    synchronized void receivedLast() {
        closedByWriter = true;
        notifyAll();
    }

    /**
     * Reads the next byte of data from this piped input stream. The
     * value byte is returned as an <code>int</code> in the range
     * <code>0</code> to <code>255</code>.
     * This method blocks until input data is available, the end of the
     * stream is detected, or an exception is thrown.
     *
     * @return the next byte of data, or <code>-1</code> if the end of the
     * stream is reached.
     * @throws IOException if the pipe is
     *                     {@link #connect(java.io.PipedOutputStream) unconnected},
     *                     <a href="#BROKEN"> <code>broken</code></a>, closed,
     *                     or if an I/O error occurs.
     *   读取单个字节，返回-1表示关闭
     */
    public synchronized int read() throws IOException {
        if (!connected) {
            throw new IOException("Pipe not connected");
        } else if (closedByReader) {
            throw new IOException("Pipe closed");
        } else if (writeSide != null && !writeSide.isAlive()
                && !closedByWriter && (in < 0)) {
            /**如果缓冲区为空且写入线程挂了*/
            throw new IOException("Write end dead");
        }
        /**存入读线程*/
        readSide = Thread.currentThread();
        /**等待写线程重试时间2s*/
        int trials = 2;
        while (in < 0) {/**in<0表示缓存区为空*/
            if (closedByWriter) {
                /* closed by writer, return EOF */
                return -1;
            }
            if ((writeSide != null) && (!writeSide.isAlive()) && (--trials < 0)) {
                throw new IOException("Pipe broken");
            }
            /* might be a writer waiting */
            /**唤醒所有线程，直到调用写线程*/
            notifyAll();
            try {
                wait(1000);
            } catch (InterruptedException ex) {
                throw new java.io.InterruptedIOException();
            }
        }
        int ret = buffer[out++] & 0xFF;
        if (out >= buffer.length) {
            out = 0;
        }
        if (in == out) {
            /* now empty */
            in = -1;
        }

        return ret;
    }

    /**
     * Reads up to <code>len</code> bytes of data from this piped input
     * stream into an array of bytes. Less than <code>len</code> bytes
     * will be read if the end of the data stream is reached or if
     * <code>len</code> exceeds the pipe's buffer size.
     * If <code>len </code> is zero, then no bytes are read and 0 is returned;
     * otherwise, the method blocks until at least 1 byte of input is
     * available, end of the stream has been detected, or an exception is
     * thrown.
     *
     * @param b   the buffer into which the data is read.
     * @param off the start offset in the destination array <code>b</code>
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or
     * <code>-1</code> if there is no more data because the end of
     * the stream has been reached.
     * @throws NullPointerException      If <code>b</code> is <code>null</code>.
     * @throws IndexOutOfBoundsException If <code>off</code> is negative,
     *                                   <code>len</code> is negative, or <code>len</code> is greater than
     *                                   <code>b.length - off</code>
     * @throws IOException               if the pipe is <a href="#BROKEN"> <code>broken</code></a>,
     *                                   {@link #connect(java.io.PipedOutputStream) unconnected},
     *                                   closed, or if an I/O error occurs.
     *
     */
    public synchronized int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        /* possibly wait on the first character */
        int c = read();
        if (c < 0) {
            return -1;
        }
        b[off] = (byte) c;
        int rlen = 1;
        /**如果缓存都读完了，或者读了len个*/
        while ((in >= 0) && (len > 1)) {

            int available;

            if (in > out) {
                available = Math.min((buffer.length - out), (in - out));
            } else {
                available = buffer.length - out;
            }

            // A byte is read beforehand outside the loop
            if (available > (len - 1)) {
                available = len - 1;
            }
            System.arraycopy(buffer, out, b, off + rlen, available);
            out += available;
            rlen += available;
            len -= available;

            if (out >= buffer.length) {
                out = 0;
            }
            if (in == out) {
                /* now empty */
                in = -1;
            }
        }
        return rlen;
    }

    /**
     * Returns the number of bytes that can be read from this input
     * stream without blocking.
     *
     * @return the number of bytes that can be read from this input stream
     * without blocking, or {@code 0} if this input stream has been
     * closed by invoking its {@link #close()} method, or if the pipe
     * is {@link #connect(java.io.PipedOutputStream) unconnected}, or
     * <a href="#BROKEN"> <code>broken</code></a>.
     * @throws IOException if an I/O error occurs.
     * @since JDK1.0.2
     *
     * 返回缓冲区可用（读）的数量
     */
    public synchronized int available() throws IOException {
        if (in < 0)
            return 0;
        else if (in == out)
            return buffer.length;
        else if (in > out)
            return in - out;
        else
            return in + buffer.length - out;
    }

    /**
     * Closes this piped input stream and releases any system resources
     * associated with the stream.
     *
     * @throws IOException if an I/O error occurs.
     * 关闭读流
     */
    public void close() throws IOException {
        closedByReader = true;
        synchronized (this) {
            in = -1;
        }
    }
}

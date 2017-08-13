//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package sun.nio.cs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;

public class StreamEncoder extends Writer {
    private static final int DEFAULT_BYTE_BUFFER_SIZE = 8192;
    private volatile boolean isOpen;
    private Charset cs;
    private CharsetEncoder encoder;
    private ByteBuffer bb;
    private final OutputStream out;
    private WritableByteChannel ch;
    private boolean haveLeftoverChar;
    private char leftoverChar;
    private CharBuffer lcb;

    private void ensureOpen() throws IOException {
        if(!this.isOpen) {
            throw new IOException("Stream closed");
        }
    }

    public static StreamEncoder forOutputStreamWriter(OutputStream var0, Object var1, String var2) throws UnsupportedEncodingException {
        String var3 = var2;
        if(var2 == null) {
            var3 = Charset.defaultCharset().name();
        }

        try {
            if(Charset.isSupported(var3)) {
                return new StreamEncoder(var0, var1, Charset.forName(var3));
            }
        } catch (IllegalCharsetNameException var5) {
            ;
        }

        throw new UnsupportedEncodingException(var3);
    }

    public static StreamEncoder forOutputStreamWriter(OutputStream var0, Object var1, Charset var2) {
        return new StreamEncoder(var0, var1, var2);
    }

    public static StreamEncoder forOutputStreamWriter(OutputStream var0, Object var1, CharsetEncoder var2) {
        return new StreamEncoder(var0, var1, var2);
    }

    public static StreamEncoder forEncoder(WritableByteChannel var0, CharsetEncoder var1, int var2) {
        return new StreamEncoder(var0, var1, var2);
    }

    public String getEncoding() {
        return this.isOpen()?this.encodingName():null;
    }

    public void flushBuffer() throws IOException {
        Object var1 = this.lock;
        synchronized(this.lock) {
            if(this.isOpen()) {
                this.implFlushBuffer();
            } else {
                throw new IOException("Stream closed");
            }
        }
    }

    public void write(int var1) throws IOException {
        char[] var2 = new char[]{(char)var1};
        this.write((char[])var2, 0, 1);
    }

    public void write(char[] var1, int var2, int var3) throws IOException {
        Object var4 = this.lock;
        synchronized(this.lock) {
            this.ensureOpen();
            if(var2 >= 0 && var2 <= var1.length && var3 >= 0 && var2 + var3 <= var1.length && var2 + var3 >= 0) {
                if(var3 != 0) {
                    this.implWrite(var1, var2, var3);
                }
            } else {
                throw new IndexOutOfBoundsException();
            }
        }
    }

    public void write(String var1, int var2, int var3) throws IOException {
        if(var3 < 0) {
            throw new IndexOutOfBoundsException();
        } else {
            char[] var4 = new char[var3];
            var1.getChars(var2, var2 + var3, var4, 0);
            this.write((char[])var4, 0, var3);
        }
    }

    public void flush() throws IOException {
        Object var1 = this.lock;
        synchronized(this.lock) {
            this.ensureOpen();
            this.implFlush();
        }
    }

    public void close() throws IOException {
        Object var1 = this.lock;
        synchronized(this.lock) {
            if(this.isOpen) {
                this.implClose();
                this.isOpen = false;
            }
        }
    }

    private boolean isOpen() {
        return this.isOpen;
    }

    private StreamEncoder(OutputStream var1, Object var2, Charset var3) {
        this(var1, var2, var3.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE));
    }

    private StreamEncoder(OutputStream var1, Object var2, CharsetEncoder var3) {
        super(var2);
        this.isOpen = true;
        this.haveLeftoverChar = false;
        this.lcb = null;
        this.out = var1;
        this.ch = null;
        this.cs = var3.charset();
        this.encoder = var3;
        if(this.ch == null) {
            this.bb = ByteBuffer.allocate(8192);
        }

    }

    private StreamEncoder(WritableByteChannel var1, CharsetEncoder var2, int var3) {
        this.isOpen = true;
        this.haveLeftoverChar = false;
        this.lcb = null;
        this.out = null;
        this.ch = var1;
        this.cs = var2.charset();
        this.encoder = var2;
        this.bb = ByteBuffer.allocate(var3 < 0?8192:var3);
    }

    private void writeBytes() throws IOException {
        this.bb.flip();
        int var1 = this.bb.limit();
        int var2 = this.bb.position();

        assert var2 <= var1;

        int var3 = var2 <= var1?var1 - var2:0;
        if(var3 > 0) {
            if(this.ch != null) {
                assert this.ch.write(this.bb) == var3 : var3;
            } else {
                this.out.write(this.bb.array(), this.bb.arrayOffset() + var2, var3);
            }
        }

        this.bb.clear();
    }

    private void flushLeftoverChar(CharBuffer var1, boolean var2) throws IOException {
        if(this.haveLeftoverChar || var2) {
            if(this.lcb == null) {
                this.lcb = CharBuffer.allocate(2);
            } else {
                this.lcb.clear();
            }

            if(this.haveLeftoverChar) {
                this.lcb.put(this.leftoverChar);
            }

            if(var1 != null && var1.hasRemaining()) {
                this.lcb.put(var1.get());
            }

            this.lcb.flip();

            while(this.lcb.hasRemaining() || var2) {
                CoderResult var3 = this.encoder.encode(this.lcb, this.bb, var2);
                if(var3.isUnderflow()) {
                    if(this.lcb.hasRemaining()) {
                        this.leftoverChar = this.lcb.get();
                        if(var1 != null && var1.hasRemaining()) {
                            this.flushLeftoverChar(var1, var2);
                        }

                        return;
                    }
                    break;
                }

                if(var3.isOverflow()) {
                    assert this.bb.position() > 0;

                    this.writeBytes();
                } else {
                    var3.throwException();
                }
            }

            this.haveLeftoverChar = false;
        }
    }

    void implWrite(char[] var1, int var2, int var3) throws IOException {
        CharBuffer var4 = CharBuffer.wrap(var1, var2, var3);
        if(this.haveLeftoverChar) {
            this.flushLeftoverChar(var4, false);
        }

        while(var4.hasRemaining()) {
            CoderResult var5 = this.encoder.encode(var4, this.bb, false);
            if(var5.isUnderflow()) {
                assert var4.remaining() <= 1 : var4.remaining();

                if(var4.remaining() == 1) {
                    this.haveLeftoverChar = true;
                    this.leftoverChar = var4.get();
                }
                break;
            }

            if(var5.isOverflow()) {
                assert this.bb.position() > 0;

                this.writeBytes();
            } else {
                var5.throwException();
            }
        }

    }

    void implFlushBuffer() throws IOException {
        if(this.bb.position() > 0) {
            this.writeBytes();
        }

    }

    void implFlush() throws IOException {
        this.implFlushBuffer();
        if(this.out != null) {
            this.out.flush();
        }

    }

    void implClose() throws IOException {
        this.flushLeftoverChar((CharBuffer)null, true);

        try {
            while(true) {
                CoderResult var1 = this.encoder.flush(this.bb);
                if(var1.isUnderflow()) {
                    if(this.bb.position() > 0) {
                        this.writeBytes();
                    }

                    if(this.ch != null) {
                        this.ch.close();
                    } else {
                        this.out.close();
                    }

                    return;
                }

                if(var1.isOverflow()) {
                    assert this.bb.position() > 0;

                    this.writeBytes();
                } else {
                    var1.throwException();
                }
            }
        } catch (IOException var2) {
            this.encoder.reset();
            throw var2;
        }
    }

    String encodingName() {
        return this.cs instanceof HistoricallyNamedCharset?((HistoricallyNamedCharset)this.cs).historicalName():this.cs.name();
    }
}

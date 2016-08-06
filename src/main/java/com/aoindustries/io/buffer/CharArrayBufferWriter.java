/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2010, 2011, 2012, 2013, 2015  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-io-buffer.
 *
 * ao-io-buffer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-io-buffer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-io-buffer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.io.buffer;

import com.aoindustries.io.AoCharArrayWriter;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.logging.Logger;

/**
 * Writes to a CharArrayBuffer.
 *
 * This class is not thread safe.
 *
 * @see  SegmentedBufferedWriter  for a possibly more efficient implementation.
 *
 * @author  AO Industries, Inc.
 */
public class CharArrayBufferWriter extends BufferWriter {

    private static final Logger logger = Logger.getLogger(CharArrayBufferWriter.class.getName());

	/**
	 * The length of the writer is the sum of the data written to the buffer.
	 * Once closed, this length will not be modified.
	 */
    private long length;

	/**
	 * The buffer used to capture data before switching to file-backed storage.
	 * Once closed, this buffer will not be modified.
	 */
    final private AoCharArrayWriter buffer;

	/**
	 * Once closed, no further information may be written.
	 * Manipulations are only active once closed.
	 */
	private boolean isClosed = false;

	public CharArrayBufferWriter(int initialSize) {
        this.length = 0;
        this.buffer = new AoCharArrayWriter(initialSize);
    }

    @Override
    public void write(int c) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        buffer.write(c);
        length += 1;
    }

    @Override
    public void write(char cbuf[]) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        buffer.write(cbuf);
        length += cbuf.length;
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        buffer.write(cbuf, off, len);
        length += len;
    }

    @Override
    public void write(String str) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        buffer.write(str);
        length += str.length();
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        buffer.write(str, off, len);
        length += len;
    }

    @Override
    public CharArrayBufferWriter append(CharSequence csq) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        buffer.append(csq);
        length += csq.length();
        return this;
    }

    @Override
    public CharArrayBufferWriter append(CharSequence csq, int start, int end) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        buffer.append(csq, start, end);
        length += (end-start);
        return this;
    }

    @Override
    public CharArrayBufferWriter append(char c) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        buffer.append(c);
        length++;
        return this;
    }

    @Override
    public void flush() {
		buffer.flush();
    }

	//private static long biggest = 0;
    @Override
    public void close() throws IOException {
		buffer.close();
		isClosed = true;
		/*
		long heap = buffer.getInternalCharArray().length * Character.SIZE;
		if(heap>biggest) {
			biggest = heap;
			System.err.println("CharArrayBufferWriter: Biggest heap: " + biggest);
		}*/
    }

	@Override
    public long getLength() {
        return length;
    }

    @Override
    public String toString() {
		return "CharArrayBufferWriter(length=" + length + ")";
    }

	// The result is cached after first created
	private BufferResult result;

	@Override
	public BufferResult getResult() throws IllegalStateException {
		if(!isClosed) throw new IllegalStateException();
		if(result==null) {
			assert length == buffer.size();
			if(length==0) {
				result = EmptyResult.getInstance();
			} else {
				result = new CharArrayBufferResult(buffer, 0, buffer.size());
			}
		}
		return result;
	}
}

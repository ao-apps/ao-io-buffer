/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2010, 2011, 2012, 2013, 2015, 2016, 2017, 2019, 2020  AO Industries, Inc.
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

import com.aoindustries.lang.EmptyArrays;
import com.aoindustries.util.BufferManager;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;

/**
 * Writes to a set of internally managed buffers.  When possible, the buffers are reused.
 * <p>
 * Maximum length is 2 ^ 30 characters (about 1 billion).
 * When this limit is insufficient, consider using along with
 * {@link AutoTempFileWriter}.
 * </p>
 * <p>
 * This class is not thread safe.
 * </p>
 *
 * @see  SegmentedWriter  for a possibly more efficient implementation.
 *
 * @author  AO Industries, Inc.
 */
// TODO: Remember the first string written, and use it for toString in order to maintain string identity for single-string writes from a resource bundle source
// TODO: for in-context translator support (and perhaps a tiny performance boost)
// TODO: This might be used with a StringResult (shared by single-segment SegmentedWriter, too)
public class CharArrayBufferWriter extends BufferWriter {

	/**
	 * The maximum buffer length is 2^30 due to array length limitations.
	 */
	private static final int MAX_LENGTH = 1 << 30;

	/**
	 * Any buffer under this size will be copied into a new array on close in
	 * order to reduce memory consumption and recycle the initial buffer.
	 */
	private static final int COPY_THEN_RECYCLE_LIMIT = BufferManager.BUFFER_SIZE / 8;

	/**
	 * The length of the writer is the sum of the data written to the buffer.
	 * Once closed, this length will not be modified.
	 */
	private int length = 0;

	/**
	 * The buffer used to capture data.
	 * Once closed, this buffer will not be modified.
	 *
	 * TODO: Consider a set of buffers to avoid copying on resize.
	 *       This would also allow beyond 32-bit limit.
	 */
	private char[] buffer = EmptyArrays.EMPTY_CHAR_ARRAY;

	/**
	 * Once closed, no further information may be written.
	 * Manipulations are only active once closed.
	 */
	private boolean isClosed = false;

	public CharArrayBufferWriter() {
	}

	/**
	 * Grows as-needed to fit the provided new capacity.
	 *
	 * @returns the possibly new buffer.
	 */
	private char[] getBuffer(int additional) throws IOException {
		long newLen = (long)length + additional;
		if(newLen > MAX_LENGTH) throw new IOException("Maximum buffer length is " + MAX_LENGTH + ", " + newLen + " requested");
		char[] buf = this.buffer;
		int bufLen = buf.length;
		if(newLen > bufLen) {
			// Find the next power of two that will hold all of the contents
			int newBufLen = (bufLen == 0) ? BufferManager.BUFFER_SIZE : (bufLen << 1);
			while(newBufLen < newLen) {
				newBufLen <<= 1;
			}
			char[] newBuf =
				(newBufLen == BufferManager.BUFFER_SIZE)
				? BufferManager.getChars()
				: new char[newBufLen];
			System.arraycopy(buf, 0, newBuf, 0, length);
			// Recycle buffer
			if(bufLen == BufferManager.BUFFER_SIZE) {
				BufferManager.release(buf, false);
			}
			buf = newBuf;
			this.buffer = buf;
		}
		return buf;
	}

	@Override
	public void write(int c) throws IOException {
		if(isClosed) throw new ClosedChannelException();
		getBuffer(1)[length++] = (char)c;
	}

	@Override
	public void write(char cbuf[], int off, int len) throws IOException {
		if(isClosed) throw new ClosedChannelException();
		if(len > 0) {
			char[] buf = getBuffer(len);
			System.arraycopy(cbuf, off, buf, length, len);
			length += len;
		}
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		if(isClosed) throw new ClosedChannelException();
		if(len > 0) {
			char[] buf = getBuffer(len);
			str.getChars(off, off + len, buf, length);
			length += len;
		}
	}

	@Override
	public CharArrayBufferWriter append(CharSequence csq) throws IOException {
		super.append(csq);
		return this;
	}

	@Override
	public CharArrayBufferWriter append(CharSequence csq, int start, int end) throws IOException {
		if(csq == null) {
			write("null");
		} else {
			if(csq instanceof String) {
				// Avoid subSequence which copies characters in Java 1.8+
				write((String)csq, start, end - start);
			} else {
		        write(csq.subSequence(start, end).toString());
			}
		}
		return this;
	}

	@Override
	public CharArrayBufferWriter append(char c) throws IOException {
		super.append(c);
		return this;
	}

	@Override
	public void flush() throws IOException {
		if(isClosed) throw new ClosedChannelException();
		// Nothing to do
	}

	@Override
	public void close() {
		isClosed = true;
		int len = this.length;
		if(len > 0) {
			if(len <= COPY_THEN_RECYCLE_LIMIT) {
				char[] oldBuf = buffer;
				this.buffer = Arrays.copyOf(oldBuf, len);
				if(oldBuf.length == BufferManager.BUFFER_SIZE) {
					BufferManager.release(oldBuf, false);
				}
			}
		}
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
		if(result == null) {
			if(length == 0) {
				result = EmptyResult.getInstance();
			} else {
				result = new CharArrayBufferResult(buffer, 0, length);
			}
		}
		return result;
	}
}

/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015  AO Industries, Inc.
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

import com.aoindustries.io.TempFile;
import com.aoindustries.nio.charset.Charsets;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.ClosedChannelException;

/**
 * Writes to a temp file buffer.
 *
 * This class is not thread safe.
 *
 * @author  AO Industries, Inc.
 */
public class TempFileWriter extends BufferWriter {

	/**
	 * The length of the writer is the sum of the data written to the buffer.
	 * Once closed, this length will not be modified.
	 */
    private long length;

	/**
	 * Once closed, no further information may be written.
	 * Manipulations are only active once closed.
	 */
	private boolean isClosed = false;

	// The temp file is in UTF16-BE encoding
    private final TempFile tempFile;
    private Writer fileWriter;

	public TempFileWriter(TempFile tempFile) throws IOException {
        this.length = 0;
		this.tempFile = tempFile;
		this.fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile.getFile()), Charsets.UTF_16BE));
    }

    @Override
    public void write(int c) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        fileWriter.write(c);
        length++;
    }

    @Override
    public void write(char cbuf[]) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        fileWriter.write(cbuf);
        length += cbuf.length;
    }

    @Override
    public void write(char cbuf[], int off, int len) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        fileWriter.write(cbuf, off, len);
        length += len;
    }

    @Override
    public void write(String str) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        fileWriter.write(str);
        length += str.length();
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        fileWriter.write(str, off, len);
        length += len;
    }

    @Override
    public TempFileWriter append(CharSequence csq) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        fileWriter.append(csq);
        length += csq.length();
        return this;
    }

    @Override
    public TempFileWriter append(CharSequence csq, int start, int end) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        fileWriter.append(csq, start, end);
        length += (end-start);
        return this;
    }

    @Override
    public TempFileWriter append(char c) throws IOException {
		if(isClosed) throw new ClosedChannelException();
        fileWriter.append(c);
        length++;
        return this;
    }

    @Override
    public void flush() throws IOException {
        if(fileWriter!=null) fileWriter.flush();
    }

    @Override
    public void close() throws IOException {
        if(fileWriter!=null) {
            fileWriter.close();
            fileWriter = null;
		}
		isClosed = true;
    }

	@Override
    public long getLength() {
        return length;
    }

    @Override
    public String toString() {
		return "TempFileWriter(tempFile=\"" + tempFile.toString() + "\", length=" + length + ")";
    }

	// The result is cached after first created
	private BufferResult result;

	@Override
	public BufferResult getResult() throws IllegalStateException {
		if(!isClosed) throw new IllegalStateException();
		if(result==null) {
			if(length==0) {
				result = EmptyResult.getInstance();
			} else {
				result = new TempFileResult(tempFile, 0, length);
			}
		}
		return result;
	}
}

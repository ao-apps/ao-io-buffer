/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2010, 2011, 2012, 2013, 2015, 2016  AO Industries, Inc.
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
import com.aoindustries.io.TempFileList;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes to an initial buffer then switches to a temp file when the threshold is reached.
 *
 * This class is not thread safe.
 *
 * @author  AO Industries, Inc.
 */
public class AutoTempFileWriter extends BufferWriter {

	private static final Logger logger = Logger.getLogger(AutoTempFileWriter.class.getName());

	/**
	 * A reasonable default temp file threshold, currently 4 MB.
	 */
	public static final long DEFAULT_TEMP_FILE_THRESHOLD = 4L * 1024L * 1024L;

	private final TempFileList tempFileList;
	private final long tempFileThreshold;

	private BufferWriter buffer;

	/**
	 * Flags that we are currently writing to a temp file.
	 */
	private boolean isInitialBuffer;

	public AutoTempFileWriter(
		BufferWriter initialBuffer,
		TempFileList tempFileList,
		long tempFileThreshold
	) {
		this.tempFileList = tempFileList;
		this.tempFileThreshold = tempFileThreshold;
		this.buffer = initialBuffer;
		this.isInitialBuffer = !(initialBuffer instanceof TempFileWriter); // If already a temp file, no need to ever switch
	}

	/**
	 * Uses the default temp file threshold.
	 */
	public AutoTempFileWriter(BufferWriter initialBuffer, TempFileList tempFileList) {
		this(
			initialBuffer,
			tempFileList,
			DEFAULT_TEMP_FILE_THRESHOLD
		);
	}

	private void switchIfNeeded(long newLength) throws IOException {
		if(isInitialBuffer && newLength>=tempFileThreshold) {
			TempFile tempFile = tempFileList.createTempFile();
			if(logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Switching to temp file: {0}", tempFile);
			buffer.close();
			TempFileWriter tempFileWriter = new TempFileWriter(tempFile);
			buffer.getResult().writeTo(tempFileWriter);
			buffer = tempFileWriter;
			isInitialBuffer = false;
		}
	}

	@Override
	public void write(int c) throws IOException {
		if(isInitialBuffer) {
			switchIfNeeded(buffer.getLength() + 1);
		}
		buffer.write(c);
	}

	@Override
	public void write(char cbuf[]) throws IOException {
		if(isInitialBuffer) {
			switchIfNeeded(buffer.getLength() + cbuf.length);
		}
		buffer.write(cbuf);
	}

	@Override
	public void write(char cbuf[], int off, int len) throws IOException {
		if(isInitialBuffer) {
			switchIfNeeded(buffer.getLength() + len);
		}
		buffer.write(cbuf, off, len);
	}

	@Override
	public void write(String str) throws IOException {
		if(isInitialBuffer) {
			switchIfNeeded(buffer.getLength() + str.length());
		}
		buffer.write(str);
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		if(isInitialBuffer) {
			switchIfNeeded(buffer.getLength() + len);
		}
		buffer.write(str, off, len);
	}

	@Override
	public AutoTempFileWriter append(CharSequence csq) throws IOException {
		if(isInitialBuffer) {
			switchIfNeeded(buffer.getLength() + csq.length());
		}
		buffer.append(csq);
		return this;
	}

	@Override
	public AutoTempFileWriter append(CharSequence csq, int start, int end) throws IOException {
		if(isInitialBuffer) {
			switchIfNeeded(buffer.getLength() + (end-start));
		}
		buffer.append(csq, start, end);
		return this;
	}

	@Override
	public AutoTempFileWriter append(char c) throws IOException {
		if(isInitialBuffer) {
			switchIfNeeded(buffer.getLength() + 1);
		}
		buffer.append(c);
		return this;
	}

	@Override
	public void flush() throws IOException {
		buffer.flush();
	}

	@Override
	public void close() throws IOException {
		buffer.close();
	}

	@Override
	public long getLength() throws IOException {
		return buffer.getLength();
	}

	@Override
	public String toString() {
		return "AutoTempFileWriter(" + buffer + ", " + tempFileThreshold + ")";
	}

	@Override
	public BufferResult getResult() throws IllegalStateException, IOException {
		return buffer.getResult();
	}
}

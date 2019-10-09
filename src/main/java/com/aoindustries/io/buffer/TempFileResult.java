/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015, 2016, 2017, 2019  AO Industries, Inc.
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

import com.aoindustries.io.Encoder;
import com.aoindustries.io.EncoderWriter;
import com.aoindustries.io.IoUtils;
import com.aoindustries.tempfiles.TempFile;
import com.aoindustries.util.BufferManager;
import com.aoindustries.util.WrappedException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@inheritDoc}
 *
 * This class is not thread safe.
 *
 * TODO: Performance: When reading blocks, align to block boundary instead of remaining offset by start / writeStart.
 *
 * @author  AO Industries, Inc.
 */
public class TempFileResult implements BufferResult {

	private static final Logger logger = Logger.getLogger(TempFileResult.class.getName());

	private final TempFile tempFile;

	private final long start;
	private final long end;

	private final AtomicReference<BufferResult> trimmed = new AtomicReference<>();

	protected TempFileResult(
		TempFile tempFile,
		long start,
		long end
	) {
		this.tempFile = tempFile;
		this.start = start;
		this.end = end;
	}

	@Override
	public long getLength() {
		return end - start;
	}

	private String toStringCache;

	@Override
	public boolean isFastToString() {
		return toStringCache!=null;
	}

	@Override
	public String toString() {
		if(toStringCache==null) {
			try {
				if(logger.isLoggable(Level.INFO)) {
					logger.log(
						Level.INFO,
						"Creating String from temp file - benefits of buffering negated.",
						new Throwable("Stack Trace")
					);
				}
				final long length = end - start;
				if(length>Integer.MAX_VALUE) throw new RuntimeException("Buffer too large to convert to String: length="+length);
				StringBuilder sb = new StringBuilder((int)length);
				try (RandomAccessFile raf = new RandomAccessFile(tempFile.getFile(), "r")) {
					byte[] bytes = BufferManager.getBytes();
					try {
						long index = this.start;
						raf.seek(index<<1);
						while(index<end) {
							// Read a block
							long blockSizeLong = (end - index)<<1;
							int blockSize = blockSizeLong > BufferManager.BUFFER_SIZE ? BufferManager.BUFFER_SIZE : (int)blockSizeLong;
							assert (blockSize&1) == 0 : "Must be an even number for UTF-16 conversion";
							raf.readFully(bytes, 0, blockSize);
							// Convert to characters in sb
							for(int bpos=0; bpos<blockSize; bpos+=2) {
								sb.append(IoUtils.bufferToChar(bytes, bpos));
							}
							// Update location
							index += blockSize>>1;
						}
					} finally {
						BufferManager.release(bytes, false);
					}
				}
				assert sb.length()==length : "sb.length()!=length: "+sb.length()+"!="+length;
				toStringCache = sb.toString();
			} catch(IOException err) {
				throw new WrappedException(err);
			}
		}
		return toStringCache;
	}

	@Override
	public void writeTo(Writer out) throws IOException {
		writeToImpl(out, start, end);
	}

	@Override
	public void writeTo(Writer out, long off, long len) throws IOException {
		if((start + off + len) > end) throw new IndexOutOfBoundsException();
		writeToImpl(out, start + off, start + off + len);
	}

	@Override
	public void writeTo(Encoder encoder, Writer out) throws IOException {
		writeTo(
			encoder!=null
				? new EncoderWriter(encoder, out)
				: out
		);
	}

	@Override
	public void writeTo(Encoder encoder, Writer out, long off, long len) throws IOException {
		writeTo(
			encoder!=null
				? new EncoderWriter(encoder, out)
				: out,
			off,
			len
		);
	}

	/**
	 * Implementation of writeTo
	 *
	 * @param out
	 * @param writeStart  The absolute index to write from
	 * @param writeEnd    The absolute index one past last character to write
	 * @throws IOException 
	 */
	private void writeToImpl(Writer out, long writeStart, long writeEnd) throws IOException {
		try ( // TODO: If copying to another SegmentedBufferedWriter or AutoTempFileWriter, we have a chance here for disk-to-disk block level copying instead of going through all the conversions.
			RandomAccessFile raf = new RandomAccessFile(tempFile.getFile(), "r")) {
			byte[] bytes = BufferManager.getBytes();
			try {
				char[] chars = BufferManager.getChars();
				try {
					long index = writeStart;
					raf.seek(index<<1);
					while(index<writeEnd) {
						// Read a block
						long blockSizeLong = (writeEnd - index)<<1;
						int blockSize = blockSizeLong > BufferManager.BUFFER_SIZE ? BufferManager.BUFFER_SIZE : (int)blockSizeLong;
						assert (blockSize&1) == 0 : "Must be an even number for UTF-16 conversion";
						raf.readFully(bytes, 0, blockSize);
						// Convert to characters
						for(
							int bpos=0, cpos=0;
							bpos<blockSize;
							bpos+=2, cpos++
						) {
							chars[cpos] = IoUtils.bufferToChar(bytes, bpos);
						}
						// Write to output
						out.write(chars, 0, blockSize>>1);
						// Update location
						index += blockSize>>1;
					}
				} finally {
					BufferManager.release(chars, false);
				}
			} finally {
				BufferManager.release(bytes, false);
			}
		}
	}

	@Override
	public BufferResult trim() throws IOException {
		BufferResult _trimmed = this.trimmed.get();
		if(trimmed != null) return _trimmed;
		// Trim from temp file
		long newStart;
		long newEnd;
		try (RandomAccessFile raf = new RandomAccessFile(tempFile.getFile(), "r")) {
			newStart = this.start;
			// Skip past the beginning whitespace characters
			raf.seek(newStart<<1);
			while(newStart<end) {
				char ch = raf.readChar();
				if(!Character.isWhitespace(ch)) break;
				newStart++;
			}
			// Skip past the ending whitespace characters
			newEnd = end;
			while(newEnd>newStart) {
				raf.seek((newEnd-1)<<1);
				char ch = raf.readChar();
				if(!Character.isWhitespace(ch)) break;
				newEnd--;
			}
		}
		// Keep this object if already trimmed
		if(
			start==newStart
			&& end==newEnd
		) {
			_trimmed = this;
		} else {
			// Check if empty
			if(newStart==newEnd) {
				_trimmed = EmptyResult.getInstance();
			} else {
				// Otherwise, return new substring
				TempFileResult newTrimmed = new TempFileResult(
					tempFile,
					newStart,
					newEnd
				);
				newTrimmed.trimmed.set(newTrimmed);
				_trimmed = newTrimmed;
			}
		}
		if(this.trimmed.compareAndSet(null, _trimmed)) {
			return _trimmed;
		} else {
			return this.trimmed.get();
		}
	}
}

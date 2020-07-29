/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2014, 2015, 2016, 2019, 2020  AO Industries, Inc.
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
import java.nio.channels.ClosedChannelException;
import java.util.logging.Logger;

/**
 * Buffers all writes in segments.  This is to hold references to strings instead
 * of copying all the characters.
 *
 * This class is not thread safe.
 * 
 * Future: If writing to another segmented buffer, the segments could be shared between
 * the two instances. (or arraycopy instead of writing each)
 *
 * @author  AO Industries, Inc.
 */
public class SegmentedWriter extends BufferWriter {

	private static final Logger logger = Logger.getLogger(SegmentedWriter.class.getName());

	/**
	 * The number of starting elements in segment arrays.
	 */
	private static final int START_LEN = 16;

	/**
	 * The set of internal types supported.
	 */
	static final byte
		TYPE_STRING = 1,
		TYPE_CHAR_NEWLINE = 2,
		TYPE_CHAR_QUOTE = 3,
		TYPE_CHAR_APOS = 4,
		TYPE_CHAR_OTHER = 5
	;

	/**
	 * The length of the writer is the sum of the length of all its segments.
	 * Once closed, this length will not be modified.
	 */
	private long length;

	/**
	 * The set of segments are maintained in an array.
	 * Once closed, these arrays will not be modified.
	 */
	private byte[] segmentTypes;
	private Object[] segmentValues;
	private int[] segmentOffsets;
	private int[] segmentLengths;
	private int segmentCount;

	/**
	 * Once closed, no further information may be written.
	 * Manipulations are only active once closed.
	 */
	private boolean isClosed;

	public SegmentedWriter() {
		this.length = 0;
		this.segmentTypes = EmptyArrays.EMPTY_BYTE_ARRAY;
		this.segmentValues = EmptyArrays.EMPTY_OBJECT_ARRAY;
		this.segmentOffsets = EmptyArrays.EMPTY_INT_ARRAY;
		this.segmentLengths = EmptyArrays.EMPTY_INT_ARRAY;
		this.segmentCount = 0;
		this.isClosed = false;
	}

	/**
	 * Adds a new segment.
	 */
	private void addSegment(byte type, Object value, int off, int len) {
		assert !isClosed;
		assert len>0 : "Empty segments should never be added";
		final int arraylen = segmentValues.length;
		if(segmentCount==arraylen) {
			// Need to grow
			if(arraylen==0) {
				this.segmentTypes = new byte[START_LEN];
				this.segmentValues = new Object[START_LEN];
				this.segmentOffsets = new int[START_LEN];
				this.segmentLengths = new int[START_LEN];
			} else {
				// Double capacity and copy
				int newLen = arraylen<<1;
				byte[] newTypes = new byte[newLen];
				System.arraycopy(segmentTypes, 0, newTypes, 0, arraylen);
				this.segmentTypes = newTypes;
				Object[] newValues = new Object[newLen];
				System.arraycopy(segmentValues, 0, newValues, 0, arraylen);
				this.segmentValues = newValues;
				int[] newOffsets = new int[newLen];
				System.arraycopy(segmentOffsets, 0, newOffsets, 0, arraylen);
				this.segmentOffsets = newOffsets;
				int[] newLengths = new int[newLen];
				System.arraycopy(segmentLengths, 0, newLengths, 0, arraylen);
				this.segmentLengths = newLengths;
			}
		}
		segmentTypes[segmentCount] = type;
		segmentValues[segmentCount] = value;
		segmentOffsets[segmentCount] = off;
		segmentLengths[segmentCount++] = len;
	}

	@Override
	public void write(int c) throws ClosedChannelException {
		if(isClosed) throw new ClosedChannelException();
		char ch = (char)c;
		switch(ch) {
			case '\n' :
				addSegment(TYPE_CHAR_NEWLINE, null, 0, 1);
				break;
			case '\'' :
				addSegment(TYPE_CHAR_APOS, null, 0, 1);
				break;
			case '"' :
				addSegment(TYPE_CHAR_QUOTE, null, 0, 1);
				break;
			default :
				addSegment(TYPE_CHAR_OTHER, (Character)ch, 0, 1);
		}
		length++;
	}

	@Override
	public void write(char cbuf[]) throws ClosedChannelException {
		if(isClosed) throw new ClosedChannelException();
		final int len = cbuf.length;
		if(len>0) {
			if(len==1) {
				write(cbuf[0]);
			} else {
				addSegment(
					TYPE_STRING,
					new String(cbuf),
					0,
					len
				);
				length += len;
			}
		}
	}

	@Override
	public void write(char cbuf[], int off, int len) throws ClosedChannelException {
		if(isClosed) throw new ClosedChannelException();
		if(len>0) {
			if(len==1) {
				write(cbuf[off]);
			} else {
				addSegment(
					TYPE_STRING,
					new String(cbuf, off, len),
					0,
					len
				);
				length += len;
			}
		}
	}

	@Override
	public void write(String str) throws ClosedChannelException {
		if(isClosed) throw new ClosedChannelException();
		final int len = str.length();
		if(len>0) {
			if(len==1) {
				// Prefer character shortcuts
				switch(str.charAt(0)) {
					case '\n' :
						addSegment(TYPE_CHAR_NEWLINE, null, 0, 1);
						break;
					case '\'' :
						addSegment(TYPE_CHAR_APOS, null, 0, 1);
						break;
					case '"' :
						addSegment(TYPE_CHAR_QUOTE, null, 0, 1);
						break;
					default :
						addSegment(TYPE_STRING, str, 0, 1);
				}
			} else {
				addSegment(TYPE_STRING, str, 0, len);
			}
			length += len;
		}
	}

	@Override
	public void write(String str, int off, int len) throws ClosedChannelException {
		if(isClosed) throw new ClosedChannelException();
		if(len>0) {
			if(len==1) {
				// Prefer character shortcuts
				switch(str.charAt(off)) {
					case '\n' :
						addSegment(TYPE_CHAR_NEWLINE, null, 0, 1);
						break;
					case '\'' :
						addSegment(TYPE_CHAR_APOS, null, 0, 1);
						break;
					case '"' :
						addSegment(TYPE_CHAR_QUOTE, null, 0, 1);
						break;
					default :
						addSegment(TYPE_STRING, str, off, 1);
				}
			} else {
				addSegment(TYPE_STRING, str, off, len);
			}
			length += len;
		}
	}

	@Override
	public SegmentedWriter append(CharSequence csq) throws ClosedChannelException {
		if(isClosed) throw new ClosedChannelException();
		if(csq==null) {
			write("null");
		} else {
			final int len = csq.length();
			if(len>0) {
				if(len==1) {
					// Prefer character shortcuts
					switch(csq.charAt(0)) {
						case '\n' :
							addSegment(TYPE_CHAR_NEWLINE, null, 0, 1);
							break;
						case '\'' :
							addSegment(TYPE_CHAR_APOS, null, 0, 1);
							break;
						case '"' :
							addSegment(TYPE_CHAR_QUOTE, null, 0, 1);
							break;
						default :
							addSegment(TYPE_STRING, csq.toString(), 0, 1);
					}
				} else {
					addSegment(TYPE_STRING, csq.toString(), 0, len);
				}
				length += len;
			}
		}
		return this;
	}

	@Override
	public SegmentedWriter append(CharSequence csq, int start, int end) throws ClosedChannelException {
		if(isClosed) throw new ClosedChannelException();
		if(csq==null) {
			write("null");
		} else {
			final int len = end-start;
			if(len>0) {
				if(len==1) {
					// Prefer character shortcuts
					char ch = csq.charAt(start);
					switch(ch) {
						case '\n' :
							addSegment(TYPE_CHAR_NEWLINE, null, 0, 1);
							break;
						case '\'' :
							addSegment(TYPE_CHAR_APOS, null, 0, 1);
							break;
						case '"' :
							addSegment(TYPE_CHAR_QUOTE, null, 0, 1);
							break;
						default :
							if(
								ch <= 127 // Always cached
								|| !(csq instanceof String) // Use Character for all non-Strings
							) {
								addSegment(
									TYPE_CHAR_OTHER,
									(Character)ch,
									0,
									1
								);
							} else {
								// Use offset for String
								addSegment(
									TYPE_STRING,
									(String)csq,
									start,
									1
								);
							}
					}
				} else {
					if(csq instanceof String) {
						// Use offset for String
						addSegment(
							TYPE_STRING,
							(String)csq,
							start,
							len
						);
					} else {
						// Use subSequence().toString() for all non-Strings
						addSegment(
							TYPE_STRING,
							csq.subSequence(start, end).toString(),
							0,
							len
						);
					}
				}
				length += len;
			}
		}
		return this;
	}

	@Override
	public SegmentedWriter append(char c) throws ClosedChannelException {
		if(isClosed) throw new ClosedChannelException();
		switch(c) {
			case '\n' :
				addSegment(TYPE_CHAR_NEWLINE, null, 0, 1);
				break;
			case '\'' :
				addSegment(TYPE_CHAR_APOS, null, 0, 1);
				break;
			case '"' :
				addSegment(TYPE_CHAR_QUOTE, null, 0, 1);
				break;
			default :
				addSegment(TYPE_CHAR_OTHER, (Character)c, 0, 1);
		}
		length++;
		return this;
	}

	@Override
	public void flush() {
		// Nothing to do
	}

	//private static long biggest = 0;
	@Override
	public void close() {
		isClosed = true;
		/*
		long heap =
			segmentTypes.length * Byte.SIZE
			+ segmentValues.length * 8 // assume 64-bit data for worst-case numbers
			+ segmentOffsets.length * Integer.SIZE
			+ segmentLengths.length * Integer.SIZE
		;
		if(heap>biggest) {
			biggest = heap;
			System.err.println("SegmentedWriter: Biggest heap: " + biggest);
		}*/
	}

	@Override
	public long getLength() {
		return length;
	}

	@Override
	public String toString() {
		return "SegmentedWriter(length=" + length + ", count=" + segmentCount + ", capacity=" + segmentValues.length +")";
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
				assert segmentCount>0 : "When not empty and using segments, must have at least one segment";
				int endSegmentIndex = segmentCount - 1;
				result = new SegmentedResult(
					segmentTypes,
					segmentValues,
					segmentOffsets,
					segmentLengths,
					0, // start
					0, // startSegmentIndex
					segmentOffsets[0],
					segmentLengths[0],
					length, // end
					endSegmentIndex,
					segmentOffsets[endSegmentIndex],
					segmentLengths[endSegmentIndex]
				);
			}
		}
		return result;
	}
}

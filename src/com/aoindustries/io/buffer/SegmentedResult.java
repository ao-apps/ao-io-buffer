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

import com.aoindustries.io.Encoder;
import com.aoindustries.lang.NotImplementedException;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * {@inheritDoc}
 *
 * This class is not thread safe.
 *
 * @author  AO Industries, Inc.
 */
public class SegmentedResult implements BufferResult {

    private static final Logger logger = Logger.getLogger(SegmentedResult.class.getName());

	/**
	 * @see  SegmentedWriter#segmentTypes
	 */
	private final byte[] segmentTypes;
	private final Object[] segmentValues;
	private final int[] segmentOffsets;
	private final int[] segmentLengths;

	/**
	 * When segments are trimmed (or other types of substring operations), they
	 * share the underlying segment data.  This is similar to the pre Java 1.7
	 * implementation of String.substring.
	 */
	private final long start;
	private final int startSegmentIndex;
	private final int startSegmentOffset;
	private final int startSegmentLength;
	private final long end;
	private final int endSegmentIndex;
	private final int endSegmentOffset;
	private final int endSegmentLength;

	private static void assertThingsMakeSense(
		long start,
		int startSegmentIndex,
		int startSegmentOffset,
		int startSegmentLength,
		long end,
		int endSegmentIndex,
		int endSegmentOffset,
		int endSegmentLength
	) {
		assert start <= end;
		assert startSegmentIndex <= endSegmentIndex;
		if(startSegmentIndex == endSegmentIndex) {
			assert startSegmentOffset == endSegmentOffset;
			assert startSegmentLength == endSegmentLength;
		}
	}

	protected SegmentedResult(
		byte[] segmentTypes,
		Object[] segmentValues,
		int[] segmentOffsets,
		int[] segmentLengths,
		long start,
		int startSegmentIndex,
		int startSegmentOffset, // Start offset and length may have been affected by trimming
		int startSegmentLength,
		long end,
		int endSegmentIndex,
		int endSegmentOffset,
		int endSegmentLength // End offset and length may have been affected by trimming
	) {
		assertThingsMakeSense(start, startSegmentIndex, startSegmentOffset, startSegmentLength, end, endSegmentIndex, endSegmentOffset, endSegmentLength);
		this.segmentTypes = segmentTypes;
		this.segmentValues = segmentValues;
		this.segmentOffsets = segmentOffsets;
		this.segmentLengths = segmentLengths;
		this.start = start;
		this.startSegmentIndex = startSegmentIndex;
		this.startSegmentOffset = startSegmentOffset;
		assert startSegmentLength>0 : "All empty results should have been converted to EmptyResult";
		this.startSegmentLength = startSegmentLength;
		this.end = end;
		assert endSegmentIndex >= startSegmentIndex;
		this.endSegmentIndex = endSegmentIndex;
		this.endSegmentOffset = endSegmentOffset;
		assert endSegmentLength>0 : "All empty results should have been converted to EmptyResult";
		this.endSegmentLength = endSegmentLength;
		assert
			endSegmentIndex != startSegmentIndex
			|| (
				startSegmentOffset == endSegmentOffset
				&& startSegmentLength == endSegmentLength
			)
			: "When start and end segments are at the same index, they must have the same offsets and lengths."
		;
    }

	@Override
    public long getLength() {
        return end - start;
    }

	/**
	 * Appends the full segment (with original offset and length) to the buffer.
	 */
	private void append(int segmentIndex, StringBuilder buffer) {
		switch(segmentTypes[segmentIndex]) {
			case SegmentedWriter.TYPE_STRING :
				int off = segmentOffsets[segmentIndex];
				buffer.append(
					(String)segmentValues[segmentIndex],
					off,
					off + segmentLengths[segmentIndex]
				);
				break;
			case SegmentedWriter.TYPE_CHAR_NEWLINE :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				buffer.append('\n');
				break;
			case SegmentedWriter.TYPE_CHAR_QUOTE :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				buffer.append('"');
				break;
			case SegmentedWriter.TYPE_CHAR_APOS :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				buffer.append('\'');
				break;
			case SegmentedWriter.TYPE_CHAR_OTHER :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				buffer.append(((Character)segmentValues[segmentIndex]).charValue());
				break;
			default :
				throw new AssertionError();
		}
	}

	/**
	 * Appends the given range of a segment. to the buffer.
	 */
	private void append(int segmentIndex, int off, int len, StringBuilder buffer) {
		switch(segmentTypes[segmentIndex]) {
			case SegmentedWriter.TYPE_STRING :
				buffer.append(
					(String)segmentValues[segmentIndex],
					off,
					off + len
				);
				break;
			case SegmentedWriter.TYPE_CHAR_NEWLINE :
				assert off==0;
				assert len==1;
				buffer.append('\n');
				break;
			case SegmentedWriter.TYPE_CHAR_QUOTE :
				assert off==0;
				assert len==1;
				buffer.append('"');
				break;
			case SegmentedWriter.TYPE_CHAR_APOS :
				assert off==0;
				assert len==1;
				buffer.append('\'');
				break;
			case SegmentedWriter.TYPE_CHAR_OTHER :
				assert off==0;
				assert len==1;
				buffer.append(((Character)segmentValues[segmentIndex]).charValue());
				break;
			default :
				throw new AssertionError();
		}
	}

	/**
	 * Writes the full segment (with original offset and length) to the given writer using the given encoder.
	 */
	private void writeSegment(int segmentIndex, Encoder encoder, Writer out) throws IOException {
		switch(segmentTypes[segmentIndex]) {
			case SegmentedWriter.TYPE_STRING :
				encoder.write(
					(String)segmentValues[segmentIndex],
					segmentOffsets[segmentIndex],
					segmentLengths[segmentIndex],
					out
				);
				break;
			case SegmentedWriter.TYPE_CHAR_NEWLINE :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				encoder.write('\n', out);
				break;
			case SegmentedWriter.TYPE_CHAR_QUOTE :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				encoder.write('"', out);
				break;
			case SegmentedWriter.TYPE_CHAR_APOS :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				encoder.write('\'', out);
				break;
			case SegmentedWriter.TYPE_CHAR_OTHER :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				encoder.write(((Character)segmentValues[segmentIndex]).charValue(), out);
				break;
			default :
				throw new AssertionError();
		}
	}

	/**
	 * Writes the given range of a segment to the given writer using the given encoder.
	 */
	private void writeSegment(int segmentIndex, int off, int len, Encoder encoder, Writer out) throws IOException {
		switch(segmentTypes[segmentIndex]) {
			case SegmentedWriter.TYPE_STRING :
				encoder.write(
					(String)segmentValues[segmentIndex],
					off,
					len,
					out
				);
				break;
			case SegmentedWriter.TYPE_CHAR_NEWLINE :
				assert off==0;
				assert len==1;
				encoder.write('\n', out);
				break;
			case SegmentedWriter.TYPE_CHAR_QUOTE :
				assert off==0;
				assert len==1;
				encoder.write('"', out);
				break;
			case SegmentedWriter.TYPE_CHAR_APOS :
				assert off==0;
				assert len==1;
				encoder.write('\'', out);
				break;
			case SegmentedWriter.TYPE_CHAR_OTHER :
				assert off==0;
				assert len==1;
				encoder.write(((Character)segmentValues[segmentIndex]).charValue(), out);
				break;
			default :
				throw new AssertionError();
		}
	}

	/**
	 * Writes the full segment (with original offset and length) to the given writer.
	 */
	private void writeSegment(int segmentIndex, Writer out) throws IOException {
		switch(segmentTypes[segmentIndex]) {
			case SegmentedWriter.TYPE_STRING :
				out.write(
					(String)segmentValues[segmentIndex],
					segmentOffsets[segmentIndex],
					segmentLengths[segmentIndex]
				);
				break;
			case SegmentedWriter.TYPE_CHAR_NEWLINE :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				out.write('\n');
				break;
			case SegmentedWriter.TYPE_CHAR_QUOTE :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				out.write('"');
				break;
			case SegmentedWriter.TYPE_CHAR_APOS :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				out.write('\'');
				break;
			case SegmentedWriter.TYPE_CHAR_OTHER :
				assert segmentOffsets[segmentIndex]==0;
				assert segmentLengths[segmentIndex]==1;
				out.write(((Character)segmentValues[segmentIndex]).charValue());
				break;
			default :
				throw new AssertionError();
		}
	}

	/**
	 * Writes the given range of a segment to the given writer.
	 */
	private void writeSegment(int segmentIndex, int off, int len, Writer out) throws IOException {
		switch(segmentTypes[segmentIndex]) {
			case SegmentedWriter.TYPE_STRING :
				out.write(
					(String)segmentValues[segmentIndex],
					off,
					len
				);
				break;
			case SegmentedWriter.TYPE_CHAR_NEWLINE :
				assert off==0;
				assert len==1;
				out.write('\n');
				break;
			case SegmentedWriter.TYPE_CHAR_QUOTE :
				assert off==0;
				assert len==1;
				out.write('"');
				break;
			case SegmentedWriter.TYPE_CHAR_APOS :
				assert off==0;
				assert len==1;
				out.write('\'');
				break;
			case SegmentedWriter.TYPE_CHAR_OTHER :
				assert off==0;
				assert len==1;
				out.write(((Character)segmentValues[segmentIndex]).charValue());
				break;
			default :
				throw new AssertionError();
		}
	}

	/**
	 * Gets the character at the given index in a segment.
	 * This is the absolute index, the offset is not added-in.
	 */
	private static char charAt(byte type, Object value, int charIndex) {
		switch(type) {
			case SegmentedWriter.TYPE_STRING :
				return ((String)value).charAt(charIndex);
			case SegmentedWriter.TYPE_CHAR_NEWLINE :
				assert charIndex==0;
				return '\n';
			case SegmentedWriter.TYPE_CHAR_QUOTE :
				assert charIndex==0;
				return '"';
			case SegmentedWriter.TYPE_CHAR_APOS :
				assert charIndex==0;
				return '\'';
			case SegmentedWriter.TYPE_CHAR_OTHER :
				assert charIndex==0;
				return ((Character)value).charValue();
			default :
				throw new AssertionError();
		}
	}

	private String toStringCache;

	@Override
	public boolean isFastToString() {
		// Already converted, fast now
		if(toStringCache!=null) return true;
		// If more than one segment, can't be fast toString
		if(startSegmentIndex!=endSegmentIndex) return false;
		// Look for one segment shortcut by type
		switch(segmentTypes[startSegmentIndex]) {
			case SegmentedWriter.TYPE_STRING :
				// If offset into string, is not fast (would incur substring)
				if(startSegmentOffset!=0) return false;
				String str = (String)segmentValues[startSegmentIndex];
				// If fast when covers entire string in this segment
				return startSegmentLength==str.length();
			case SegmentedWriter.TYPE_CHAR_NEWLINE :
				assert startSegmentOffset==0;
				assert startSegmentLength==1;
				return true;
			case SegmentedWriter.TYPE_CHAR_QUOTE :
				assert startSegmentOffset==0;
				assert startSegmentLength==1;
				return true;
			case SegmentedWriter.TYPE_CHAR_APOS :
				assert startSegmentOffset==0;
				assert startSegmentLength==1;
				return true;
			case SegmentedWriter.TYPE_CHAR_OTHER :
				assert startSegmentOffset==0;
				assert startSegmentLength==1;
				return true;
			default :
				throw new AssertionError();
		}
	}

    @Override
    public String toString() {
		if(toStringCache==null) {
			if(startSegmentIndex==endSegmentIndex) {
				// Shortcut for one segment
				switch(segmentTypes[startSegmentIndex]) {
					case SegmentedWriter.TYPE_STRING :
						toStringCache = ((String)segmentValues[startSegmentIndex]).substring(startSegmentOffset, startSegmentOffset + startSegmentLength);
						break;
					case SegmentedWriter.TYPE_CHAR_NEWLINE :
						assert startSegmentOffset==0;
						assert startSegmentLength==1;
						toStringCache = "\n";
						break;
					case SegmentedWriter.TYPE_CHAR_QUOTE :
						assert startSegmentOffset==0;
						assert startSegmentLength==1;
						toStringCache = "\"";
						break;
					case SegmentedWriter.TYPE_CHAR_APOS :
						assert startSegmentOffset==0;
						assert startSegmentLength==1;
						toStringCache = "'";
						break;
					case SegmentedWriter.TYPE_CHAR_OTHER :
						assert startSegmentOffset==0;
						assert startSegmentLength==1;
						toStringCache = String.valueOf(((Character)segmentValues[startSegmentIndex]).charValue());
						break;
					default :
						throw new AssertionError();
				}
			} else {
				logger.fine("Creating String from segments - benefits of SegmentedWriter negated.");
				final long length = end - start;
				if(length>Integer.MAX_VALUE) throw new RuntimeException("Buffer too large to convert to String: length="+length);
				StringBuilder buffer = new StringBuilder((int)length);
				// First segment
				append(startSegmentIndex, startSegmentOffset, startSegmentLength, buffer);
				// Middle segments
				for(int i=startSegmentIndex+1; i<endSegmentIndex; i++) {
					append(i, buffer);
				}
				// End segment
				append(endSegmentIndex, endSegmentOffset, endSegmentLength, buffer);
				assert buffer.length()==length : "buffer.length()!=length: "+buffer.length()+"!="+length;
				toStringCache = buffer.toString();
			}
		}
		return toStringCache;
    }

	@Override
    public void writeTo(Writer out) throws IOException {
		// First segment
		writeSegment(
			startSegmentIndex,
			startSegmentOffset,
			startSegmentLength,
			out
		);
		if(endSegmentIndex!=startSegmentIndex) {
			// Middle segments
			int i = startSegmentIndex+1;
			if(i<endSegmentIndex) {
				// if(out instanceof SegmentedWriter) System.err.println("TODO: Perform array copy of middle segments");
				do {
					writeSegment(i++, out);
				} while(i<endSegmentIndex);
			}
			// End segment
			writeSegment(
				endSegmentIndex,
				endSegmentOffset,
				endSegmentLength,
				out
			);
		}
    }

	@Override
    public void writeTo(Writer out, long off, long len) throws IOException {
		// Implementation will need to start through beginning
		throw new NotImplementedException("Implement when first needed.");
	}

	@Override
    public void writeTo(Encoder encoder, Writer out) throws IOException {
		if(encoder==null) {
			writeTo(out);
		} else {
			// TODO: If copying to another SegmentedWriter, we have a chance here to share segment list (current the StringBuilder)
			// First segment
			writeSegment(
				startSegmentIndex,
				startSegmentOffset,
				startSegmentLength,
				encoder,
				out
			);
			if(endSegmentIndex!=startSegmentIndex) {
				// Middle segments
				for(int i=startSegmentIndex+1; i<endSegmentIndex; i++) {
					writeSegment(i, encoder, out);
				}
				// End segment
				writeSegment(
					endSegmentIndex,
					endSegmentOffset,
					endSegmentLength,
					encoder,
					out
				);
			}
		}
	}

	@Override
    public void writeTo(Encoder encoder, Writer out, long off, long len) throws IOException {
		// Implementation will need to start through beginning
		throw new NotImplementedException("Implement when first needed.");
	}

	@Override
	public BufferResult trim() throws IOException {
		// Trim from the left
		long newStart = start;
		int newStartSegmentIndex = startSegmentIndex;
		int newStartSegmentOffset = startSegmentOffset;
		int newStartSegmentLength = startSegmentLength;
		long newEnd = end;
		int newEndSegmentIndex = endSegmentIndex;
		int newEndSegmentOffset = endSegmentOffset;
		int newEndSegmentLength = endSegmentLength;
		assertThingsMakeSense(newStart, newStartSegmentIndex, newStartSegmentOffset, newStartSegmentLength, newEnd, newEndSegmentIndex, newEndSegmentOffset, newEndSegmentLength);
		// Skip past the beginning whitespace characters
		TRIM_LEFT :
		while(newStart<newEnd) {
			// Work on one segment
			final byte type = segmentTypes[newStartSegmentIndex];
			final Object value = segmentValues[newStartSegmentIndex];
			// do...while because segments are never empty
			do {
				char ch = charAt(type, value, newStartSegmentOffset);
				if(!Character.isWhitespace(ch)) break TRIM_LEFT;
				newStart++;
				newStartSegmentOffset++;
				newStartSegmentLength--;
				// Also trim end segment numbers if equal to begin segment index
				if(newStartSegmentIndex==newEndSegmentIndex) {
					newEndSegmentOffset++;
					newEndSegmentLength--;
				}
				assertThingsMakeSense(newStart, newStartSegmentIndex, newStartSegmentOffset, newStartSegmentLength, newEnd, newEndSegmentIndex, newEndSegmentOffset, newEndSegmentLength);
				if(newEnd==newStart) break TRIM_LEFT;
			} while(newStartSegmentLength>0);
			// Move to next segment
			newStartSegmentIndex++;
			if(newStartSegmentIndex==newEndSegmentIndex) {
				// Now reached end segment
				newStartSegmentOffset = newEndSegmentOffset;
				newStartSegmentLength = newEndSegmentLength;
			} else {
				// Middle segment
				newStartSegmentOffset = segmentOffsets[newStartSegmentIndex];
				newStartSegmentLength = segmentLengths[newStartSegmentIndex];
			}
			assertThingsMakeSense(newStart, newStartSegmentIndex, newStartSegmentOffset, newStartSegmentLength, newEnd, newEndSegmentIndex, newEndSegmentOffset, newEndSegmentLength);
		}
		// Trim from the right
		if(newEnd>newStart) {
			assert newEndSegmentIndex >= 0;
			// Work on one segment
			byte type = segmentTypes[newEndSegmentIndex];
			Object value = segmentValues[newEndSegmentIndex];
			TRIM_RIGHT :
			do {
				// do...while because segments are never empty
				do {
					char ch = charAt(type, value, newEndSegmentOffset + newEndSegmentLength - 1);
					if(!Character.isWhitespace(ch)) break TRIM_RIGHT;
					newEnd--;
					newEndSegmentLength--;
					// Also trim start segment numbers of equal to end segment index
					if(newStartSegmentIndex==newEndSegmentIndex)  {
						newStartSegmentLength--;
					}
					assertThingsMakeSense(newStart, newStartSegmentIndex, newStartSegmentOffset, newStartSegmentLength, newEnd, newEndSegmentIndex, newEndSegmentOffset, newEndSegmentLength);
					if(newEnd==newStart) break TRIM_RIGHT;
				} while(newEndSegmentLength > 0);
				// Move to previous segment
				newEndSegmentIndex--;
				assert newEndSegmentIndex >= 0 : "Must be non-negative because we have not made it back to newStart yet";
				type = segmentTypes[newEndSegmentIndex];
				value = segmentValues[newEndSegmentIndex];
				if(newEndSegmentIndex==newStartSegmentIndex) {
					// Now reached start segment
					newEndSegmentOffset = newStartSegmentOffset;
					newEndSegmentLength = newStartSegmentLength;
				} else {
					// Middle segment
					newEndSegmentOffset = segmentOffsets[newEndSegmentIndex];
					newEndSegmentLength = segmentLengths[newEndSegmentIndex];
				}
				assertThingsMakeSense(newStart, newStartSegmentIndex, newStartSegmentOffset, newStartSegmentLength, newEnd, newEndSegmentIndex, newEndSegmentOffset, newEndSegmentLength);
			} while(true);
		}

		// Keep this object if already trimmed
		if(
			start==newStart
			&& end==newEnd
		) {
			return this;
		} else {
			// Check if empty
			if(newStart==newEnd) {
				return EmptyResult.getInstance();
			} else {
				// Otherwise, return new substring
				return new SegmentedResult(
					segmentTypes,
					segmentValues,
					segmentOffsets,
					segmentLengths,
					newStart,
					newStartSegmentIndex,
					newStartSegmentOffset,
					newStartSegmentLength,
					newEnd,
					newEndSegmentIndex,
					newEndSegmentOffset,
					newEndSegmentLength
				);
			}
		}
	}
}

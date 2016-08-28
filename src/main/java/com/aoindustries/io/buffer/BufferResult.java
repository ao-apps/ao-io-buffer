/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015, 2016  AO Industries, Inc.
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

import com.aoindustries.io.Writable;
import java.io.IOException;

/**
 * The result from completion of a buffered writer.  Only available after a
 * buffered writer has been closed.
 * <p>
 * Idea: Add contentEquals(String) method to avoid some uses of toString?
 * </p>
 *
 * @see  AoBufferedWriter
 *
 * @author  AO Industries, Inc.
 */
public interface BufferResult extends Writable {

	/**
	 * Trims the contents of this result, returning the instance that represents this result trimmed.
	 */
	BufferResult trim() throws IOException;
}

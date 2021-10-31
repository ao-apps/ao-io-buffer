/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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
 * along with ao-io-buffer.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.aoapps.io.buffer;

import com.aoapps.lang.Strings;
import com.aoapps.lang.io.Writable;
import java.io.IOException;

/**
 * The result from completion of a buffered writer.  Only available after a
 * buffered writer has been closed.
 * <p>
 * All implementations are thread safe.
 * </p>
 * <p>
 * Idea: Add contentEquals(String) method to avoid some uses of toString?
 * </p>
 *
 * @see  BufferWriter
 *
 * @author  AO Industries, Inc.
 */
public interface BufferResult extends Writable {

	/**
	 * <p>
	 * Trims the contents of this result, as per rules of {@link Strings#isWhitespace(int)},
	 * returning the instance that represents this result trimmed.
	 * </p>
	 * <p>
	 * All implementations cache the result for constant-time secondary access.
	 * </p>
	 */
	@Override
	BufferResult trim() throws IOException;
}

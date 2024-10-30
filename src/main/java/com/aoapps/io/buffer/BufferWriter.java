/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015, 2016, 2020, 2021, 2022, 2024  AO Industries, Inc.
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

import java.io.IOException;
import java.io.Writer;

/**
 * A buffered writer with results that may be trimmed, converted to String, and written to another
 * writer.
 *
 * @author  AO Industries, Inc.
 */
public abstract class BufferWriter extends Writer {

  protected BufferWriter() {
    // Do nothing
  }

  /**
   * Gets the number of characters in this buffer.
   * Once closed, this length will not be modified.
   */
  public abstract long getLength() throws IOException;

  /**
   * Gets a short message (like type and length).
   *
   * @see  #getResult()  To get access to the buffered content.
   */
  @Override
  public abstract String toString();

  /**
   * Gets the result from this buffer.
   * The buffer must be closed.
   *
   * <p>Note: Although the {@link BufferWriter} are generally not thread-safe, the {@link BufferResult} is thread-safe.
   * It is expected to commonly create a buffer on one thread, but the be able to safely share the result among many
   * threads.</p>
   *
   * @exception  IllegalStateException if not closed
   */
  public abstract BufferResult getResult() throws IllegalStateException, IOException;
}

/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015, 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.lang.io.Encoder;
import java.io.Writer;

/**
 * A completely empty result.
 *
 * @author  AO Industries, Inc.
 */
public final class EmptyResult implements BufferResult {

  private static final EmptyResult instance = new EmptyResult();

  public static EmptyResult getInstance() {
    return instance;
  }

  private EmptyResult() {
    // Do nothing
  }

  @Override
  public long getLength() {
    return 0;
  }

  @Override
  public boolean isFastToString() {
    return true;
  }

  @Override
  public String toString() {
    return "";
  }

  @Override
  public void writeTo(Writer out) {
    // Nothing to write
  }

  @Override
  public void writeTo(Writer out, long off, long len) {
    // Nothing to write
  }

  @Override
  public void writeTo(Encoder encoder, Writer out) {
    // Nothing to write
  }

  @Override
  public void writeTo(Encoder encoder, Writer out, long off, long len) {
    // Nothing to write
  }

  @Override
  public void appendTo(Appendable out) {
    // Nothing to append
  }

  @Override
  public void appendTo(Appendable out, long start, long end) {
    // Nothing to append
  }

  @Override
  public void appendTo(Encoder encoder, Appendable out) {
    // Nothing to append
  }

  @Override
  public void appendTo(Encoder encoder, Appendable out, long start, long end) {
    // Nothing to append
  }

  @Override
  public EmptyResult trim() {
    return this;
  }
}

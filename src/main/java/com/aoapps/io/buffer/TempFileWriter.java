/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015, 2016, 2017, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.tempfiles.TempFile;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Writes to a temp file buffer.
 *
 * This class is not thread safe.
 *
 * @author  AO Industries, Inc.
 */
public class TempFileWriter extends BufferWriter {

  private static final Logger logger = Logger.getLogger(TempFileWriter.class.getName());

  /**
   * The length of the writer is the sum of the data written to the buffer.
   * Once closed, this length will not be modified.
   */
  private long length;

  /**
   * Once closed, no further information may be written.
   * Manipulations are only active once closed.
   */
  private boolean isClosed;

  // The temp file is in UTF16-BE encoding
  private final TempFile tempFile;
  private Writer fileWriter;

  /**
   * Keep the first string that was written, using it as the result when possible in order to maintain string identity
   * for in-context translation tools.
   */
  private String firstString;
  private int firstStringBegin;
  private int firstStringEnd;

  public TempFileWriter(TempFile tempFile) throws IOException {
    this.length = 0;
    this.tempFile = tempFile;
    this.fileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile.getFile()), StandardCharsets.UTF_16BE));
  }

  @Override
  public void write(int c) throws IOException {
    if (isClosed) {
      throw new ClosedChannelException();
    }
    firstString = null;
    fileWriter.write(c);
    length++;
  }

  @Override
  public void write(char[] cbuf) throws IOException {
    if (isClosed) {
      throw new ClosedChannelException();
    }
    int len = cbuf.length;
    if (len > 0) {
      firstString = null;
      fileWriter.write(cbuf);
      length += len;
    }
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    if (isClosed) {
      throw new ClosedChannelException();
    }
    if (len > 0) {
      firstString = null;
      fileWriter.write(cbuf, off, len);
      length += len;
    }
  }

  @Override
  public void write(String str) throws IOException {
    if (isClosed) {
      throw new ClosedChannelException();
    }
    int len = str.length();
    if (len > 0) {
      if (length == 0) {
        firstString = str;
        firstStringBegin = 0;
        firstStringEnd = len;
      } else {
        firstString = null;
      }
      fileWriter.write(str);
      length += len;
    }
  }

  @Override
  public void write(String str, int off, int len) throws IOException {
    if (isClosed) {
      throw new ClosedChannelException();
    }
    if (len > 0) {
      if (length == 0) {
        firstString = str;
        firstStringBegin = off;
        firstStringEnd = off + len;
      } else {
        firstString = null;
      }
      fileWriter.write(str, off, len);
      length += len;
    }
  }

  @Override
  public TempFileWriter append(CharSequence csq) throws IOException {
    if (isClosed) {
      throw new ClosedChannelException();
    }
    if (csq == null) {
      csq = "null";
    }
    int len = csq.length();
    if (len > 0) {
      if (length == 0 && csq instanceof String) {
        firstString = (String) csq;
        firstStringBegin = 0;
        firstStringEnd = len;
      } else {
        firstString = null;
      }
      fileWriter.append(csq);
      length += len;
    }
    return this;
  }

  @Override
  public TempFileWriter append(CharSequence csq, int start, int end) throws IOException {
    if (isClosed) {
      throw new ClosedChannelException();
    }
    if (csq == null) {
      csq = "null";
    }
    int len = end - start;
    if (len > 0) {
      if (length == 0 && csq instanceof String) {
        firstString = (String) csq;
        firstStringBegin = start;
        firstStringEnd = end;
      } else {
        firstString = null;
      }
      fileWriter.append(csq, start, end);
      length += len;
    }
    return this;
  }

  @Override
  public TempFileWriter append(char c) throws IOException {
    if (isClosed) {
      throw new ClosedChannelException();
    }
    fileWriter.append(c);
    firstString = null;
    length++;
    return this;
  }

  @Override
  public void flush() throws IOException {
    if (fileWriter != null) {
      fileWriter.flush();
    }
  }

  @Override
  public void close() throws IOException {
    if (fileWriter != null) {
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
    if (!isClosed) {
      throw new IllegalStateException();
    }
    if (result == null) {
      if (length == 0) {
        result = EmptyResult.getInstance();
        logger.finest("EmptyResult optimized result");
      } else {
        String fs = firstString;
        if (fs != null) {
          assert (firstStringEnd - firstStringBegin) == length;
          result = new StringResult(fs, firstStringBegin, firstStringEnd);
          firstString = null;
          logger.finest("StringResult optimized result");
        } else {
          result = new TempFileResult(tempFile, 0, length);
        }
      }
    }
    return result;
  }
}

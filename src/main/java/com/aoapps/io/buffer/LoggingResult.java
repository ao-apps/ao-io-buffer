/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015, 2016, 2020, 2021, 2022  AO Industries, Inc.
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
import com.aoapps.lang.io.EncoderWriter;
import com.aoapps.lang.util.AtomicSequence;
import com.aoapps.lang.util.Sequence;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Logs all write calls in a way that can be put into Java source code.
 * This is used to capture real-world scenarios for unit testing.
 *
 * @author  AO Industries, Inc.
 */
public class LoggingResult implements BufferResult {

	private static final Sequence idSeq = new AtomicSequence(0);

	final long id = idSeq.getNextSequenceValue();
	private final BufferResult wrapped;
	private final Writer log;

	protected LoggingResult(BufferResult wrapped, Writer log) {
		this.wrapped = wrapped;
		this.log = log;
	}

	public long getId() {
		return id;
	}

	/**
	 * Provides detailed logging for a media encoder.
	 */
	private synchronized void log(Encoder encoder) throws IOException {
		if(encoder == null) log.write("null");
		else {
			String className = encoder.getClass().getName();
			// Some shortcuts from the ao-encoding project, classnames used here to avoid hard dependency
			if("com.aoapps.encoding.JavaScriptInXhtmlAttributeEncoder".equals(className)) {
				log.write("javascriptInXhtmlAttributeEncoder");
			} else if("com.aoapps.encoding.JavaScriptInXhtmlEncoder".equals(className)) {
				log.write("javascriptInXhtmlEncoder");
			} else if("com.aoapps.encoding.TextInXhtmlAttributeEncoder".equals(className)) {
				log.write("textInXhtmlAttributeEncoder");
			} else {
				log.write(className);
			}
		}
	}

	/**
	 * Provides detailed logging for a writer.
	 */
	private synchronized void log(Writer writer) throws IOException {
		if(writer == null) {
			log.write("null");
		} else if(writer instanceof LoggingWriter) {
			LoggingWriter loggingWriter = (LoggingWriter)writer;
			log.write("writer[");
			log.write(Long.toString(loggingWriter.getId()));
			log.write(']');
		} else if(writer instanceof EncoderWriter) {
			EncoderWriter encoderWriter = (EncoderWriter)writer;
			log.write("new EncoderWriter(");
			log(encoderWriter.getEncoder());
			log.write(", ");
			log(encoderWriter.getOut());
			log.write(')');
		} else {
			String classname = writer.getClass().getName();
			if(classname.equals("org.apache.jasper.runtime.BodyContentImpl")) log.write("bodyContent");
			else if(classname.equals("org.apache.jasper.runtime.JspWriterImpl")) log.write("jspWriter");
			else log.write(classname);
		}
	}

	/**
	 * Provides detailed logging for an appendable.
	 */
	private synchronized void log(Appendable appendable) throws IOException {
		if(appendable == null) {
			log.write("null");
		} else if(appendable instanceof Writer) {
			log((Writer)appendable);
		} else {
			String classname = appendable.getClass().getName();
			log.write(classname);
		}
	}

	@Override
	public long getLength() throws IOException {
		synchronized(this) {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].getLength();\n");
			log.flush();
		}
		return wrapped.getLength();
	}

	@Override
	public boolean isFastToString() {
		return wrapped.isFastToString();
	}

	@Override
	public String toString() {
		try {
			synchronized(this) {
				log.write("result[");
				log.write(Long.toString(id));
				log.write("].toString();\n");
				log.flush();
			}
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
		return wrapped.toString();
	}

	@Override
	public void writeTo(Writer out) throws IOException {
		synchronized(this) {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].writeTo(");
			log(out);
			log.write(");\n");
			log.flush();
		}
		wrapped.writeTo(out);
	}

	@Override
	public void writeTo(Writer out, long off, long len) throws IOException {
		synchronized(this) {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].writeTo(");
			log(out);
			log.write(", ");
			log.write(Long.toString(off));
			log.write(", ");
			log.write(Long.toString(len));
			log.write(");\n");
			log.flush();
		}
		wrapped.writeTo(out, off, len);
	}

	@Override
	public void writeTo(Encoder encoder, Writer out) throws IOException {
		synchronized(this) {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].writeTo(");
			log(encoder);
			log.write(", ");
			log(out);
			log.write(");\n");
			log.flush();
		}
		wrapped.writeTo(encoder, out);
	}

	@Override
	public void writeTo(Encoder encoder, Writer out, long off, long len) throws IOException {
		synchronized(this) {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].writeTo(");
			log(encoder);
			log.write(", ");
			log(out);
			log.write(", ");
			log.write(Long.toString(off));
			log.write(", ");
			log.write(Long.toString(len));
			log.write(");\n");
			log.flush();
		}
		wrapped.writeTo(encoder, out, off, len);
	}

	@Override
	public void appendTo(Appendable out) throws IOException {
		synchronized(this) {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].appendTo(");
			log(out);
			log.write(");\n");
			log.flush();
		}
		wrapped.appendTo(out);
	}

	@Override
	public void appendTo(Appendable out, long start, long end) throws IOException {
		synchronized(this) {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].appendTo(");
			log(out);
			log.write(", ");
			log.write(Long.toString(start));
			log.write(", ");
			log.write(Long.toString(end));
			log.write(");\n");
			log.flush();
		}
		wrapped.appendTo(out, start, end);
	}

	@Override
	public void appendTo(Encoder encoder, Appendable out) throws IOException {
		synchronized(this) {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].appendTo(");
			log(encoder);
			log.write(", ");
			log(out);
			log.write(");\n");
			log.flush();
		}
		wrapped.appendTo(encoder, out);
	}

	@Override
	public void appendTo(Encoder encoder, Appendable out, long start, long end) throws IOException {
		synchronized(this) {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].appendTo(");
			log(encoder);
			log.write(", ");
			log(out);
			log.write(", ");
			log.write(Long.toString(start));
			log.write(", ");
			log.write(Long.toString(end));
			log.write(");\n");
			log.flush();
		}
		wrapped.appendTo(encoder, out, start, end);
	}

	private final AtomicReference<LoggingResult> trimmed = new AtomicReference<>();

	@Override
	public LoggingResult trim() throws IOException {
		LoggingResult _trimmed = this.trimmed.get();
		if(_trimmed == null) {
			BufferResult wrappedTrimmed = wrapped.trim();
			if(wrappedTrimmed == wrapped) {
				_trimmed = this;
			} else {
				_trimmed = new LoggingResult(wrappedTrimmed, log);
				_trimmed.trimmed.set(_trimmed);
			}
			if(!this.trimmed.compareAndSet(null, _trimmed)) {
				_trimmed = this.trimmed.get();
			}
			synchronized(this) {
				log.write("result[");
				log.write(Long.toString(_trimmed.id));
				log.write("] = result[");
				log.write(Long.toString(id));
				log.write("].trim();\n");
				log.flush();
			}
		}
		return _trimmed;
	}
}

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
import com.aoindustries.io.EncoderWriter;
import com.aoindustries.util.AtomicSequence;
import com.aoindustries.util.Sequence;
import com.aoindustries.util.WrappedException;
import java.io.IOException;
import java.io.Writer;

/**
 * Logs all write calls in a way that can be put into Java source code.
 * This is used to capture real-world scenarios for unit testing.
 *
 * This class is not thread safe.
 *
 * @author  AO Industries, Inc.
 */
public class LoggingResult implements BufferResult {

	private static final Sequence idSeq = new AtomicSequence(0);

	final long id = idSeq.getNextSequenceValue();
	private final BufferResult wrapped;
	private final Writer log;

	protected LoggingResult(BufferResult wrapped, Writer log) throws IOException {
		this.wrapped = wrapped;
		this.log = log;
    }

	public long getId() {
		return id;
	}

	/**
	 * Provides detailed logging for a media encoder.
	 */
	private void log(Encoder encoder) throws IOException {
		if(encoder==null) log.write("null");
		else {
			String className = encoder.getClass().getName();
			// Some shortcuts from the ao-encoding project, classnames used here to avoid hard dependency
			if("com.aoindustries.encoding.JavaScriptInXhtmlAttributeEncoder".equals(className)) {
				log.write("javaScriptInXhtmlAttributeEncoder");
			} else if("com.aoindustries.encoding.JavaScriptInXhtmlEncoder".equals(className)) {
				log.write("javaScriptInXhtmlEncoder");
			} else if("com.aoindustries.encoding.TextInXhtmlAttributeEncoder".equals(className)) {
				log.write("textInXhtmlAttributeEncoder");
			} else {
				log.write(className);
			}
		}
	}

	/**
	 * Provides detailed logging for a writer.
	 */
	private void log(Writer writer) throws IOException {
		if(writer==null) {
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

	@Override
    public long getLength() throws IOException {
		log.write("result[");
		log.write(Long.toString(id));
		log.write("].getLength();\n");
		log.flush();
		return wrapped.getLength();
    }

	@Override
	public boolean isFastToString() {
		return wrapped.isFastToString();
	}

	@Override
    public String toString() {
		try {
			log.write("result[");
			log.write(Long.toString(id));
			log.write("].toString();\n");
			log.flush();
		} catch(IOException e) {
			throw new WrappedException(e);
		}
		return wrapped.toString();
    }

	@Override
    public void writeTo(Writer out) throws IOException {
		log.write("result[");
		log.write(Long.toString(id));
		log.write("].writeTo(");
		log(out);
		log.write(");\n");
		log.flush();
		wrapped.writeTo(out);
    }

	@Override
    public void writeTo(Writer out, long off, long len) throws IOException {
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
		wrapped.writeTo(out, off, len);
    }

	@Override
    public void writeTo(Encoder encoder, Writer out) throws IOException {
		log.write("result[");
		log.write(Long.toString(id));
		log.write("].writeTo(");
		log(encoder);
		log.write(", ");
		log(out);
		log.write(");\n");
		log.flush();
		wrapped.writeTo(encoder, out);
	}

	@Override
    public void writeTo(Encoder encoder, Writer out, long off, long len) throws IOException {
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
		wrapped.writeTo(encoder, out, off, len);
	}

	@Override
	public LoggingResult trim() throws IOException {
		LoggingResult result = new LoggingResult(wrapped.trim(), log);
		log.write("result[");
		log.write(Long.toString(result.id));
		log.write("] = result[");
		log.write(Long.toString(id));
		log.write("].trim();\n");
		log.flush();
		return result;
	}
}

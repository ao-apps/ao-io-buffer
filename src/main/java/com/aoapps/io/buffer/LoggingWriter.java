/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015, 2016, 2020, 2021  AO Industries, Inc.
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

import com.aoapps.lang.util.AtomicSequence;
import com.aoapps.lang.util.Sequence;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

/**
 * Logs all write calls in a way that can be put into Java source code.
 * This is used to capture real-world scenarios for unit testing.
 *
 * This class is not thread safe.
 *
 * @author  AO Industries, Inc.
 */
public class LoggingWriter extends BufferWriter {

	private static final Sequence idSeq = new AtomicSequence();

	private final long id = idSeq.getNextSequenceValue();
	private final BufferWriter wrapped;
	private final Writer log;

	public LoggingWriter(BufferWriter wrapped, Writer log) throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("] = factory.newBufferWriter();\n");
		log.flush();
		this.wrapped = wrapped;
		this.log = log;
	}

	public long getId() {
		return id;
	}

	/**
	 * Writes a character, unicode escaping as needed.
	 */
	private void log(char ch) throws IOException {
		if(ch == '\t') log.write("'\\t'");
		else if(ch == '\b') log.write("'\\b'");
		else if(ch == '\n') log.write("'\\n'");
		else if(ch == '\r') log.write("'\\r'");
		else if(ch == '\f') log.write("'\\f'");
		else if(ch == '\'') log.write("'\\'");
		else if(ch == '\\') log.write("'\\\\'");
		else if(ch == '"') log.write("'\\\"'");
		else if(ch < ' ') {
			log.write("'\\u");
			String hex = Integer.toHexString(ch);
			for(int l = hex.length(); l < 4; l++) {
				log.write('0');
			}
			log.write(hex);
			log.write('\'');
		} else {
			log.write('\'');
			log.write(ch);
			log.write('\'');
		}
	}

	/**
	 * Writes a String, unicode escaping as needed.
	 */
	private void log(String value) throws IOException {
		if(value == null) {
			log.write("(String)null");
		} else {
			log.write('"');
			for(int i = 0, len = value.length(); i < len; i++) {
				char ch = value.charAt(i);
				if(ch == '\t') log.write("\\t");
				else if(ch == '\b') log.write("\\b");
				else if(ch == '\n') log.write("\\n");
				else if(ch == '\r') log.write("\\r");
				else if(ch == '\f') log.write("\\f");
				else if(ch == '\\') log.write("\\\\");
				else if(ch == '"') log.write("\\\"");
				else if(ch < ' ') {
					log.write("\\u");
					String hex = Integer.toHexString(ch);
					for(int l = hex.length(); l < 4; l++) {
						log.write('0');
					}
					log.write(hex);
				} else {
					log.write(ch);
				}
			}
			log.write('"');
		}
	}

	@Override
	public void write(int c) throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].write(");
		log.write(Integer.toString(c));
		log.write(");\n");
		log.flush();
		wrapped.write(c);
	}

	@Override
	public void write(char[] cbuf) throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].write(");
		log(new String(cbuf));
		log.write(".toCharArray());\n");
		/*
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].write(new char[] {");
		for(int i = 0, end = cbuf.length; i < end; i++) {
			if((i % 50) == 0) log.write("\n    ");
			if(i > 0) log.write(',');
			log(cbuf[i]);
		}
		log.write("\n});\n");
		*/
		log.flush();
		wrapped.write(cbuf);
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].write(");
		log(new String(cbuf, 0, off+len));
		log.write(".toCharArray(), ");
		log.write(Integer.toString(off));
		log.write(", ");
		log.write(Integer.toString(len));
		log.write(");\n");
		/*
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].write(new char[] {");
		for(int i = 0, end = off + len; i < end; i++) {
			if((i % 50) == 0) log.write("\n    ");
			if(i > 0) log.write(',');
			log(cbuf[i]);
		}
		log.write("\n}, ");
		log.write(Integer.toString(off));
		log.write(", ");
		log.write(Integer.toString(len));
		log.write(");\n");
		*/
		log.flush();
		wrapped.write(cbuf, off, len);
	}

	@Override
	public void write(String str) throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].write(");
		log(str);
		log.write(");\n");
		log.flush();
		wrapped.write(str);
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].write(");
		log(str);
		log.write(", ");
		log.write(Integer.toString(off));
		log.write(", ");
		log.write(Integer.toString(len));
		log.write(");\n");
		log.flush();
		wrapped.write(str, off, len);
	}

	@Override
	public LoggingWriter append(CharSequence csq) throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].append(");
		log(csq == null ? null : csq.toString());
		log.write(");\n");
		log.flush();
		wrapped.append(csq);
		return this;
	}

	@Override
	public LoggingWriter append(CharSequence csq, int start, int end) throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].append(");
		log(csq == null ? null : csq.toString());
		log.write(", ");
		log.write(Integer.toString(start));
		log.write(", ");
		log.write(Integer.toString(end));
		log.write(");\n");
		log.flush();
		wrapped.append(csq, start, end);
		return this;
	}

	@Override
	public LoggingWriter append(char c) throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].append(");
		log(c);
		log.write(");\n");
		log.flush();
		wrapped.append(c);
		return this;
	}

	@Override
	public void flush() throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].flush();\n");
		log.flush();
		wrapped.flush();
	}

	@Override
	public void close() throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].close();\n");
		log.flush();
		wrapped.close();
	}

	@Override
	public long getLength() throws IOException {
		log.write("writer[");
		log.write(Long.toString(id));
		log.write("].getLength();\n");
		log.flush();
		return wrapped.getLength();
	}

	@Override
	public String toString() {
		try {
			log.write("writer[");
			log.write(Long.toString(id));
			log.write("].toString();\n");
			log.flush();
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
		return "LoggingWriter(" + wrapped.toString() + ")";
	}

	// The result is cached after first created
	private LoggingResult result;

	@Override
	public LoggingResult getResult() throws IllegalStateException, IOException {
		if(result == null) {
			result = new LoggingResult(wrapped.getResult(), log);
			log.write("result[");
			log.write(Long.toString(result.id));
			log.write("] = writer[");
			log.write(Long.toString(id));
			log.write("].getResult();\n");
			log.flush();
		}
		return result;
	}
}

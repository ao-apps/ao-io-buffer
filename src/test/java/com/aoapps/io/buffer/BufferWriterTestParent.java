/*
 * ao-io-buffer - Output buffering library.
 * Copyright (C) 2013, 2015, 2016, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import junit.framework.TestCase;

/**
 * @author  AO Industries, Inc.
 */
public abstract class BufferWriterTestParent extends TestCase {

	protected BufferWriterTestParent(String testName) {
		super(testName);
	}

	public static interface BufferWriterFactory {
		String getName();
		BufferWriter newBufferWriter();
	}

	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	public void benchmarkSimulate(BufferWriterFactory factory) throws IOException {
		try (Writer out = new BufferedWriter(new FileWriter(new File("/dev/null")))) {
			final int loops = 1000;
			for(int i=1; i<=10; i++) {
				long startTime = System.nanoTime();
				for(int j=0; j<loops; j++) {
					simulateCalls(factory, out);
				}
				long endTime = System.nanoTime();
				System.out.println(factory.getName() + ": " + i + ": Simulated " + loops + " calls in " + BigDecimal.valueOf(endTime - startTime, 6)+" ms");
			}
		}
	}

	/**
	 * Performs the same set of calls that were performed in JSP request for:
	 *
	 * <a href="http://localhost:11156/essential-mining.com/purchase/domains.jsp?cartIndex=2&amp;ui.lang=en&amp;cookie%3AshoppingCart=jPAbu2Xc1JKVicbIGilVSW">http://localhost:11156/essential-mining.com/purchase/domains.jsp?cartIndex=2&amp;ui.lang=en&amp;cookie%3AshoppingCart=jPAbu2Xc1JKVicbIGilVSW</a>
	 */
	protected abstract void simulateCalls(BufferWriterFactory factory, Writer out) throws IOException;
}

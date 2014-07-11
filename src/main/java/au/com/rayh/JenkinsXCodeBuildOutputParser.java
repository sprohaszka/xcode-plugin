/*
 * The MIT License
 *
 * Copyright (c) 2011 Ray Yamamoto Hilton
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.rayh;

import hudson.FilePath;
import hudson.model.TaskListener;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 *
 * @author ray
 */
public class JenkinsXCodeBuildOutputParser extends XCodeBuildOutputParser {
    protected TaskListener buildListener;
    private FilePath testReportsDir;
    /**
     * Use to write output from xcodebuild cmd
     */
    private PrintWriter printWriter;

	public JenkinsXCodeBuildOutputParser(FilePath workspace,
                                         TaskListener buildListener) throws IOException, InterruptedException {
		this(workspace, buildListener, false);
    }

    public JenkinsXCodeBuildOutputParser(FilePath workspace,
                                         TaskListener buildListener,
                                         boolean hasToSaveOutput) throws IOException, InterruptedException {
        super();
        this.buildListener = buildListener;
        this.captureOutputStream = new LineBasedFilterOutputStream(hasToSaveOutput);

        testReportsDir = workspace.child("test-reports");
        testReportsDir.mkdirs();
        if (hasToSaveOutput) {
            FilePath rawOutput = workspace.child("xcodebuild.log");
            printWriter = new PrintWriter(rawOutput.write());
        }
    }

    private class LineBasedFilterOutputStream extends FilterOutputStream {
        StringBuilder buffer = new StringBuilder();
        private Boolean hasToSaveOutput;

        public LineBasedFilterOutputStream(Boolean hasToSaveOutput) {
            super(buildListener.getLogger());
            this.hasToSaveOutput = hasToSaveOutput;
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            if((char)b == '\n') {
                try {
                    handleLine(buffer.toString());
                    if (hasToSaveOutput) {
                        writeRawLine(buffer.toString());
                    }
                    buffer = new StringBuilder();
                } catch(Exception e) {  // Very fugly
                    buildListener.fatalError(e.getMessage(), e);
                    throw new IOException(e);
                }
            } else {
                buffer.append((char)b);
            }
        }

        @Override
        public void close() throws IOException {
            super.close();
            closeRawPrinter();
        }
    }

    private void writeRawLine(String line) throws IOException, InterruptedException {
        printWriter.write(line);
        printWriter.write("\n");
    }

    private void closeRawPrinter() {
        printWriter.close();
    }

	@Override
	protected OutputStream outputForSuite() throws IOException,
			InterruptedException {
		return testReportsDir.child("TEST-" + currentTestSuite.getName() + ".xml").write();
	}
}

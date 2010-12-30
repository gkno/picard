/*
 * The MIT License
 *
 * Copyright (c) 2009 The Broad Institute
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
package net.sf.picard.cmdline;

import java.io.File;
import java.util.*;

import net.sf.picard.metrics.Header;
import net.sf.picard.metrics.MetricBase;
import net.sf.picard.metrics.MetricsFile;
import net.sf.picard.metrics.StringHeader;
import net.sf.picard.util.Log;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMFileWriterImpl;
import net.sf.samtools.util.BlockCompressedOutputStream;
import net.sf.samtools.util.BlockCompressedStreamConstants;

/**
 * Abstract class to facilitate writing command-line programs.
 *
 * To use:
 *
 * 1. Extend this class with a concrete class that has data members annotated with @Option, @PositionalArguments
 * and/or @Usage annotations.
 *
 * 2. If there is any custom command-line validation, override customCommandLineValidation().  When this method is
 * called, the command line has been parsed and set into the data members of the concrete class.
 *
 * 3. Implement a method doWork().  This is called after successful comand-line processing.  The value it returns is
 * the exit status of the program.  It is assumed that the concrete class emits any appropriate error message before
 * returning non-zero.  doWork() may throw unchecked exceptions, which are caught and reported appropriately.
 *
 * 4. Implement the following static method in the concrete class:
 *
 *     public static void main(String[] argv) {
        new MyConcreteClass().instanceMainWithExit(argv);
    }


 */
public abstract class CommandLineProgram {

    @Option(common=true)
    public File TMP_DIR = (System.getProperty("java.io.tmpdir").endsWith("/" + System.getProperty("user.name"))?
            new File(System.getProperty("java.io.tmpdir")):
            new File(System.getProperty("java.io.tmpdir"), System.getProperty("user.name")));

    @Option(doc = "Control verbosity of logging.", common=true)
    public Log.LogLevel VERBOSITY = Log.LogLevel.INFO;

    @Option(doc = "Whether to suppress job-summary info on System.err.", common=true)
    public Boolean QUIET = false;

    @Option(doc = "Validation stringency for all SAM files read by this program.  Setting stringency to SILENT " +
            "can improve performance when processing a BAM file in which variable-length data (read, qualities, tags) " +
            "do not otherwise need to be decoded.", common=true)
    public SAMFileReader.ValidationStringency VALIDATION_STRINGENCY = SAMFileReader.ValidationStringency.DEFAULT_STRINGENCY;

    @Option(doc = "Compression level for all compressed files created (e.g. BAM and GELI).", common=true)
    public int COMPRESSION_LEVEL = BlockCompressedStreamConstants.DEFAULT_COMPRESSION_LEVEL;

    @Option(doc = "When writing SAM files that need to be sorted, this will specify the number of records stored in RAM before spilling to disk. Increasing this number reduces the number of file handles needed to sort a SAM file, and increases the amount of RAM needed.", optional=true, common=true)
    public Integer MAX_RECORDS_IN_RAM = SAMFileWriterImpl.getDefaultMaxRecordsInRam();

    @Option(doc = "Whether to create a BAM index when writing a coordinate-sorted BAM file.", common=true)
    public Boolean CREATE_INDEX = false;

    @Option(doc="Whether to create an MD5 digest for any BAM files created.  ", common=true)
    public boolean CREATE_MD5_FILE = false;

    private final String standardUsagePreamble = CommandLineParser.getStandardUsagePreamble(getClass());

    /**
    * Initialized in parseArgs.  Subclasses may want to access this to do their
    * own validation, and then print usage using commandLineParser.
    */
    private CommandLineParser commandLineParser;

    private final List<Header> defaultHeaders = new ArrayList<Header>();

    /**
    * The reconstructed commandline used to run this program. Used for logging
    * and debugging.
    */
    private String commandLine;

    /**
    * Do the work after command line has been parsed. RuntimeException may be
    * thrown by this method, and are reported appropriately.
    * @return program exit status.
    */
    protected abstract int doWork();

    public void instanceMainWithExit(final String[] argv) {
        System.exit(instanceMain(argv));
    }

    public int instanceMain(final String[] argv) {
        if (!parseArgs(argv)) {
            return 1;
        }

        // Build the default headers
        final Date startDate = new Date();
        this.defaultHeaders.add(new StringHeader(commandLine));
        this.defaultHeaders.add(new StringHeader("Started on: " + startDate));

        Log.setGlobalLogLevel(VERBOSITY);
        SAMFileReader.setDefaultValidationStringency(VALIDATION_STRINGENCY);
        BlockCompressedOutputStream.setDefaultCompressionLevel(COMPRESSION_LEVEL);

        if (MAX_RECORDS_IN_RAM != null) {
            SAMFileWriterImpl.setDefaultMaxRecordsInRam(MAX_RECORDS_IN_RAM);
        }

        if (CREATE_INDEX){
            SAMFileWriterFactory.setDefaultCreateIndexWhileWriting(true);
        }

        SAMFileWriterFactory.setDefaultCreateMd5File(CREATE_MD5_FILE);

        if (!TMP_DIR.exists()) {
            // Intentially not checking the return value, because it may be that the program does not
            // need a tmp_dir. If this fails, the problem will be discovered downstream.
            TMP_DIR.mkdirs();
        }
        System.setProperty("java.io.tmpdir", TMP_DIR.getAbsolutePath());
        if (!QUIET) {
            System.err.println("[" + new Date() + "] " + commandLine);
        }
        final int ret;

        try {
            ret = doWork();
        } finally {
            // Emit the time even if program throws
            if (!QUIET) {
                System.err.println("[" + new Date() + "] " + getClass().getName() + " done.");
                System.err.println("Runtime.totalMemory()=" + Runtime.getRuntime().totalMemory());
            }
        }
        return ret;
    }

    /**
    * Put any custom command-line validation in an override of this method.
    * clp is initialized at this point and can be used to print usage and access argv.
     * Any options set by command-line parser can be validated.
    * @return null if command line is valid.  If command line is invalid, returns an array of error message
    * to be written to the appropriate place.
    */
    protected String[] customCommandLineValidation() {
        return null;
    }

    /**
    *
    * @return true if command line is valid
    */
    protected boolean parseArgs(final String[] argv) {

        commandLineParser = new CommandLineParser(this);
        final boolean ret = commandLineParser.parseOptions(System.err, argv);
        commandLine = commandLineParser.getCommandLine();
        if (!ret) {
            return false;
        }
        final String[] customErrorMessages = customCommandLineValidation();
        if (customErrorMessages != null) {
            for (final String msg : customErrorMessages) {
                System.err.println(msg);
            }
            commandLineParser.usage(System.err, false);
            return false;
        }
        return true;
    }

    /** Gets a MetricsFile with default headers already written into it. */
    protected <A extends MetricBase,B extends Comparable<?>> MetricsFile<A,B> getMetricsFile() {
        final MetricsFile<A,B> file = new MetricsFile<A,B>();
        for (final Header h : this.defaultHeaders) {
            file.addHeader(h);
        }

        return file;
    }

    public String getStandardUsagePreamble() {
        return standardUsagePreamble;
    }

    public CommandLineParser getCommandLineParser() {
        return commandLineParser;
    }

    public String getProgramVersion() {
        return commandLineParser.getProgramVersion();
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setDefaultHeaders(List<Header> headers) {
        this.defaultHeaders.clear();
        this.defaultHeaders.addAll(headers);
    }

    public List<Header> getDefaultHeaders() {
        return this.defaultHeaders;
    }
}


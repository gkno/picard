/*
 * The MIT License
 *
 * Copyright (c) 2011 The Broad Institute
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
package net.sf.picard.util;

import net.sf.picard.PicardException;

import java.util.Iterator;

import net.sf.samtools.util.CloseableIterator;

/**
 * Class for parsing text files where each line consists of fields separated by whitespace.
 * Code is abstracted into this class so that we can optimize its performance over time.
 *
 * This class assumes that every line will have the same number of whitespace-separated "words"
 * and that lines that start with "#" are comments and should be ignored.
 *
 * Classes that extend this parser can do so simply by implementing their own constructors and the
 * readNextLine(), close(), and getFileName() methods.
 *
 * @author Kathleen Tibbetts
 */
public abstract class AbstractInputParser
extends AbstractIterator<String[]>
implements Iterable<String[]>, CloseableIterator<String[]> {

    private boolean treatGroupedDelimitersAsOne = true; // Whether multiple delimiters in succession should be treated as one
    private int wordCount = 0;      /* The number of delimiter-separated "words" per line of the file.
                                       We can save a little caclulation, or handle files with varying numbers of
                                       words per line, by specifying this if known in advance */
    private boolean skipBlankLines = true;

    /**
     * Closes this stream and releases any system resources associated with it.
     */
    public abstract void close();

    /**
     * @return the next line of text from the underlying stream(s) or null if there is no next line
     */
    protected abstract byte[] readNextLine();

    /**
     * @return  the name(s) of the file(s) being parsed, or null if no name is available
     */
    public abstract String getFileName();

    /**
     * @return an iterator over a set of elements of type String[]
     */
    public Iterator<String[]> iterator() {
        if (isIterating()) {
            throw new IllegalStateException("iterator() method can only be called once, before the" +
                    "first call to hasNext()");
        }
        hasNext();
        return this;
    }

    @Override
    protected String[] advance() {
        byte[] nextLine;
        do {
            nextLine = readNextLine();
        }
        while (nextLine != null && ((this.skipBlankLines && isBlank(nextLine)) || isComment(nextLine)));
        return nextLine == null ? null : parseLine(nextLine);
    }

    /**
     * This method represents the most efficient way (so far) to parse a line of whitespace-delimited text
     *
     * @param line the line to parse
     * @return  an array of all the "words"
     */
    private String[] parseLine(final byte[] line) {

        if (getWordCount() == 0) {
            calculateWordCount(line);
        }
        final String[] parts = new String[getWordCount()];
        boolean delimiter = true;
        int index=0;
        int start = 0;

        try
        {
            for (int i = 0; i < line.length; i++) {
                if (isDelimiter(line[i])) {
                    if (!delimiter) {
                        parts[index++] = new String(line,start,i-start);
                    }
                    else if(!isTreatGroupedDelimitersAsOne()) {
                        parts[index++] = null;
                    }
                    delimiter=true;
                }
                else {
                    if (delimiter)  start = i;
                    delimiter = false;
                }
            }
            if (!delimiter) {
                 parts[index] = new String(line,start,line.length-start);
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new PicardException("Unexpected number of elements found when parsing file " +
                    this.getFileName() + ": " + index + ".  Expected a maximum of " +
                    this.getWordCount() + " elements per line:" + new String(line,0,line.length));
        }
        return parts;
    }

    /**
     * Calculates the number of delimiter-separated "words" in a line and sets the value of <code>wordCount</code>
     *
     * @param line  representative line from the file
     */
    protected void calculateWordCount(final byte[] line) {
        int words = 0;
        boolean delimiter = true;
        for (final byte b : line) {
            if (isDelimiter(b)) {
                if (delimiter && !isTreatGroupedDelimitersAsOne()) words++;
                delimiter = true;
            } else {
                if (delimiter) words++;
                delimiter = false;
            }
        }
        if (delimiter && !isTreatGroupedDelimitersAsOne()) {
            words += 1;
        }
        setWordCount(words);
    }

    /**
     * Determines whether a given line is a comment
     *
     * @param line  the line to evaluate
     * @return  true if the line is a comment (and should be ignored) otherwise false
     */
    protected boolean isComment(final byte[] line) {
        return line.length > 0 && line[0] == '#';
    }

    /**
     * Determines whether a given line is a comment
     *
     * @param line  the line to evaluate
     * @return  true if the line is a comment (and should be ignored) otherwise false
     */
    protected boolean isBlank(final byte[] line) {
        return line.length == 0;
    }

    /**
     * Determines whether a given character is a delimiter
     *
     * @param b the character to evaluate
     * @return  true if <code>b</code> is a delimiter; otherwise false
     */
    protected boolean isDelimiter(final byte b) {
        return b == ' ' || b == '\t';
    }

    protected int getWordCount() { return wordCount; }
    protected void setWordCount(final int wordCount) { this.wordCount = wordCount; }
    protected boolean isTreatGroupedDelimitersAsOne() { return treatGroupedDelimitersAsOne; }
    protected void setTreatGroupedDelimitersAsOne(final boolean treatGroupedDelimitersAsOne) {
        this.treatGroupedDelimitersAsOne = treatGroupedDelimitersAsOne;
    }
    protected boolean isSkipBlankLines() { return this.skipBlankLines; }
    protected void setSkipBlankLines(final boolean skipBlankLines) {
        this.skipBlankLines = skipBlankLines;
    }
}

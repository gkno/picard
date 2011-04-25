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
package net.sf.picard.analysis.directed;

import net.sf.picard.metrics.MetricBase;

/**
 * Metrics about the alignment of cDNA reads within a SAM file to genes, produced by the CollectCDnaMetrics
 * program and usually stored in a file with the extension ".cDNA_metrics".  These metrics are counted base by base
 * rather than looking at an entire read at a time.
 */
public class CDnaMetrics extends MetricBase{
    /**
     * The total number of aligned PF bases.
     */
    public long ALIGNED_PF_BASES;

    /**
     * Number of bases in primary aligments that align to a coding base for some gene.
     */
    public long CODING_BASES;

    /**
     * Number of bases in primary aligments that align to a UTR base for some gene, and not a coding base.
     */
    public long UTR_BASES;

    /**
     * Number of bases in primary aligments that align to an intronic base for some gene, and not a coding or UTR base.
     */
    public long INTRONIC_BASES;

    /**
     * Number of bases in primary aligments that do not align to any gene.
     */
    public long INTRAGENIC_BASES;

    /**
     * CODING_BASES / ALIGNED_PF_BASES
     */
    public double PCT_CODING_BASES;

    /**
     * UTR_BASES / ALIGNED_PF_BASES
     */
    public double PCT_UTR_BASES;

    /**
     * INTRONIC_BASES / ALIGNED_PF_BASES
     */
    public double PCT_INTRONIC_BASES;

    /**
     * INTRAGENIC_BASES / ALIGNED_PF_BASES
     */
    public double PCT_INTRAGENIC_BASES;
}

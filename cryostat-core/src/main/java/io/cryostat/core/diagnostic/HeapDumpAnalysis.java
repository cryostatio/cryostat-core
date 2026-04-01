/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.core.diagnostic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmc.joverflow.batch.BatchProblemRecorder;
import org.openjdk.jmc.joverflow.batch.DetailedStats;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster.Collections;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster.DupArrays;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster.DupStrings;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster.HighSizeObjects;
import org.openjdk.jmc.joverflow.batch.ReferencedObjCluster.WeakHashMaps;
import org.openjdk.jmc.joverflow.heap.model.JavaClass;
import org.openjdk.jmc.joverflow.heap.model.JavaObject;
import org.openjdk.jmc.joverflow.heap.model.Snapshot;
import org.openjdk.jmc.joverflow.heap.parser.DumpCorruptedException;
import org.openjdk.jmc.joverflow.heap.parser.HeapDumpReader;
import org.openjdk.jmc.joverflow.heap.parser.HprofParsingCancelledException;
import org.openjdk.jmc.joverflow.heap.parser.ReadBuffer;
import org.openjdk.jmc.joverflow.stats.ObjectHistogram;
import org.openjdk.jmc.joverflow.stats.ObjectHistogram.ProblemFieldsEntry;
import org.openjdk.jmc.joverflow.stats.StandardStatsCalculator;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap.Entry;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class HeapDumpAnalysis {

    private HeapDumpReader reader;

    // Reference Chains
    public List<List<Collections>> collectionClusters;
    public List<List<DupArrays>> duplicateArrayClusters;
    public List<List<DupStrings>> duplicateStringClusters;
    public List<List<HighSizeObjects>> highSizeObjectClusters;
    public List<List<WeakHashMaps>> weakHashMapClusters;

    // Class Histogram
    public List<ObjectHistogram.Entry> objectHistogram;
    public HistogramStats histogramStats;

    // Fundamental Stats
    public FundamentalStats fundamentalStats;

    // Problem Fields (null)
    public List<ProblemFieldsEntry> nullProblemFields;
    // Problem Fields (nearly null)
    public List<ProblemFieldsEntry> nearNullProblemFields;
    // Problem Fields (null)
    public List<ProblemFieldsEntry> fullBytesFields;
    // Problem Fields (nearly null)
    public List<ProblemFieldsEntry> highBytesFields;

    // Classloader Stats
    public List<AggregateValue> classLoaderInstanceStats = new ArrayList<AggregateValue>();
    public List<AggregateValue> classLoaderClassStats = new ArrayList<AggregateValue>();

    // String Stats
    public CompressibleStringStats compressibleStringStats;
    public DuplicateStringStats duplicateStringStats;

    private HeapStats heapStats;
    private DetailedStats detailedStats;

    @SuppressFBWarnings({
        "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
        "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD"
    })
    public HeapDumpAnalysis(String heapDumpId, String jvmId, InputStream inputStream) {
        try {
            Path tmpFile =
                    Files.createTempFile(String.format("%s-%s", jvmId, heapDumpId), ".hprof");
            // Copy the heap dump from storage to a temporary file for analysis
            Files.copy(inputStream, tmpFile);

            // Parse the heap dump using the JOVerflow libraries
            VerboseOutputCollector vc = new VerboseOutputCollector();
            reader =
                    HeapDumpReader.createReader(
                            new ReadBuffer.CachedReadBufferFactory(
                                    tmpFile.toString(), calculateReadBufMemory()),
                            0,
                            vc);
            Snapshot snapshot = reader.read();
            BatchProblemRecorder recorder = new BatchProblemRecorder();
            StandardStatsCalculator ssc = new StandardStatsCalculator(snapshot, recorder, true);
            heapStats = ssc.calculate();
            // TODO: Should this be configurable?
            int minOvhdToReport = (int) heapStats.totalObjSize / 1000;
            detailedStats = recorder.getDetailedStats(minOvhdToReport);

            // Reference Chains
            collectionClusters = detailedStats.collectionClusters;
            duplicateArrayClusters = detailedStats.dupArrayClusters;
            duplicateStringClusters = detailedStats.dupStringClusters;
            highSizeObjectClusters = detailedStats.highSizeObjClusters;
            weakHashMapClusters = detailedStats.weakHashMapClusters;

            // Object Histogram
            // 0 lists the full histogram
            objectHistogram = heapStats.objHisto.getListSortedByInclusiveSize(0);
            // Fields that are null/zero/non-existent
            nullProblemFields = heapStats.objHisto.getListSortedByNullFieldsOvhd(1.0f);
            nearNullProblemFields = heapStats.objHisto.getListSortedByNullFieldsOvhd(0.9f);
            // Fields with unused high bytes (100th, 90th percentile)
            fullBytesFields = heapStats.objHisto.getListSortedByUnusedHiByteFieldsOvhd(1.0f);
            highBytesFields = heapStats.objHisto.getListSortedByUnusedHiByteFieldsOvhd(0.9f);
            histogramStats =
                    new HistogramStats(
                            heapStats.nClasses,
                            heapStats.nObjects,
                            heapStats.objHisto.calculateNumSmallInstClasses()[0],
                            heapStats.objHisto.calculateNumSmallInstClasses()[1]);

            // Fundamental Stats
            fundamentalStats =
                    new FundamentalStats(
                            heapStats.ptrSize,
                            heapStats.usingNarrowPointers,
                            heapStats.objHeaderSize,
                            heapStats.objAlignment,
                            heapStats.nObjects,
                            heapStats.nInstances,
                            heapStats.nObjectArrays,
                            heapStats.nValueArrays,
                            heapStats.totalObjSize,
                            heapStats.totalInstSize,
                            heapStats.totalObjArraySize,
                            heapStats.totalValueArraySize);

            // ClassLoader Stats
            for (Entry<JavaObject> e :
                    heapStats.classloaderStats.getCLInstToNumLoadedClasses().getEntries()) {
                classLoaderInstanceStats.add(new AggregateValue(e.key.valueAsString(), e.value));
            }
            for (Entry<JavaClass> e :
                    heapStats.classloaderStats.getClClazzToNumLoadedClasses().getEntries()) {
                classLoaderClassStats.add(new AggregateValue(e.key.valueAsString(), e.value));
            }

            // Compressible String Stats
            compressibleStringStats =
                    new CompressibleStringStats(
                            heapStats.compressibleStringStats.nTotalStrings,
                            heapStats.compressibleStringStats.totalUsedBackingArrayBytes,
                            heapStats.compressibleStringStats.nCompressedStrings,
                            heapStats.compressibleStringStats.compressedBackingArrayBytes,
                            heapStats.compressibleStringStats.nAsciiCharBackedStrings,
                            heapStats.compressibleStringStats.asciiCharBackingArrayBytes);

            // Duplicate String stats
            duplicateStringStats =
                    new DuplicateStringStats(
                            heapStats.dupStringStats.nStrings,
                            heapStats.dupStringStats.nUniqueStringValues,
                            heapStats.dupStringStats.nUniqueDupStringValues,
                            heapStats.dupStringStats.dupStringsOverhead);

            // Cleanup
            snapshot.discard();
            snapshot.resetReadBuffer(
                    new ReadBuffer.CachedReadBufferFactory(tmpFile.toString(), 25 * 1024 * 1024));
            Files.deleteIfExists(tmpFile);
        } catch (IOException | DumpCorruptedException | HprofParsingCancelledException e) {
            e.printStackTrace();
        }
    }

    @SuppressFBWarnings("DM_GC")
    // Taken from JMC's model generation for the JOverflow UI.
    // Dynamically determine read buffer size, we should have this be configurable
    private static int calculateReadBufMemory() {
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long availableMemory = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
        return (int) Math.min(1000 * 1024 * 1024, availableMemory / 3);
    }

    public record FundamentalStats(
            int pointerSize,
            boolean narrowPointers,
            int objectHeaderSize,
            int objectHeaderAlignment,
            int numObjects,
            int objectInstances,
            int objectArrays,
            int primitiveArrays,
            long objectSize,
            long instanceSize,
            long objArraySize,
            long primitiveSize) {}
    ;

    public record CompressibleStringStats(
            int stringObjects,
            long backingArrayBytes,
            int compressedStrings,
            long compressedStringBytes,
            int asciiStrings,
            long asciiStringBytes) {}
    ;

    public record HistogramStats(
            int totalClasses, int totalObjects, int zeroInstances, int singleInstances) {}
    ;

    public record DuplicateStringStats(
            int totalStrings, int uniqueStrings, int duplicateStrings, long overhead) {}
    ;

    public record AggregateValue(String value, long count) {}
    ;
}

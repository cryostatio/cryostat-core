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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.openjdk.jmc.joverflow.support.ClassAndOvhdCombo;
import org.openjdk.jmc.joverflow.support.ClassAndSizeCombo;
import org.openjdk.jmc.joverflow.support.HeapStats;
import org.openjdk.jmc.joverflow.support.PrimitiveArrayWrapper;
import org.openjdk.jmc.joverflow.support.RefChainElement;
import org.openjdk.jmc.joverflow.support.RefChainElementImpl;
import org.openjdk.jmc.joverflow.support.ReferenceChain;
import org.openjdk.jmc.joverflow.util.ObjectToIntMap.Entry;
import org.openjdk.jmc.joverflow.util.VerboseOutputCollector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class HeapDumpAnalysis {

    // Passed into the constructor
    private int readBufferMemoryLimit;

    // Reference Chains
    private List<ProblemCollection> problemCollections;
    private List<DuplicateArray> duplicateArrays;
    private List<DuplicateString> duplicateStrings;
    private List<HighSizeObject> highSizeObjects;
    private List<WeakHashMapEntry> weakHashMaps;

    // Class Histogram
    private List<HistogramEntry> objectHistogram;
    private HistogramStats histogramStats;

    // Fundamental Stats
    private FundamentalStats fundamentalStats;

    // Problem Fields (null)
    private List<ProblemField> nullProblemFields;
    // Problem Fields (nearly null)
    private List<ProblemField> nearNullProblemFields;
    // Problem Fields (null)
    private List<ProblemField> fullBytesFields;
    // Problem Fields (nearly null)
    private List<ProblemField> highBytesFields;

    // Classloader Stats
    private List<AggregateValue> classLoaderInstanceStats;
    private List<AggregateValue> classLoaderClassStats;

    // String Stats
    private CompressibleStringStats compressibleStringStats;
    private DuplicateStringStats duplicateStringStats;

    private HeapStats heapStats;
    private DetailedStats detailedStats;

    // Default Constructor for serializer
    public HeapDumpAnalysis() {
        this(0);
    }

    public HeapDumpAnalysis(int readBufferLimit) {
        readBufferMemoryLimit = readBufferLimit;
        objectHistogram = new ArrayList<HistogramEntry>();
        classLoaderInstanceStats = new ArrayList<AggregateValue>();
        classLoaderClassStats = new ArrayList<AggregateValue>();
        problemCollections = new ArrayList<ProblemCollection>();
        duplicateArrays = new ArrayList<DuplicateArray>();
        duplicateStrings = new ArrayList<DuplicateString>();
        highSizeObjects = new ArrayList<HighSizeObject>();
        weakHashMaps = new ArrayList<WeakHashMapEntry>();
    }

    public void analyze(InputStream heapDumpStream)
            throws IOException, DumpCorruptedException, HprofParsingCancelledException {
        Path tmpFile = Files.createTempFile("", ".hprof");
        // Copy the heap dump from storage to a temporary file for analysis
        Files.copy(heapDumpStream, tmpFile, StandardCopyOption.REPLACE_EXISTING);
        VerboseOutputCollector vc = new VerboseOutputCollector();
        HeapDumpReader reader =
                HeapDumpReader.createReader(
                        new ReadBuffer.CachedReadBufferFactory(
                                tmpFile.toString(), calculateReadBufMemory()),
                        0,
                        vc);
        Snapshot snapshot = reader.read();
        try {
            // Parse the heap dump using the JOVerflow libraries
            BatchProblemRecorder recorder = new BatchProblemRecorder();
            StandardStatsCalculator ssc = new StandardStatsCalculator(snapshot, recorder, true);
            heapStats = ssc.calculate();

            int minOvhdToReport = (int) heapStats.totalObjSize / 1000;
            detailedStats = recorder.getDetailedStats(minOvhdToReport);

            // Reference Chains
            List<List<Collections>> collectionClusters = detailedStats.collectionClusters;
            List<List<DupArrays>> duplicateArrayClusters = detailedStats.dupArrayClusters;
            List<List<DupStrings>> duplicateStringClusters = detailedStats.dupStringClusters;
            List<List<HighSizeObjects>> highSizeObjectClusters = detailedStats.highSizeObjClusters;
            List<List<WeakHashMaps>> weakHashMapClusters = detailedStats.weakHashMapClusters;

            // Problem Collections
            for (Collections r : collectionClusters.get(1)) {
                RefChainElement classAndField = r.getReferer();
                String fieldDefiningClass = getFieldDefiningClass(classAndField);
                String classAndFieldStr = classAndFieldLabel(classAndField, fieldDefiningClass);

                var problemClasses = new ArrayList<ProblemClass>();
                for (ClassAndOvhdCombo c : r.getList()) {
                    var problemClass =
                            new ProblemClass(
                                    c.getClazz().getHumanFriendlyNameWithLoaderIfNeeded(),
                                    c.getProblemKind().toString(),
                                    c.getNumInstances(),
                                    c.getOverhead());
                    problemClasses.add(problemClass);
                }

                problemCollections.add(
                        new ProblemCollection(
                                classAndFieldStr,
                                fieldDefiningClass,
                                r.getTotalOverhead(),
                                r.getNumBadObjects(),
                                r.getNumGoodCollections(),
                                problemClasses));
            }

            // Duplicate Arrays
            for (DupArrays d : duplicateArrayClusters.get(1)) {
                RefChainElement classAndField = d.getReferer();
                String fieldDefiningClass = getFieldDefiningClass(classAndField);
                String classAndFieldStr = classAndFieldLabel(classAndField, fieldDefiningClass);

                var aggregates = new ArrayList<AggregateValue>();
                for (Entry<PrimitiveArrayWrapper> e : d.getEntries()) {
                    var array = e.key.getArray();
                    aggregates.add(new AggregateValue(array.valueAsString(), e.value));
                }

                DuplicateArray dup =
                        new DuplicateArray(
                                classAndFieldStr,
                                fieldDefiningClass,
                                d.getNumBadObjects(),
                                d.getNumNonDupArrays(),
                                d.getTotalOverhead(),
                                aggregates);
                duplicateArrays.add(dup);
            }

            // Duplicate Strings
            for (DupStrings d : duplicateStringClusters.get(1)) {
                RefChainElement classAndField = d.getReferer();
                String fieldDefiningClass = getFieldDefiningClass(classAndField);
                String classAndFieldStr = classAndFieldLabel(classAndField, fieldDefiningClass);

                var aggregates = new ArrayList<AggregateValue>();
                for (Entry<String> e : d.getEntries()) {
                    aggregates.add(new AggregateValue(e.key, e.value));
                }

                DuplicateString dup =
                        new DuplicateString(
                                classAndFieldStr,
                                fieldDefiningClass,
                                d.getTotalOverhead(),
                                d.getNumBadObjects(),
                                d.getNumDupBackingCharArrays(),
                                d.getNumNonDupStrings(),
                                aggregates);
                duplicateStrings.add(dup);
            }

            // HighSizeObjects
            for (HighSizeObjects h : highSizeObjectClusters.get(1)) {
                RefChainElement classAndField = h.getReferer();
                String fieldDefiningClass = getFieldDefiningClass(classAndField);
                String classAndFieldStr = classAndFieldLabel(classAndField, fieldDefiningClass);

                var objectEntries = new ArrayList<ObjectEntry>();
                for (ClassAndSizeCombo c : h.getList()) {
                    objectEntries.add(
                            new ObjectEntry(
                                    c.getClazz().getHumanFriendlyNameWithLoaderIfNeeded(),
                                    c.getNumInstances(),
                                    c.getSizeOrOvhd()));
                }

                HighSizeObject obj =
                        new HighSizeObject(
                                classAndFieldStr,
                                fieldDefiningClass,
                                h.getTotalOverhead(),
                                h.getNumBadObjects(),
                                objectEntries);
                highSizeObjects.add(obj);
            }

            // WeakHashMaps
            for (WeakHashMaps w : weakHashMapClusters.get(1)) {
                RefChainElement classAndField = w.getReferer();
                String classAndFieldStr = ReferenceChain.toStringInStraightOrder(classAndField);

                String fieldDefiningClass =
                        getFieldDefiningClassFromFieldRefChain(
                                ReferenceChain.getRootElement(classAndField));
                if (fieldDefiningClass != null) {
                    classAndFieldStr += " (defined in " + fieldDefiningClass + ")";
                }
                WeakHashMapEntry entry =
                        new WeakHashMapEntry(
                                classAndFieldStr,
                                fieldDefiningClass,
                                w.getTotalOverhead(),
                                w.getNumBadObjects(),
                                Arrays.asList(w.getClasses()));
                weakHashMaps.add(entry);
            }

            // Object Histogram
            // 0 lists the full histogram
            for (ObjectHistogram.Entry e : heapStats.objHisto.getListSortedByInclusiveSize(0)) {
                objectHistogram.add(
                        new HistogramEntry(
                                e.getClazz().getHumanFriendlyNameWithLoaderIfNeeded(),
                                e.getNumInstances(),
                                e.getTotalInclusiveSize(),
                                e.getTotalShallowSize()));
            }

            // Fields that are null/zero/non-existent
            nullProblemFields =
                    parseProblemFields(heapStats.objHisto.getListSortedByNullFieldsOvhd(1.0f));
            nearNullProblemFields =
                    parseProblemFields(heapStats.objHisto.getListSortedByNullFieldsOvhd(0.9f));
            // Fields with unused high bytes (100th, 90th percentile)
            fullBytesFields =
                    parseProblemFields(
                            heapStats.objHisto.getListSortedByUnusedHiByteFieldsOvhd(1.0f));
            highBytesFields =
                    parseProblemFields(
                            heapStats.objHisto.getListSortedByUnusedHiByteFieldsOvhd(0.9f));

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

        } finally {
            // Clean up the temporary file and reset the memory buffer
            snapshot.discard();
            snapshot.resetReadBuffer(
                    new ReadBuffer.CachedReadBufferFactory(tmpFile.toString(), 25 * 1024 * 1024));
            Files.deleteIfExists(tmpFile);
        }
    }

    @SuppressFBWarnings("DM_GC")
    // Taken from JMC's model generation for the JOverflow UI.
    // Dynamically determine read buffer size, we should have this be configurable
    private int calculateReadBufMemory() {
        if (readBufferMemoryLimit == 0) {
            System.gc();
            Runtime runtime = Runtime.getRuntime();
            long availableMemory =
                    runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
            return (int) Math.min(1000 * 1024 * 1024, availableMemory / 3);
        } else {
            return readBufferMemoryLimit;
        }
    }

    private String getFieldDefiningClass(RefChainElement r) {
        return getFieldDefiningClassFromFieldRefChain(ReferenceChain.getRootElement(r));
    }

    private String classAndFieldLabel(RefChainElement r, String fieldDefiningClass) {
        String classAndFieldStr = ReferenceChain.toStringInStraightOrder(r);
        if (fieldDefiningClass != null) {
            classAndFieldStr += " (defined in " + fieldDefiningClass + ")";
        }
        return classAndFieldStr;
    }

    private static String getFieldDefiningClassFromFieldRefChain(RefChainElement desc) {
        if (!(desc instanceof RefChainElementImpl.AbstractField)) {
            return null;
        }

        RefChainElementImpl.AbstractField fieldDesc = (RefChainElementImpl.AbstractField) desc;
        JavaClass clazz = fieldDesc.getJavaClass();
        int fieldIdx = fieldDesc.getFieldIdx();

        JavaClass defClazz = clazz.getDeclaringClassForField(fieldIdx);
        if (defClazz == clazz || defClazz == null) {
            return null;
        } else {
            return defClazz.getName();
        }
    }

    private List<ProblemField> parseProblemFields(List<ProblemFieldsEntry> entries) {
        var returnVal = new ArrayList<ProblemField>();
        for (ProblemFieldsEntry e : entries) {
            var fieldList = new ArrayList<Field>();
            for (int i = 0; i < e.getProblemFieldNames().length; i++) {
                fieldList.add(
                        new Field(
                                e.getProblemFieldDeclaringClasses()[i]
                                        .getHumanFriendlyNameWithLoaderIfNeeded(),
                                e.getProblemFieldNames()[i],
                                e.getPerFieldOvhd()[i]));
            }
            returnVal.add(
                    new ProblemField(
                            e.getClazz().getHumanFriendlyNameWithLoaderIfNeeded(),
                            e.getNumInstances(),
                            fieldList,
                            e.getAllProblemFieldsOvhd(),
                            e.getStatus().toString()));
        }
        return returnVal;
    }

    public List<ProblemCollection> getProblemCollections() {
        return new ArrayList<ProblemCollection>(problemCollections);
    }

    public List<DuplicateArray> getDuplicateArrays() {
        return new ArrayList<DuplicateArray>(duplicateArrays);
    }

    public List<DuplicateString> getDuplicateStrings() {
        return new ArrayList<DuplicateString>(duplicateStrings);
    }

    public List<HighSizeObject> getHighSizeObjects() {
        return new ArrayList<HighSizeObject>(highSizeObjects);
    }

    public List<WeakHashMapEntry> getWeakHashMaps() {
        return new ArrayList<WeakHashMapEntry>(weakHashMaps);
    }

    public List<HistogramEntry> getObjectHistogram() {
        return new ArrayList<HistogramEntry>(objectHistogram);
    }

    public HistogramStats getHistogramStats() {
        return histogramStats;
    }

    public FundamentalStats getFundamentalStats() {
        return fundamentalStats;
    }

    public List<ProblemField> getNullProblemFields() {
        return new ArrayList<ProblemField>(nullProblemFields);
    }

    public List<ProblemField> getNearNullProblemFields() {
        return new ArrayList<ProblemField>(nearNullProblemFields);
    }

    public List<ProblemField> getFullBytesFields() {
        return new ArrayList<ProblemField>(fullBytesFields);
    }

    public List<ProblemField> getHighBytesFields() {
        return new ArrayList<ProblemField>(highBytesFields);
    }

    public List<AggregateValue> getClassLoaderInstanceStats() {
        return new ArrayList<AggregateValue>(classLoaderInstanceStats);
    }

    public List<AggregateValue> getClassLoaderClassStats() {
        return new ArrayList<AggregateValue>(classLoaderClassStats);
    }

    public CompressibleStringStats getCompressibleStringStats() {
        return compressibleStringStats;
    }

    public DuplicateStringStats getDuplicateStringStats() {
        return duplicateStringStats;
    }

    public record HistogramEntry(
            String clazz, int numInstances, long inclusiveSize, long shallowSize) {}
    ;

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

    public record ProblemClass(String clazz, String problemKind, int numInstances, int overhead) {}
    ;

    public record ProblemCollection(
            String classAndField,
            String definingClass,
            int overhead,
            int badObjs,
            int goodCollections,
            List<ProblemClass> classAndOvhds) {

        public ProblemCollection {
            classAndOvhds = List.copyOf(classAndOvhds);
        }

        public List<ProblemClass> getClassAndOvhds() {
            return new ArrayList<ProblemClass>(classAndOvhds);
        }
    }
    ;

    public record DuplicateArray(
            String classAndField,
            String definingClass,
            int overhead,
            int badObjs,
            int nonDupArrays,
            List<AggregateValue> aggregates) {

        public DuplicateArray {
            aggregates = List.copyOf(aggregates);
        }

        public List<AggregateValue> getAggregates() {
            return new ArrayList<AggregateValue>(aggregates);
        }
    }
    ;

    public record DuplicateString(
            String classAndField,
            String definingClass,
            int overhead,
            int badObjs,
            int dupBackingCharArrays,
            int nonDupStrings,
            List<AggregateValue> aggregates) {

        public DuplicateString {
            aggregates = List.copyOf(aggregates);
        }

        public List<AggregateValue> getAggregateValues() {
            return new ArrayList<AggregateValue>(aggregates);
        }
    }
    ;

    public record ObjectEntry(String clazz, int numInstances, int overhead) {}
    ;

    public record HighSizeObject(
            String classAndField,
            String definingClass,
            int overhead,
            int badObjs,
            List<ObjectEntry> classAndSizeCombos) {
        public HighSizeObject {
            classAndSizeCombos = List.copyOf(classAndSizeCombos);
        }

        public List<ObjectEntry> getClassAndSizeCombos() {
            return new ArrayList<ObjectEntry>(classAndSizeCombos);
        }
    }

    public record WeakHashMapEntry(
            String classAndField,
            String definingClass,
            int overhead,
            int badObjs,
            List<String> classes) {
        public WeakHashMapEntry {
            classes = List.copyOf(classes);
        }

        public List<String> getClasses() {
            return new ArrayList<String>(classes);
        }
    }
    ;

    public record ProblemField(
            String clazz, int numInstances, List<Field> fields, long overhead, String problemKind) {
        public ProblemField {
            fields = List.copyOf(fields);
        }

        public List<Field> getFields() {
            return new ArrayList<Field>(fields);
        }
    }
    ;

    public record Field(String clazz, String field, long overhead) {}
    ;
}

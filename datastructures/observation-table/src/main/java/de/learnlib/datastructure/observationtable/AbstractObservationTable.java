/* Copyright (C) 2013-2019 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.datastructure.observationtable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Observation table class.
 * <p>
 * An observation table (OT) is the central data structure used by Angluin's L* algorithm, as described in the paper
 * "Learning Regular Sets from Queries and Counterexamples".
 * <p>
 * An observation table is a two-dimensional table, with rows indexed by prefixes, and columns indexed by suffixes. For
 * a prefix <code>u</code> and a suffix <code>v</code>, the respective cell contains the result of the membership query
 * <code>(u, v)</code>.
 * <p>
 * The set of prefixes (row labels) is divided into two disjoint sets: short and long prefixes. Each long prefix is a
 * one-letter extension of a short prefix; conversely, every time a prefix is added to the set of short prefixes, all
 * possible one-letter extensions are added to the set of long prefixes.
 * <p>
 * In order to derive a well-defined hypothesis from an observation table, it must satisfy two properties: closedness
 * and consistency. <ul> <li>An observation table is <b>closed</b> iff for each long prefix <code>u</code> there exists
 * a short prefix <code>u'</code> such that the row contents for both prefixes are equal. <li>An observation table is
 * <b>consistent</b> iff for every two short prefixes <code>u</code> and <code>u'</code> with identical row contents, it
 * holds that for every input symbol <code>a</code> the rows indexed by <code>ua</code> and <code>u'a</code> also have
 * identical contents. </ul>
 *
 * @param <I>
 *         input symbol type
 * @param <D>
 *         output domain type
 *
 * @author Malte Isberner
 */
public abstract class AbstractObservationTable<I, D> implements MutableObservationTable<I, D>, Serializable {

    private static final Integer NO_ENTRY = null; // TODO: replace with primitive specialization
    protected final List<RowImpl<I>> shortPrefixRows = new ArrayList<>();
    // private static final int NO_ENTRY = -1;
    private final List<RowImpl<I>> longPrefixRows = new ArrayList<>();
    private final List<RowImpl<I>> allRows = new ArrayList<>();
    private final List<List<D>> allRowContents = new ArrayList<>();
    private final List<RowImpl<I>> canonicalRows = new ArrayList<>();
    // private final TObjectIntMap<List<D>> rowContentIds = new TObjectIntHashMap<>(10, 0.75f, NO_ENTRY);
    private final Map<List<D>, Integer> rowContentIds = new HashMap<>(); // TODO: replace with primitive specialization
    private final Map<Word<I>, RowImpl<I>> rowMap = new HashMap<>();
    private final List<Word<I>> suffixes = new ArrayList<>();
    private final Set<Word<I>> suffixSet = new HashSet<>();
    private transient Alphabet<I> alphabet;
    private int numRows;
    protected boolean initialConsistencyCheckRequired;

    /**
     * Constructor.
     *
     * @param alphabet
     *         the learning alphabet.
     */
    public AbstractObservationTable(Alphabet<I> alphabet) {
        this.alphabet = alphabet;
    }

    protected static <I, D> void buildQueries(List<DefaultQuery<I, D>> queryList,
                                              Word<I> prefix,
                                              List<? extends Word<I>> suffixes) {
        for (Word<I> suffix : suffixes) {
            queryList.add(new DefaultQuery<>(prefix, suffix));
        }
    }

    protected void checkInitialShortPrefixes(List<Word<I>> initialShortPrefixes) {
        if (!allRows.isEmpty()) {
            throw new IllegalStateException("Called initialize, but there are already rows present");
        }

        if (!checkPrefixClosed(initialShortPrefixes)) {
            throw new IllegalArgumentException("Initial short prefixes are not prefix-closed");
        }

        if (!initialShortPrefixes.get(0).isEmpty()) {
            throw new IllegalArgumentException("First initial short prefix MUST be the empty word!");
        }
    }

    protected List<Word<I>> initializeSuffixes(List<Word<I>> initialSuffixes) {
        if (!suffixSet.isEmpty() || !suffixes.isEmpty()) {
            throw new IllegalStateException("Trying to initialize suffixes, but there are already suffixes present");
        }

        for (Word<I> suffix : initialSuffixes) {
            if (suffixSet.add(suffix)) {
                suffixes.add(suffix);
            }
        }

        return suffixes;
    }

    private static <I> boolean checkPrefixClosed(Collection<? extends Word<I>> initialShortPrefixes) {
        Set<Word<I>> prefixes = new HashSet<>(initialShortPrefixes);

        for (Word<I> pref : initialShortPrefixes) {
            if (!pref.isEmpty()) {
                if (!prefixes.contains(pref.prefix(-1))) {
                    return false;
                }
            }
        }

        return true;
    }

    protected RowImpl<I> createSpRow(Word<I> prefix) {
        RowImpl<I> newRow = new RowImpl<>(prefix, numRows++, alphabet.size());
        allRows.add(newRow);
        rowMap.put(prefix, newRow);
        shortPrefixRows.add(newRow);
        return newRow;
    }

    protected RowImpl<I> createLpRow(Word<I> prefix) {
        RowImpl<I> newRow = new RowImpl<>(prefix, numRows++);
        allRows.add(newRow);
        rowMap.put(prefix, newRow);
        int idx = longPrefixRows.size();
        longPrefixRows.add(newRow);
        newRow.setLpIndex(idx);
        return newRow;
    }

    /**
     * Fetches the given number of query responses and adds them to the specified output list. Also, the query iterator
     * is advanced accordingly.
     *
     * @param queryIt
     *         the query iterator
     * @param output
     *         the output list to write to
     * @param numSuffixes
     *         the number of suffixes (queries)
     */
    protected static <I, D> void fetchResults(Iterator<DefaultQuery<I, D>> queryIt, List<D> output, int numSuffixes) {
        for (int j = 0; j < numSuffixes; j++) {
            DefaultQuery<I, D> qry = queryIt.next();
            output.add(qry.getOutput());
        }
    }

    protected boolean processContents(RowImpl<I> row, List<D> rowContents, boolean makeCanonical) {
        Integer contentId; // TODO: replace with primitive specialization
        // int contentId;
        boolean added = false;
        contentId = rowContentIds.get(rowContents);
        if (contentId == NO_ENTRY) {
            contentId = numberOfDistinctRows();
            rowContentIds.put(rowContents, contentId);
            allRowContents.add(rowContents);
            added = true;
            if (makeCanonical) {
                canonicalRows.add(row);
            } else {
                canonicalRows.add(null);
            }
        }
        row.setRowContentId(contentId);
        return added;
    }

    @Override
    public int numberOfDistinctRows() {
        return allRowContents.size();
    }

    @Override
    public List<List<Row<I>>> addSuffix(Word<I> suffix, MembershipOracle<I, D> oracle) {
        return addSuffixes(Collections.singletonList(suffix), oracle);
    }

    @Override
    public List<List<Row<I>>> addSuffixes(Collection<? extends Word<I>> newSuffixes, MembershipOracle<I, D> oracle) {
        int oldSuffixCount = suffixes.size();
        // we need a stable iteration order, and only List guarantees this
        List<Word<I>> newSuffixList = new ArrayList<>();
        for (Word<I> suffix : newSuffixes) {
            if (suffixSet.add(suffix)) {
                newSuffixList.add(suffix);
            }
        }

        if (newSuffixList.isEmpty()) {
            return Collections.emptyList();
        }

        int numNewSuffixes = newSuffixList.size();

        int numSpRows = shortPrefixRows.size();
        int rowCount = numSpRows + longPrefixRows.size();

        List<DefaultQuery<I, D>> queries = new ArrayList<>(rowCount * numNewSuffixes);

        for (RowImpl<I> row : shortPrefixRows) {
            buildQueries(queries, row.getLabel(), newSuffixList);
        }

        for (RowImpl<I> row : longPrefixRows) {
            buildQueries(queries, row.getLabel(), newSuffixList);
        }

        oracle.processQueries(queries);

        Iterator<DefaultQuery<I, D>> queryIt = queries.iterator();

        for (RowImpl<I> row : shortPrefixRows) {
            List<D> rowContents = allRowContents.get(row.getRowContentId());
            if (rowContents.size() == oldSuffixCount) {
                rowContentIds.remove(rowContents);
                fetchResults(queryIt, rowContents, numNewSuffixes);
                rowContentIds.put(rowContents, row.getRowContentId());
            } else {
                List<D> newContents = new ArrayList<>(oldSuffixCount + numNewSuffixes);
                newContents.addAll(rowContents.subList(0, oldSuffixCount));
                fetchResults(queryIt, newContents, numNewSuffixes);
                processContents(row, newContents, true);
            }
        }

        List<List<Row<I>>> unclosed = new ArrayList<>();
        numSpRows = numberOfDistinctRows();

        for (RowImpl<I> row : longPrefixRows) {
            List<D> rowContents = allRowContents.get(row.getRowContentId());
            if (rowContents.size() == oldSuffixCount) {
                rowContentIds.remove(rowContents);
                fetchResults(queryIt, rowContents, numNewSuffixes);
                rowContentIds.put(rowContents, row.getRowContentId());
            } else {
                List<D> newContents = new ArrayList<>(oldSuffixCount + numNewSuffixes);
                newContents.addAll(rowContents.subList(0, oldSuffixCount));
                fetchResults(queryIt, newContents, numNewSuffixes);
                if (processContents(row, newContents, false)) {
                    unclosed.add(new ArrayList<>());
                }

                int id = row.getRowContentId();
                if (id >= numSpRows) {
                    unclosed.get(id - numSpRows).add(row);
                }
            }
        }

        this.suffixes.addAll(newSuffixList);

        return unclosed;
    }

    @Override
    public boolean isInitialConsistencyCheckRequired() {
        return initialConsistencyCheckRequired;
    }

    @Override
    public List<List<Row<I>>> addShortPrefixes(List<? extends Word<I>> shortPrefixes, MembershipOracle<I, D> oracle) {
        List<Row<I>> toSpRows = new ArrayList<>();

        for (Word<I> sp : shortPrefixes) {
            RowImpl<I> row = rowMap.get(sp);
            if (row != null) {
                if (row.isShortPrefixRow()) {
                    continue;
                }
            } else {
                row = createSpRow(sp);
            }
            toSpRows.add(row);
        }

        return toShortPrefixes(toSpRows, oracle);
    }

    protected void makeShort(RowImpl<I> row) {
        if (row.isShortPrefixRow()) {
            return;
        }

        int lastIdx = longPrefixRows.size() - 1;
        RowImpl<I> last = longPrefixRows.get(lastIdx);
        int rowIdx = row.getLpIndex();
        longPrefixRows.remove(lastIdx);
        if (last != row) {
            longPrefixRows.set(rowIdx, last);
            last.setLpIndex(rowIdx);
        }

        shortPrefixRows.add(row);
        row.makeShort(alphabet.size());

        if (row.hasContents()) {
            int cid = row.getRowContentId();
            if (canonicalRows.get(cid) == null) {
                canonicalRows.set(cid, row);
            }
        }
    }

    protected static <I, D> void buildRowQueries(List<DefaultQuery<I, D>> queryList,
                                                 List<? extends Row<I>> rows,
                                                 List<? extends Word<I>> suffixes) {
        for (Row<I> row : rows) {
            buildQueries(queryList, row.getLabel(), suffixes);
        }
    }

    @Override
    public List<D> rowContents(Row<I> row) {
        return allRowContents.get(row.getRowContentId());
    }

    @Override
    public RowImpl<I> getRow(int rowId) {
        return allRows.get(rowId);
    }

    @Override
    public RowImpl<I> getRow(Word<I> prefix) {
        return rowMap.get(prefix);
    }

    @Override
    public int numberOfRows() {
        return shortPrefixRows.size() + longPrefixRows.size();
    }

    @Override
    public List<Word<I>> getSuffixes() {
        return suffixes;
    }

    @Override
    public boolean isInitialized() {
        return !allRows.isEmpty();
    }

    @Override
    public Alphabet<I> getInputAlphabet() {
        return alphabet;
    }

    /**
     * This is an internal method used for de-serializing. Do not deliberately set input alphabets.
     *
     * @param alphabet
     *         the input alphabet corresponding to the previously serialized one.
     */
    public void setInputAlphabet(Alphabet<I> alphabet) {
        this.alphabet = alphabet;
    }

    @Override
    public Word<I> transformAccessSequence(Word<I> word) {
        Row<I> current = shortPrefixRows.get(0);

        for (I sym : word) {
            current = getRowSuccessor(current, sym);
            current = canonicalRows.get(current.getRowContentId());
        }

        return current.getLabel();
    }

    @Override
    public boolean isAccessSequence(Word<I> word) {
        Row<I> current = shortPrefixRows.get(0);

        for (I sym : word) {
            current = getRowSuccessor(current, sym);
            if (!isCanonical(current)) {
                return false;
            }
        }

        return true;
    }

    private boolean isCanonical(Row<I> row) {
        if (!row.isShortPrefixRow()) {
            return false;
        }
        int contentId = row.getRowContentId();
        return (canonicalRows.get(contentId) == row);
    }

    @NonNull
    @Override
    public List<Row<I>> getShortPrefixRows() {
        return Collections.unmodifiableList(shortPrefixRows);
    }

    @NonNull
    @Override
    public Collection<Row<I>> getLongPrefixRows() {
        return Collections.unmodifiableList(longPrefixRows);
    }
}

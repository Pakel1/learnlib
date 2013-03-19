package de.learnlib.lstar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.util.Words;
import de.learnlib.api.MembershipOracle;
import de.learnlib.api.Query;
import de.learnlib.lstar.table.Row;


/**
 * Observation table class.
 * 
 * An observation table (OT) is the central data structure used by Angluin's L* algorithm,
 * as described in the paper "Learning Regular Sets from Queries and Counterexamples".
 * 
 * An observation table is a two-dimensional table, with rows indexed by prefixes,
 * and columns indexed by suffixes. For a prefix <code>u</code> and a suffix <code>v</code>,
 * the respective cell contains the result of the membership query <code>(u, v)</code>.
 * 
 * The set of prefixes (row labels) is divided into two disjoint sets: short and long prefixes.
 * Each long prefix is a one-letter extension of a short prefix; conversely, every time a
 * prefix is added to the set of short prefixes, all possible one-letter extensions are added
 * to the set of long prefixes.
 * 
 * In order to derive a well-defined hypothesis from an observation table, it must satisfy two
 * properties: closedness and consistency.
 * 
 * - An observation table is <b>closed</b> iff for each long prefix <code>u</code> there exists
 * a short prefix <code>u'</code> such that the row contents for both prefixes are equal.
 * - An observation table is <b>consistent</b> iff for every two short prefixes <code>u</code> and
 * <code>u'</code> with identical row contents, it holds that for every input symbol <code>a</code>
 * the rows indexed by <code>ua</code> and <code>u'a</code> also have identical contents.  
 * 
 * @author Malte Isberner <malte.isberner@gmail.com>
 *
 * @param <I> input symbol class
 * @param <O> output class
 */
public class ObservationTable<I,O> {
	
	
	private final Alphabet<? extends I> alphabet;
	
	private final List<Row<I>> shortPrefixRows
		= new ArrayList<Row<I>>();
	private final List<Row<I>> longPrefixRows
		= new ArrayList<Row<I>>();
	
	private final List<Row<I>> allRows
		= new ArrayList<Row<I>>();
	
	private final List<List<O>> allRowContents
		= new ArrayList<List<O>>();
	private final Map<List<O>,Integer> rowContentIds
		= new HashMap<List<O>,Integer>();
	
	private final Map<Word<I>,Row<I>> rowMap
		= new HashMap<Word<I>,Row<I>>();
	
	private int numRows = 0;
	
	private final List<Word<I>> suffixes
		= new ArrayList<Word<I>>();
	
	
	/**
	 * Constructor.
	 * @param alphabet the learning alphabet.
	 */
	public ObservationTable(Alphabet<? extends I> alphabet) {
		this.alphabet = alphabet;
	}
	
	/**
	 * Initializes an observation table using a specified set of suffixes.
	 * 
	 * @param initialSuffixes the set of initial column labels.
	 * @param oracle the {@link MembershipOracle} to use for performing queries
	 * @return a list of equivalence classes of unclosed rows
	 */
	public List<List<Row<I>>> initialize(List<Word<I>> initialSuffixes, MembershipOracle<I,O> oracle) {
		if(allRows.size() > 0)
			throw new IllegalStateException("Called initialize, but there are already rows present");
		
		int numSuffixes = initialSuffixes.size();
		suffixes.addAll(initialSuffixes);
		
		int numLps = alphabet.size();
		int numPrefixes = 1 + numLps;
		
		List<Query<I,O>> queries = new ArrayList<Query<I,O>>(numPrefixes * numSuffixes);
		
		Word<I> eps = Words.epsilon();
		Row<I> epsRow = createSpRow(Words.<I>epsilon());
		
		buildQueries(queries, eps, suffixes);
		
		for(int i = 0; i < alphabet.size(); i++) {
			I sym = alphabet.getSymbol(i);
			Word<I> w = Words.asWord(sym);
			Row<I> lpRow = createLpRow(w);
			buildQueries(queries, w, suffixes);
			epsRow.setSuccessor(i, lpRow);
		}
		
		oracle.processQueries(queries);
		
		Iterator<Query<I,O>> queryIt = queries.iterator();
		
		List<O> firstRowContents = new ArrayList<O>(numSuffixes);
		fetchResults(queryIt, firstRowContents, numSuffixes);
		processContents(epsRow, firstRowContents);
		
		List<List<Row<I>>> unclosed = new ArrayList<List<Row<I>>>();
		
		for(Row<I> lpRow : longPrefixRows) {
			List<O> rowContents = new ArrayList<O>(numSuffixes);
			fetchResults(queryIt, rowContents, numSuffixes);
			if(processContents(lpRow, rowContents))
				unclosed.add(new ArrayList<Row<I>>());
			
			int id = lpRow.getRowContentId();
			
			if(id > 0)
				unclosed.get(id - 1).add(lpRow);
		}
		
		return unclosed;
	}
	
	/**
	 * Adds a suffix to the list of distinguishing suffixes. This is a convenience method
	 * that can be used as shorthand for
	 * <code>addSufixes(Collections.singletonList(suffix), oracle)</code>
	 * @param suffix the suffix to add
	 * @param oracle the membership oracle
	 * @return a list of equivalence classes of unclosed rows
	 */
	public List<List<Row<I>>> addSuffix(Word<I> suffix, MembershipOracle<I,O> oracle) {
		return addSuffixes(Collections.singletonList(suffix), oracle);
	}
	
	/**
	 * Adds suffixes to the list of distinguishing suffixes.
	 * @param newSuffixes the suffixes to add
	 * @param oracle the membership oracle
	 * @return a list of equivalence classes of unclosed rows
	 */
	public List<List<Row<I>>> addSuffixes(List<Word<I>> newSuffixes, MembershipOracle<I, O> oracle) {
		int oldSuffixCount = suffixes.size();
		int numNewSuffixes = newSuffixes.size();
		
		int numSpRows = shortPrefixRows.size();
		int rowCount = numSpRows + longPrefixRows.size();
		
		List<Query<I,O>> queries = new ArrayList<Query<I,O>>(rowCount * numNewSuffixes);
		
		for(Row<I> row : shortPrefixRows)
			buildQueries(queries, row.getPrefix(), newSuffixes);
		
		for(Row<I> row : longPrefixRows)
			buildQueries(queries, row.getPrefix(), newSuffixes);
		
		oracle.processQueries(queries);
		
		Iterator<Query<I,O>> queryIt = queries.iterator();
		
		for(Row<I> row : shortPrefixRows) {
			List<O> rowContents = allRowContents.get(row.getRowContentId());
			if(rowContents.size() == oldSuffixCount) {
				rowContentIds.remove(rowContents);
				fetchResults(queryIt, rowContents, numNewSuffixes);
				rowContentIds.put(rowContents, row.getRowContentId());
			}
			else {
				List<O> newContents = new ArrayList<O>(oldSuffixCount + numNewSuffixes);
				newContents.addAll(rowContents.subList(0, oldSuffixCount));
				fetchResults(queryIt, newContents, numNewSuffixes);
				processContents(row, newContents);
			}
		}
		
		List<List<Row<I>>> unclosed = new ArrayList<List<Row<I>>>();
		numSpRows = numRows;
		
		for(Row<I> row : longPrefixRows) {
			List<O> rowContents = allRowContents.get(row.getRowContentId());
			if(rowContents.size() == oldSuffixCount) {
				rowContentIds.remove(rowContents);
				fetchResults(queryIt, rowContents, numNewSuffixes);
				rowContentIds.put(rowContents, row.getRowContentId());
			}
			else {
				List<O> newContents = new ArrayList<O>(oldSuffixCount + numNewSuffixes);
				newContents.addAll(rowContents.subList(0, oldSuffixCount));
				fetchResults(queryIt, newContents, numNewSuffixes);
				if(processContents(row, newContents))
					unclosed.add(new ArrayList<Row<I>>());
				
				int id = row.getRowContentId();
				if(id >= numSpRows)
					unclosed.get(id - numSpRows).add(row);
			}	
		}
		
		this.suffixes.addAll(newSuffixes);
		
		return unclosed;
	}
	
	
	/**
	 * Moves the specified rows to the set of short prefix rows. If some of the specified
	 * rows already are short prefix rows, they are ignored (unless they do not have any
	 * contents, in which case they are completed).
	 * @param lpRows the rows to move to the set of short prefix rows
	 * @param oracle the membership oracle
	 * @return a list of equivalence classes of unclosed rows
	 */
	public List<List<Row<I>>> toShortPrefixes(List<Row<I>> lpRows, MembershipOracle<I,O> oracle) {
		List<Row<I>> freshSpRows = new ArrayList<Row<I>>();
		List<Row<I>> freshLpRows = new ArrayList<Row<I>>();
		
		for(Row<I> row : lpRows) {
			if(row.isShortPrefix()) {
				if(row.hasContents())
					continue;
				freshSpRows.add(row);
			}
			else {
				makeShort(row);
				if(!row.hasContents())
					freshSpRows.add(row);
			}
			
			Word<I> prefix = row.getPrefix();
						
			for(int i = 0; i < alphabet.size(); i++) {
				I sym = alphabet.getSymbol(i);
				Word<I> lp = Words.append(prefix, sym);
				Row<I> lpRow = rowMap.get(lp);
				if(lpRow == null) {
					lpRow = createLpRow(lp);
					freshLpRows.add(lpRow);
				}
				row.setSuccessor(i, lpRow);
			}
		}
		
		int numSuffixes = suffixes.size();
		
		int numFreshRows = freshSpRows.size() + freshLpRows.size();
		List<Query<I,O>> queries = new ArrayList<Query<I,O>>(numFreshRows * numSuffixes);
		buildRowQueries(queries, freshSpRows, suffixes);
		buildRowQueries(queries, freshLpRows, suffixes);
		
		oracle.processQueries(queries);
		Iterator<Query<I,O>> queryIt = queries.iterator();
		
		for(Row<I> row : freshSpRows) {
			List<O> contents = new ArrayList<O>(numSuffixes);
			fetchResults(queryIt, contents, numSuffixes);
			processContents(row, contents);
		}
		
		int numSpRows = numDistinctRows();
		List<List<Row<I>>> unclosed = new ArrayList<List<Row<I>>>();
		
		for(Row<I> row : freshLpRows) {
			List<O> contents = new ArrayList<O>(numSuffixes);
			fetchResults(queryIt, contents, numSuffixes);
			if(processContents(row, contents))
				unclosed.add(new ArrayList<Row<I>>());
			
			int id = row.getRowContentId();
			if(id >= numSpRows)
				unclosed.get(id - numSpRows).add(row);
		}
		
		return unclosed;
	}
	
	public List<List<Row<I>>> addShortPrefixes(List<Word<I>> shortPrefixes, MembershipOracle<I,O> oracle) {	
		List<Row<I>> toSpRows = new ArrayList<Row<I>>();
		
		for(Word<I> sp : shortPrefixes) {
			Row<I> row = rowMap.get(sp);
			if(row != null) {
				if(row.isShortPrefix())
					continue;
			}
			else
				row = createSpRow(sp);
			toSpRows.add(row);
		}
		
		return toShortPrefixes(toSpRows, oracle);
	}
	
	
	@SuppressWarnings("unchecked")
	public Inconsistency<I,O> findInconsistency() {
		Row<I>[] canonicRows = (Row<I>[])new Row<?>[numDistinctRows()];
		
		for(Row<I> spRow : shortPrefixRows) {
			int contentId = spRow.getRowContentId();
			
			Row<I> canRow = canonicRows[contentId];
			if(canRow == null) {
				canonicRows[contentId] = spRow;
				continue;
			}
			
			for(int i = 0; i < alphabet.size(); i++) {
				int spSuccContent = spRow.getSuccessor(i).getRowContentId();
				int canSuccContent = canRow.getSuccessor(i).getRowContentId();
				if(spSuccContent != canSuccContent)
					return new Inconsistency<I,O>(canRow, spRow, i);
			}
		}
		
		return null;
	}
	
	
	protected boolean makeShort(Row<I> row) {
		if(row.isShortPrefix())
			return false;
		
		// TODO: Use DynamicList for O(1) removal/insertion
		longPrefixRows.remove(row);
		shortPrefixRows.add(row);
		row.makeShort(alphabet.size());
		return true;
	}
	
	protected Row<I> createLpRow(Word<I> prefix) {
		Row<I> newRow = new Row<I>(prefix, numRows++);
		allRows.add(newRow);
		rowMap.put(prefix, newRow);
		longPrefixRows.add(newRow);
		return newRow;
	}
	
	protected Row<I> createSpRow(Word<I> prefix) {
		Row<I> newRow = new Row<I>(prefix, numRows++, alphabet.size());
		allRows.add(newRow);
		rowMap.put(prefix, newRow);
		shortPrefixRows.add(newRow);
		return newRow;
	}
	
	public List<O> rowContents(Row<I> row) {
		return allRowContents.get(row.getRowContentId());
	}
	
	public O cellContents(Row<I> row, int columnId) {
		List<O> contents = rowContents(row);
		return contents.get(columnId);
	}
	
	public Row<I> getRow(int rowId) {
		return allRows.get(rowId);
	}
	
	public int numDistinctRows() {
		return allRowContents.size();
	}
	
	public List<Row<I>> getShortPrefixRows() {
		return shortPrefixRows;
	}

	public int numShortPrefixRows() {
		return shortPrefixRows.size();
	}
	
	public int numLongPrefixRows() {
		return longPrefixRows.size();
	}
	
	public int numTotalRows() {
		return shortPrefixRows.size() + longPrefixRows.size();
	}
	
	public int numSuffixes() {
		return suffixes.size();
	}
	
	public List<Word<I>> getSuffixes() {
		return suffixes;
	}
	
	protected boolean processContents(Row<I> row, List<O> rowContents) {
		Integer contentId = rowContentIds.get(rowContents);
		boolean added = false;
		if(contentId == null) {
			rowContentIds.put(rowContents, contentId = numDistinctRows());
			allRowContents.add(rowContents);
			added = true;
		}
		row.setRowContentId(contentId);
		return added;
	}
	
	protected static <I,O>
	void buildQueries(List<Query<I,O>> queryList, List<Word<I>> prefixes, List<Word<I>> suffixes) {
		for(Word<I> prefix : prefixes)
			buildQueries(queryList, prefix, suffixes);
	}
	
	protected static <I,O>
	void buildRowQueries(List<Query<I,O>> queryList, List<Row<I>> rows, List<Word<I>> suffixes) {
		for(Row<I> row : rows)
			buildQueries(queryList, row.getPrefix(), suffixes);
	}
	
	protected static <I,O>
	void buildQueries(List<Query<I,O>> queryList, Word<I> prefix, List<Word<I>> suffixes) {
		for(Word<I> suffix : suffixes)
			queryList.add(new Query<I,O>(prefix, suffix));
	}
	
	protected static <I,O>
	void fetchResults(Iterator<Query<I,O>> queryIt, List<O> output, int numSuffixes) {
		for(int j = 0; j  < numSuffixes; j++) {
			Query<I,O> qry = queryIt.next();
			output.add(qry.getOutput());
		}
	}

	public boolean isInitialized() {
		return (allRows.size() > 0);
	}
	

}
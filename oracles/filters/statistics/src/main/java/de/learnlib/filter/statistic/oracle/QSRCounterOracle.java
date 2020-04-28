package de.learnlib.filter.statistic.oracle;

import de.learnlib.api.oracle.SymbolQueryOracle;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

public class QSRCounterOracle<I,O> implements SymbolQueryOracle<I,O> {

    private final SymbolQueryOracle<I, O> delegate;
    private final AtomicLong resetCounter = new AtomicLong();
    private final AtomicLong symbolCounter = new AtomicLong();
    private final AtomicLong queryCounter = new AtomicLong();
    private final AtomicLong batchCounter = new AtomicLong();
    private final AtomicLong batchSizeCounter = new AtomicLong();


    public QSRCounterOracle(SymbolQueryOracle<I, O> delegate) {
        this.delegate = delegate;
    }

    @Override
    public O query(I i) {
        symbolCounter.incrementAndGet();
        return delegate.query(i);
    }

    @Override
    public void reset() {
        resetCounter.incrementAndGet();
        delegate.reset();
    }

    @Override
    public void processQueries(Collection<? extends Query<I, Word<O>>> queries) {
        batchCounter.incrementAndGet();
        batchSizeCounter.addAndGet(queries.size());
        final WordBuilder<O> wb = new WordBuilder<>();

        for (final Query<I, Word<O>> q : queries) {
            queryCounter.incrementAndGet();
            reset();

            for (final I i : q.getPrefix()) {
                query(i);
            }

            for (final I i : q.getSuffix()) {
                wb.append(query(i));
            }

            q.answer(wb.toWord());
            wb.clear();
        }
    }

    public long getResetCount() {
        return resetCounter.get();
    }

    public long getSymbolCount() {
        return symbolCounter.get();
    }

    public long getQueryCount() {
        return queryCounter.get();
    }

    public long getBatchCount (){return batchCounter.get();}

    public long getAverageBatchSize (){return getAverageValue();}

    private long getAverageValue() {
        if(batchCounter.get() == 0 || batchSizeCounter.get() == 0) return 0;
        return batchSizeCounter.get()/batchCounter.get();
    }
}

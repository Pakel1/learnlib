package de.learnlib.filter.statistic.oracle;

import de.learnlib.api.oracle.SymbolQueryOracle;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

public class CounterQueryOracle<I,O> implements SymbolQueryOracle<I,O> {

    private final SymbolQueryOracle<I, O> delegate;
    private final AtomicLong resetCounter = new AtomicLong();
    private final AtomicLong symbolCounter = new AtomicLong();
    private final AtomicLong queryCounter = new AtomicLong();


    public CounterQueryOracle(SymbolQueryOracle<I, O> delegate) {
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
        queryCounter.incrementAndGet();
        delegate.processQueries(queries);
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

}

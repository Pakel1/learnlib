package de.learnlib.oracle.parallelism;

import de.learnlib.api.oracle.SymbolQueryOracle;
import de.learnlib.api.query.Query;
import net.automatalib.words.Word;

import java.util.Collection;

public class SuperOracle<I,O> implements SymbolQueryOracle<I,O> {

    private final SymbolQueryOracle<I,O> oracle;
    private final ParallelOracle<I,Word<O>> parallelOracle;

    public SuperOracle(SymbolQueryOracle<I,O> oracle, ParallelOracle<I,Word<O>> parallelOracle) {
        this.oracle = oracle;
        this.parallelOracle = parallelOracle;
    }

    public SymbolQueryOracle getOracle(){return oracle;}

    @Override
    public O query(I i) {
       return oracle.query(i);
    }

    @Override
    public void reset() {
        oracle.reset();
    }

    @Override
    public void processQueries(Collection<? extends Query<I, Word<O>>> queries) {
        parallelOracle.processQueries(queries);
    }
}

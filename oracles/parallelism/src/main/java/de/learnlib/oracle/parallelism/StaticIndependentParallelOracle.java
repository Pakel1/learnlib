package de.learnlib.oracle.parallelism;

import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.oracle.SymbolQueryOracle;
import org.checkerframework.checker.index.qual.NonNegative;

import java.util.Collection;

public class StaticIndependentParallelOracle<I,O> extends StaticParallelOracle<I,O> {
    private final SymbolQueryOracle<I,O> oracle;
    public StaticIndependentParallelOracle(Collection<? extends MembershipOracle<I, O>> membershipOracles,
                                           @NonNegative int minBatchSize, PoolPolicy policy, SymbolQueryOracle<I,O> oracle) {
        super(membershipOracles, minBatchSize, policy);
        this.oracle = oracle;
    }
    public SymbolQueryOracle getOracle(){return oracle;}
}

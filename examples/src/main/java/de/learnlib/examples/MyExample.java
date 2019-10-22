package de.learnlib.examples;

import de.learnlib.acex.AbstractCounterexample;
import de.learnlib.acex.analyzers.AbstractNamedAcexAnalyzer;
import de.learnlib.acex.analyzers.AcexAnalysisAlgorithms;
import de.learnlib.algorithms.adt.config.ADTExtenders;
import de.learnlib.algorithms.adt.config.LeafSplitters;
import de.learnlib.algorithms.adt.config.SubtreeReplacers;
import de.learnlib.algorithms.adt.learner.ADTLearner;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealy;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.driver.util.MealySimulatorSUL;
import de.learnlib.filter.cache.mealy.SymbolQueryCache;
import de.learnlib.filter.statistic.oracle.CounterQueryOracle;
import de.learnlib.oracle.equivalence.MealySimulatorEQOracle;
import de.learnlib.oracle.membership.SULSymbolQueryOracle;
import de.learnlib.oracle.parallelism.ParallelOracle;
import de.learnlib.oracle.parallelism.StaticIndependentParallelOracle;
import de.learnlib.oracle.parallelism.StaticParallelOracle;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;

import java.util.ArrayList;
import java.util.Collection;

public class MyExample {
    private MyExample(){}

    public static void main(String[] args){

        //create alphabet for mealy machine
        Alphabet<Character> alphabet = Alphabets.characters('a', 'b');

        // create mealy machine
        CompactMealy<Character,Integer> target = new CompactMealy<Character, Integer>(alphabet);
        target.addInitialState();
        target.addState();
        target.addState();

        target.setInitialState(0);

        target.addTransition(0, 'a', 1, 0);
        target.addTransition(0, 'b', 2, 1);

        target.addTransition(1, 'a', 2, 0);
        target.addTransition(1, 'b', 1, 1);

        target.addTransition(2, 'a', 2, 1);
        target.addTransition(2, 'b', 1, 0);

        //create oracles & cache
        MealySimulatorEQOracle<Character, Integer> orc = new MealySimulatorEQOracle<Character, Integer>(target);
        SULSymbolQueryOracle<Character, Integer> oracle1 = new SULSymbolQueryOracle<>(new MealySimulatorSUL<>(target));
        SULSymbolQueryOracle<Character, Integer> oracle2 = new SULSymbolQueryOracle<>(new MealySimulatorSUL<>(target));
        SymbolQueryCache<Character, Integer> cache = new SymbolQueryCache<>(oracle1, alphabet);
        CounterQueryOracle<Character, Integer> pipeline = new CounterQueryOracle<>(cache);

        ArrayList arg = new ArrayList<>();
        arg.add(pipeline);
        arg.add(oracle2);
        StaticParallelOracle<Character, Integer> test = new StaticParallelOracle<Character, Integer>((Collection<? extends MembershipOracle<Character, Integer>>) arg,10, ParallelOracle.PoolPolicy.CACHED);
        StaticIndependentParallelOracle<Character, Integer> parallelOracle = new StaticIndependentParallelOracle(
                (Collection<? extends MembershipOracle<Character, Integer>>) arg, 10,ParallelOracle.PoolPolicy.CACHED, pipeline);

        //ADT Learner
        ADTLearner<Character, Integer> learner = new ADTLearner<>(alphabet, oracle1, parallelOracle, LeafSplitters.DEFAULT_SPLITTER,
                ADTExtenders.EXTEND_BEST_EFFORT, SubtreeReplacers.LEVELED_BEST_EFFORT);

        //TTT Learner
        AbstractNamedAcexAnalyzer analyzer = new AbstractNamedAcexAnalyzer("Linear") {
            @Override
            public int analyzeAbstractCounterexample(AbstractCounterexample<?> acex, int low, int high) {
                return AcexAnalysisAlgorithms.linearSearchFwd(acex, low, high);
            }
        };

        @SuppressWarnings("Unused")
        TTTLearnerMealy<Character, Integer> learner2 = new TTTLearnerMealy<Character, Integer>(alphabet, pipeline ,analyzer);

        //statement below changes learner to TTT (ADT Learner above needs to get -> // )
        //  TTTLearnerMealy<Character, Integer> learner = learner2;

        //learning setup
        DefaultQuery<Character, Word<Integer>> counterexample = null;
        do {
            if (counterexample == null) {
                learner.startLearning();
            } else {
                boolean refined = learner.refineHypothesis(counterexample);
                if (!refined) {
                    System.err.println("No refinement effected by counterexample!");
                }
            }

            counterexample = orc.findCounterExample(learner.getHypothesisModel(), alphabet);

        } while (counterexample != null);


        System.out.println("------------------------------------------");
        MealyMachine<?, Character, ?, Integer> model1 = learner.getHypothesisModel();
        Collection<Character> test1 = new ArrayList<>();
        test1.add('a');
        System.out.print("INPUT:a - Successors: ");
        System.out.println(model1.getState(test1));
        test1.add('a');
        System.out.print("INPUT:aa - Successors: ");
        System.out.println(model1.getState(test1));
        test1.add('a');
        System.out.print("INPUT:aaa - Successors: ");
        System.out.println(model1.getState(test1));
        Collection<Character> test2 = new ArrayList<>();
        test2.add('b');
        System.out.print("INPUT:b - Successors: ");
        System.out.println(model1.getState(test2));
        test2.add('b');
        System.out.print("INPUT:bb - Successors: ");
        System.out.println(model1.getState(test2));
        test2.add('b');
        System.out.print("INPUT:bbb - Successors: ");
        System.out.println(model1.getState(test2));
        System.out.println("------------------------------------------");

        System.out.print("Symbol Count: ");
        System.out.println(pipeline.getSymbolCount());
        System.out.print("Reset Count: ");
        System.out.println(pipeline.getResetCount());
        System.out.print("Query Count: ");
        System.out.println(pipeline.getQueryCount());


    }
}

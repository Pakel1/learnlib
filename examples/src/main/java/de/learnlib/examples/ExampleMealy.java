package de.learnlib.examples;

import de.learnlib.acex.AbstractCounterexample;
import de.learnlib.acex.analyzers.AbstractNamedAcexAnalyzer;
import de.learnlib.acex.analyzers.AcexAnalysisAlgorithms;
import de.learnlib.algorithms.adt.automaton.ADTState;
import de.learnlib.algorithms.adt.learner.ADTLearner;
import de.learnlib.algorithms.adt.learner.ADTLearnerBuilder;
import de.learnlib.algorithms.dhc.mealy.MealyDHC;
import de.learnlib.algorithms.lstar.mealy.ClassicLStarMealy;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealy;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.driver.util.MealySimulatorSUL;
import de.learnlib.filter.cache.mealy.MealyCaches;
import de.learnlib.filter.cache.mealy.SymbolQueryCache;
import de.learnlib.oracle.equivalence.MealySimulatorEQOracle;
import de.learnlib.oracle.equivalence.SimulatorEQOracle;
import de.learnlib.oracle.equivalence.mealy.SymbolEQOracleWrapper;
import de.learnlib.oracle.membership.SULSymbolQueryOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import de.learnlib.oracle.membership.SimulatorSULSymbolQueryOracle;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.Unused;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ExampleMealy {
    private ExampleMealy(){}

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

        //create oracles for finding counterexamples
       MealySimulatorEQOracle<Character, Integer> orc = new MealySimulatorEQOracle<Character, Integer>(target);

       //oracles for learners
        SULSymbolQueryOracle<Character, Integer> oracle = new SULSymbolQueryOracle<>(new MealySimulatorSUL<>(target));
        MembershipOracle<Character, Word<Integer>> oracle3 = new SimulatorOracle<>(target);

        //ADT Learner
       ADTLearnerBuilder<Character, Integer> builder = new ADTLearnerBuilder<>();
       ADTLearner<Character, Integer> learner = builder.withAlphabet(alphabet).withOracle(oracle).create();

       //TTT Learner
        AbstractNamedAcexAnalyzer analyzer = new AbstractNamedAcexAnalyzer("Linear") {
            @Override
            public int analyzeAbstractCounterexample(AbstractCounterexample<?> acex, int low, int high) {
                return AcexAnalysisAlgorithms.linearSearchFwd(acex, low, high);
            }
        };

        @SuppressWarnings("Unused")
        TTTLearnerMealy<Character, Integer> learner2 = new TTTLearnerMealy<Character, Integer>(alphabet,oracle,analyzer);

        //statement below changes learner to TTT (ADT Learner above needs to get -> // )
       // TTTLearnerMealy<Character, Integer> learner = learner2;

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
    }
}

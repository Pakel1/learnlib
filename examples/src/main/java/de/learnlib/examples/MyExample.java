package de.learnlib.examples;

import de.learnlib.acex.analyzers.AcexAnalyzers;
import de.learnlib.algorithms.adt.learner.ADTLearner;
import de.learnlib.algorithms.adt.learner.ADTLearnerBuilder;
import de.learnlib.algorithms.ttt.mealy.TTTLearnerMealy;
import de.learnlib.api.algorithm.LearningAlgorithm;
import de.learnlib.api.query.DefaultQuery;
import de.learnlib.driver.util.MealySimulatorSUL;
import de.learnlib.filter.cache.mealy.SymbolQueryCache;
import de.learnlib.filter.statistic.oracle.CounterQueryOracle;
import de.learnlib.oracle.equivalence.MealySimulatorEQOracle;
import de.learnlib.oracle.membership.SULSymbolQueryOracle;
import de.learnlib.oracle.membership.SimulatorOracle;
import de.learnlib.oracle.parallelism.StaticParallelOracle;
import de.learnlib.oracle.parallelism.SuperOracle;
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
        SymbolQueryCache<Character, Integer> cache = new SymbolQueryCache<>(oracle1, alphabet);
        CounterQueryOracle<Character, Integer> pipeline = new CounterQueryOracle<>(cache);

        //memberships oracles for StaticParallelOracle
        SimulatorOracle<Character, Word<Integer>> para1 = new SimulatorOracle<>(target);
        SimulatorOracle<Character, Word<Integer>> para2 = new SimulatorOracle<>(target);
        SimulatorOracle<Character, Word<Integer>> para3 = new SimulatorOracle<>(target);

        ArrayList arg = new ArrayList<>();
        arg.add(para1);
        arg.add(para2);
        arg.add(para3);

        StaticParallelOracle<Character,Word<Integer>> parallelOracle = new StaticParallelOracle<>(arg,3
                ,StaticParallelOracle.PoolPolicy.FIXED);

        SuperOracle<Character, Integer> superOracle = new SuperOracle<>(pipeline, parallelOracle);

        ADTLearnerBuilder<Character, Integer> builder = new ADTLearnerBuilder<>();
        ADTLearner<Character, Integer> adtLearner = builder.withOracle(superOracle).withAlphabet(alphabet).create();
        TTTLearnerMealy<Character, Integer> tttLearner = new TTTLearnerMealy<>(alphabet, superOracle,
                AcexAnalyzers.LINEAR_BWD);

        learn(adtLearner, alphabet, orc);
        printModel(adtLearner);
        printStatistic(pipeline);

        learn(tttLearner, alphabet, orc);
        printModel(tttLearner);
        printStatistic(pipeline);
    }

    public static void learn(LearningAlgorithm.MealyLearner<Character,Integer> learner, Alphabet<Character> alphabet,
                             MealySimulatorEQOracle<Character, Integer> orc){
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
    }

    public static void printModel(LearningAlgorithm.MealyLearner<Character,Integer> learner){
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

    public static void printStatistic(CounterQueryOracle<Character, Integer> pipeline) {
        System.out.print("Symbol Count: ");
        System.out.println(pipeline.getSymbolCount());
        System.out.print("Reset Count: ");
        System.out.println(pipeline.getResetCount());
        System.out.print("Query Count: ");
        System.out.println(pipeline.getQueryCount());
    }
}

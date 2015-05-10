/* Copyright (C) 2013-2015 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 * 
 * LearnLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * LearnLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with LearnLib; if not, see
 * <http://www.gnu.de/documents/lgpl.en.html>.
 */
package de.learnlib.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import de.learnlib.oracles.DefaultQuery;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.transout.MealyMachine;
import net.automatalib.words.Word;

/**
 * A query is the basic form of interaction between a {@link LearningAlgorithm learner} and a
 * {@link MembershipOracle (membership) oracle}, or teacher.
 * <p>
 * In LearnLib, queries are performed in a callback-like fashion: an oracle does not <i>return</i>
 * the responses to the queries, but rather invokes the {@link #answer(Object)} method on the query
 * objects it was provided with. This allows for implementing queries which directly react to an answered
 * query (e.g., by modifying some internal data structure), without the need for buffering answers.
 * It also allows for a more efficient parallel processing of queries, as there is no need for maintaining
 * a common (synchronized) result data structure such as a map. However, this means that a learner
 * cannot rely on the {@link #answer(Object)} method of a query being called from the same thread which
 * invoked {@link MembershipOracle#processQueries(java.util.Collection)}. If this causes concurrency
 * issues, a safe choice is to use queries of class {@link DefaultQuery}, which simply store the response
 * and make it accessible via {@link DefaultQuery#getOutput()} for processing after the
 * {@link MembershipOracle#processQueries(java.util.Collection)} call returns, guaranteeing thread-safety.
 * <p>
 * Conceptually, a query is divided into a {@link #getPrefix() prefix} and a {@link #getSuffix()}
 * suffix. The prefix part of a query identifies a state in the (unknown) target system, whereas
 * the suffix is the "experiment" which is conducted on the system starting from the state to which
 * it was transferred by the prefix. While the prefix influences the response of the target system
 * to a query, the answer is the <i>directly observable</i> reaction to executing the suffix.
 * <p>
 * <b>Example 1:</b> when learning {@link MealyMachine Mealy machines}, the prefix transfers the target
 * system to a certain state. The outputs produced by the system while executing the prefix are
 * <i>not</i> part of the answer, as the role of the prefix is limited to reaching a certain state.
 * The reaction of the target system consists of the output word produced while executing the suffix.
 * Therefore, in the setting of Mealy machine learning, a valid oracle will call the {@link #answer(Object)}
 * method with a word of the same length as the suffix.
 * <p>
 * <b>Example 2:</b> when learning {@link DFA}s, the reaction of the target system is fully determined
 * by the state reached by an input word. Since both prefix and suffix have the same effect on producing
 * this output (by transferring the system to a certain state), the response will always be a single
 * {@link Boolean}, and, furthermore, for every input word {@code w}, the response to a query will
 * always be the same regardless of the subdivision of {@code w = uv} into prefix {@code u} and
 * suffix {@code v} (including the corner cases <code>u = &epsilon;</code> and
 * <code>v = &epsilon;</code>).
 * 
 * @author Malte Isberner
 *
 * @param <I> input symbol type
 * @param <D> output domain type
 */
public abstract class Query<I, D> {
	
	private int hashCode = 0;
	
	/**
	 * Returns the prefix part of this query. The prefix of a query is responsible
	 * for transferring the system into a certain state, but (apart from that) does
	 * not directly influence the output.
	 * @return the prefix of this query
	 */
	@Nonnull
	public abstract Word<I> getPrefix();
	
	/**
	 * Returns the suffix part of this query. The suffix of a query is the experiment
	 * performed on the system when in the state it was transferred into by the prefix,
	 * and thus directly influences the output.
	 * @return the suffix of this query
	 */
	@Nonnull
	public abstract Word<I> getSuffix();
	
	/**
	 * Answers the query. This method should be called by a {@link MembershipOracle}, and only
	 * once per query to process. Calling this method more than once may result in undefined
	 * behavior, possibly (but not necessarily) throwing an exception.
	 * 
	 * @param output the output, i.e., the response to the query
	 */
	public abstract void answer(@Nullable D output);
	
	/**
	 * Retrieves the input word of this query. The input word corresponding to a query
	 * is the concatenation of its prefix and suffix.
	 * @return the input word of this query
	 */
	@Nonnull
	public Word<I> getInput() {
		return getPrefix().concat(getSuffix());
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object o) {
		if(o == null)
			return false;
		if(this == o)
			return true;
		if(!(o instanceof Query))
			return false;
		Query<?,?> other = (Query<?,?>)o;
		
		Word<I> thisPref = getPrefix();
		Word<I> thisSuff = getSuffix();
		
		Word<?> otherPref = other.getPrefix();
		Word<?> otherSuff = other.getSuffix();
		
		if(thisPref != otherPref && !thisPref.equals(otherPref))
			return false;
		if(thisSuff != otherSuff && !thisSuff.equals(otherSuff))
			return false;
		
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		if(hashCode != 0)
			return hashCode;
		
		Word<I> prefix = getPrefix(), suffix = getSuffix();
		hashCode = 5;
        hashCode = 89 * hashCode + prefix.hashCode();
        hashCode = 89 * hashCode + suffix.hashCode();
        return hashCode;
	}

	/**
	 * Returns the string representation of this query.
	 *
	 * @return A string of the form {@code "Query[<prefix>|<suffix>]"} for queries not containing
	 * an answer or {@code "Query[<prefix>|<suffix> / <answer>]"} if an answer may be specified.
	 */
	@Override
	public String toString() {
		return "Query[" + getPrefix() + '|' + getSuffix() + ']';
	}

}

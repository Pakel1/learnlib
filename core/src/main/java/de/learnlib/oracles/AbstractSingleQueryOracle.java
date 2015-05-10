/* Copyright (C) 2013-2014 TU Dortmund
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
 * http://www.gnu.de/documents/lgpl.en.html.
 */
package de.learnlib.oracles;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import net.automatalib.words.Word;
import de.learnlib.api.MembershipOracle;
import de.learnlib.api.QueryAnswerer;
import de.learnlib.api.SingleQueryOracle;

/**
 * Base class for oracles whose semantic is defined in terms of directly answering single queries
 * (like a {@link QueryAnswerer}, and that cannot profit from batch processing of queries.
 * <p>
 * Subclassing this class instead of directly implementing {@link MembershipOracle} means that
 * the {@link #answerQuery(Word, Word)} instead of the {@link #processQueries(Collection)} method
 * needs to be implemented.
 * 
 * @deprecated since 2015-05-10. This class is no longer necessary due to the introduction
 * of default methods. Instead, implement {@link SingleQueryOracle} (or the respective specialization)
 * directly.
 * 
 * @author Malte Isberner
 *
 * @param <I> input symbol type
 * @param <D> output domain type
 */
@Deprecated
@ParametersAreNonnullByDefault
public abstract class AbstractSingleQueryOracle<I, D> implements SingleQueryOracle<I, D> {
	
	@Deprecated
	public static abstract class AbstractSingleQueryOracleDFA<I>
			extends AbstractSingleQueryOracle<I,Boolean> implements SingleQueryOracleDFA<I> {}
	
	@Deprecated
	public static abstract class AbstractSingleQueryOracleMealy<I,O>
			extends AbstractSingleQueryOracle<I,Word<O>> implements SingleQueryOracleMealy<I,O> {}
}

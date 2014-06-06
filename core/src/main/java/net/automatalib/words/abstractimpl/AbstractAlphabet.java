/* Copyright (C) 2013 TU Dortmund
 * This file is part of AutomataLib, http://www.automatalib.net/.
 * 
 * AutomataLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * AutomataLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with AutomataLib; if not, see
 * http://www.gnu.de/documents/lgpl.en.html.
 */
package net.automatalib.words.abstractimpl;

import java.util.AbstractList;

import net.automatalib.words.Alphabet;

public abstract class AbstractAlphabet<I> extends AbstractList<I> implements Alphabet<I> {


	@Override
	public int compare(I o1, I o2) {
		return getSymbolIndex(o1) - getSymbolIndex(o2);
	}

	@Override
	public I get(int index) {
		return getSymbol(index);
	}
	
	
	@Override
	public void writeToArray(int offset, Object[] array, int tgtOfs, int num) {
		for(int i = offset, j = tgtOfs, k = 0; k < num; i++, j++, k++) {
			array[j] = getSymbol(i);
		}
	}
}
/* Copyright (C) 2013 TU Dortmund
 * This file is part of AutomataLib, http://www.automatalib.net/.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.automatalib.automata.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.automatalib.automata.concepts.StateIDs;
import net.automatalib.automata.simple.SimpleAutomaton;

public class SimpleStateIDs<S> implements StateIDs<S> {
	
	private final Map<S,Integer> stateIds;
	private final List<S> states;
	
	public SimpleStateIDs(SimpleAutomaton<S,?> automaton) {
		this.states = new ArrayList<>(automaton.getStates());
		int numStates = this.states.size();
		this.stateIds = new HashMap<>(numStates);
		
		for(int i = 0; i < numStates; i++) {
			S state = this.states.get(i);
			stateIds.put(state, i);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.automatalib.automata.concepts.StateIDs#getStateId(java.lang.Object)
	 */
	@Override
	public int getStateId(S state) {
		return stateIds.get(state).intValue();
	}

	/*
	 * (non-Javadoc)
	 * @see net.automatalib.automata.concepts.StateIDs#getState(int)
	 */
	@Override
	public S getState(int id) {
		return states.get(id);
	}

}
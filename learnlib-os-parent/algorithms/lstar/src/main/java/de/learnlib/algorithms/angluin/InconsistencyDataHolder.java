package de.learnlib.algorithms.angluin;

import net.automatalib.words.Word;

public class InconsistencyDataHolder<S> {

	private final Word<S> firstState;
	private final Word<S> secondState;
	private final S differingSymbol;

	public InconsistencyDataHolder(Word<S> firstState, Word<S> secondState, S differingSymbol) {
		this.firstState = firstState;
		this.secondState = secondState;
		this.differingSymbol = differingSymbol;
	}

	public Word<S> getFirstState() {
		return firstState;
	}

	public Word<S> getSecondState() {
		return secondState;
	}

	public S getDifferingSymbol() {
		return differingSymbol;
	}
}
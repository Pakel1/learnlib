/* Copyright (C) 2013-2018 TU Dortmund
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
package net.automatalib.commons.smartcollections;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import net.automatalib.commons.util.array.ResizingObjectArray;

/**
 * This class implements a collection for storing objects in no particular order.
 * <p>
 * It supports (amortized) constant time insertion and removal. Removal does not invalidate the references of other
 * objects, and can be performed during iteration (using the respective {@link Iterator#remove()} method).
 *
 * @param <E>
 *         element class.
 *
 * @author Malte Isberner
 */
public class UnorderedCollection<E> extends AbstractSmartCollection<E> implements CapacityManagement {

    private static final int DEFAULT_INITIAL_CAPACITY = 10;
    // The collection's storage
    private final ResizingObjectArray storage;
    private int size;

    /**
     * Default constructor. Reserves capacity for 10 elements.
     */
    public UnorderedCollection() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Constructor. Reserves the specified initial capacity.
     *
     * @param initialCapacity
     *         the number of elements to reserve capacity for.
     */
    public UnorderedCollection(int initialCapacity) {
        final int capacity = initialCapacity <= 0 ? DEFAULT_INITIAL_CAPACITY : initialCapacity;
        this.storage = new ResizingObjectArray(capacity);
    }

    /**
     * Constructor. Initializes the collection with the contents of the specified collection.
     *
     * @param coll
     *         the collection.
     */
    public UnorderedCollection(Collection<? extends E> coll) {
        this(coll.size());
        addAll(coll);
    }

    @Override
    public boolean ensureCapacity(int minCapacity) {
        return storage.ensureCapacity(minCapacity);
    }

    @Override
    public boolean ensureAdditionalCapacity(int additionalSpace) {
        return ensureCapacity(size + additionalSpace);
    }

    @Override
    public void hintNextCapacity(int nextCapacityHint) {
        storage.hintNextCapacity(nextCapacityHint);
    }

    @Override
    public E get(ElementReference ref) {
        Reference<E> r = asIndexedRef(ref);
        return r.element;
    }

    /**
     * Convenience method, renders a plain cast obsolete and throws a more specific exception if the cast fails.
     */
    @SuppressWarnings("unchecked")
    private static <E> Reference<E> asIndexedRef(ElementReference ref) {
        if (ref.getClass() != Reference.class) {
            throw new InvalidReferenceException(
                    "Reference is of wrong class '" + ref.getClass().getName() + "', should be " +
                    Reference.class.getName() + ".");
        }
        return (Reference<E>) ref;
    }

    @Override
    public ElementReference referencedAdd(E elem) {
        ensureCapacity(size + 1);
        int insertPos = size++;
        Reference<E> ref = new Reference<>(elem, insertPos);
        storage.array[insertPos] = ref;
        return ref;
    }

    @Override
    public void remove(ElementReference ref) {
        remove(extractValidIndex(ref));
    }

    /**
     * Removes an element by its index.
     */
    @SuppressWarnings("unchecked")
    private void remove(int index) {
        int lastIndex = --size;
        Reference<E> removed = (Reference<E>) storage.array[index];
        Reference<E> lastElem = (Reference<E>) storage.array[lastIndex];
        storage.array[index] = lastElem;
        lastElem.index = index;
        removed.index = -1;
        storage.array[lastIndex] = null;
    }

    @Override
    public boolean remove(Object elem) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(storage.array[i], elem)) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<ElementReference> referenceIterator() {
        return new ReferenceIterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void replace(ElementReference ref, E newElement) {
        int idx = extractValidIndex(ref);
        ((Reference<E>) storage.array[idx]).element = newElement;
    }

    /**
     * Convenience method for extracting the index stored in an ElementReference, and throws a more specific exception
     * if the cast fails or the index is not valid.
     */
    private int extractValidIndex(ElementReference ref) {
        Reference<E> iRef = asIndexedRef(ref);
        int idx = iRef.index;
        if (idx < 0 || idx >= size) {
            throw new InvalidReferenceException(
                    "Index " + idx + " is not valid for collection with size " + size + ".");
        }
        return idx;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E choose() {
        if (size == 0) {
            return null;
        }
        return ((Reference<E>) storage.array[0]).element;
    }

    @Override
    public ElementReference chooseRef() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return (ElementReference) storage.array[0];
    }

    @Override
    public Iterable<ElementReference> references() {
        return this::referenceIterator;
    }

    @Override
    public <T extends E> void addAll(T[] array) {
        int sizeInc = array.length;
        ensureCapacity(size + sizeInc);
        for (int index = size, i = 0; i < array.length; index++, i++) {
            storage.array[index] = new Reference<E>(array[i], index);
        }
        size += sizeInc;
    }

    @Override
    public boolean addAll(Collection<? extends E> coll) {
        int sizeInc = coll.size();
        ensureCapacity(size + sizeInc);
        for (E elem : coll) {
            storage.array[size] = new Reference<>(elem, size);
        }
        size += sizeInc;

        return true;
    }

    @Override
    public void quickClear() {
        size = 0;
    }

    @Override
    public void deepClear() {
        storage.setAll(null);
    }

    @Override
    public Iterator<E> iterator() {
        return new ElementIterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return (size == 0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void clear() {
        for (int i = 0; i < size; i++) {
            ((Reference<E>) storage.array[i]).index = -1;
            storage.array[i] = null;
        }
        size = 0;
    }

    /**
     * Swaps the contents of this {@link UnorderedCollection} with another one storing the same elements. This operation
     * runs in constant time, by only swapping storage references.
     *
     * @param other
     *         the {@link UnorderedCollection} to swap contents with.
     */
    public void swap(UnorderedCollection<E> other) {
        int sizeTmp = size;
        size = other.size;
        other.size = sizeTmp;
        storage.swap(other.storage);
    }

    /**
     * The reference for this collection, effectively containing an index in addition to the element itself.
     */
    private static class Reference<E> implements ElementReference {

        public E element;
        public int index;

        /**
         * Constructor.
         *
         * @param element
         *         the stored element.
         * @param index
         *         its index.
         */
        Reference(E element, int index) {
            this.element = element;
            this.index = index;
        }
    }

    /**
     * The iterator for iterating over the element references.
     */
    private class ReferenceIterator implements Iterator<ElementReference> {

        private int index;

        @Override
        public boolean hasNext() {
            return (index < size);
        }

        @Override
        public ElementReference next() {
            if (index >= size) {
                throw new NoSuchElementException();
            }
            return (ElementReference) storage.array[index++];
        }

        @Override
        public void remove() {
            UnorderedCollection.this.remove(--index);
        }
    }

    /**
     * The iterator for iterating over the elements themselves.
     */
    private class ElementIterator implements Iterator<E> {

        private int index;

        @Override
        public boolean hasNext() {
            return (index < size);
        }

        @Override
        @SuppressWarnings("unchecked")
        public E next() {
            if (index >= size) {
                throw new NoSuchElementException();
            }
            return ((Reference<E>) storage.array[index++]).element;
        }

        @Override
        public void remove() {
            UnorderedCollection.this.remove(--index);
        }
    }
}

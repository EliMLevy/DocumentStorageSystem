package edu.yu.cs.com1320.project.impl;

import java.lang.reflect.Array;
import java.util.NoSuchElementException;

import edu.yu.cs.com1320.project.MinHeap;

public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E> {


    private E holder;

    public MinHeapImpl() {
    }

    @Override
    public void insert(E x) {
        if (this.elements == null) {
            this.holder = x;
            this.elements = (E[]) Array.newInstance(x.getClass(), 10);
        }
        // double size of array if necessary
        if (this.count >= this.elements.length - 1) {
            this.doubleArraySize();
        }
        // add x to the bottom of the heap
        this.elements[++this.count] = x;
        // percolate it up to maintain heap order property
        this.upHeap(this.count);
    }

    private boolean isLeaf(int pos) {
        if (pos >= (this.count / 2) && pos <= this.count) {
            return true;
        }
        return false;
    }

    private int leftChild(int pos) {
        return (2 * pos);
    }

    private int rightChild(int pos) {
        return (2 * pos) + 1;
    }

    @Override
    public void reHeapify(Comparable element) {
        int index = this.getArrayIndex(element);
        if(index == -1) {
            this.insert((E)element);
            index = this.getArrayIndex(element);
        }

        // Make sure its larger than parents
        if(index > 1) { //We only worry about parents if it has parents
            if (this.isGreater(index / 2, index)) {
                this.swap(index / 2, index);
                this.reHeapify(element);
            }
        }

        // Make sure its smaller than both children
        if(this.rightChild(index) <= this.count && this.elements[this.rightChild(index)] != null) {
            if (this.isGreater(index, this.leftChild(index)) || this.isGreater(index, this.rightChild(index))) {
                // swap with the greater child
                if (this.isGreater(this.leftChild(index), this.rightChild(index))) {
                    this.swap(index, this.rightChild(index));
                    this.reHeapify(this.elements[this.leftChild(index)]);
                } else {
                    this.swap(index, this.leftChild(index));
                    this.reHeapify(this.elements[this.rightChild(index)]);
                }
            }
        } else if (this.leftChild(index) <= this.count && this.elements[this.leftChild(index)] != null){
            if (this.isGreater(index, this.leftChild(index)) ) {
                this.swap(index, this.leftChild(index));
                this.reHeapify(this.elements[this.leftChild(index)]);
            }
        }

    }

    @Override
    protected int getArrayIndex(Comparable element) {
        for (int i = 1; i <= this.count; i++) {
            if (this.elements[i].compareTo((E) element) == 0) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void doubleArraySize() {

        E[] newArr = (E[]) Array.newInstance(holder.getClass(), this.count * 2);

        for (int i = 0; i < this.elements.length; i++) {
            newArr[i] = this.elements[i];
        }

        this.elements = newArr;

    }

}

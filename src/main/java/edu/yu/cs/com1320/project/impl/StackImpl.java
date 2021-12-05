package edu.yu.cs.com1320.project.impl;



import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {

    // List<T> theStack = new LinkedList<>();

    private StackEntry<T> head = null;
    private int size = 0;

    public StackImpl() {

    }

    @Override
    public void push(T element) {
        head = new StackEntry<T>(element, head);
        this.size ++;
    }

    @Override
    public T pop() {
        if(this.head != null) {
            T result = this.head.element;
            this.head = head.next;
            this.size--;
            return result;
        } else {
            return null;
        }
    }

    @Override
    public T peek() {
        if(this.head != null) {
            return this.head.element;
        } else {
            return null;
        }
    }

    @Override
    public int size() {
        return this.size;
    }


    private class StackEntry<T> {
        private T element;
        private StackEntry<T> next;

        StackEntry(T element, StackEntry<T> next) {
            this.element = element;
            this.next = next;
        }


    }
    
}

package edu.yu.cs.com1320.project.impl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import edu.yu.cs.com1320.project.stage5.impl.DocumentPersistenceManager;

public class BTreeImpl<Key extends Comparable<Key>, Value> implements BTree<Key, Value> {

    private static final int MAX = 4;
    private Node root; // root of the B-tree
    private Node leftMostExternalNode;
    private int height; // height of the B-tree
    private int n; // number of key-value pairs in the B-tree

    private DocumentPersistenceManager pm;

    public BTreeImpl() {
        this.root = new Node(0);
        this.leftMostExternalNode = this.root;
        File pmDir = new File(System.getProperty("user.dir"));
        pm = new DocumentPersistenceManager(pmDir);
    }

    public static class Entry {
        private Comparable key;
        private Object val;
        private Node child;

        public Entry(Comparable key, Object val, Node child) {
            this.key = key;
            this.val = val;
            this.child = child;
        }

        public Object getValue() {
            return this.val;
        }

        public Comparable getKey() {
            return this.key;
        }
    }

    private static final class Node {
        private int entryCount;
        // number of entries
        private Entry[] entries = new Entry[BTreeImpl.MAX];// child links

        private Node next;
        private Node previous;

        // create a node with k entries
        private Node(int k) {
            this.entryCount = k;
        }

        private void setNext(Node next) {
            this.next = next;
        }

        private Node getNext() {
            return this.next;
        }

        private void setPrevious(Node previous) {
            this.previous = previous;
        }

        private Node getPrevious() {
            return this.previous;
        }

        private Entry[] getEntries() {
            return Arrays.copyOf(this.entries, this.entryCount);
        }
    }

    public Value get(Key k) {
        if (k == null) {
            throw new IllegalArgumentException("argument to get() is null");
        }

        Entry result = this.get(this.root, k, this.height);

        if (result == null) {
            return null;
        } else {
            Object val = result.getValue();
            if (val instanceof URI) {
                try {
                    Value unserialized = (Value) pm.deserialize((URI) val);
                    result.val = unserialized;
                    return unserialized;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return (Value) result.getValue();
            }

        }

    };

    private Entry get(Node n, Key k, int h) {
        Entry[] entries = n.getEntries();
        // If we reached an external node, search for a matvhing key
        if (h == 0) {
            for (int i = 0; i < n.entryCount; i++) {
                if (entries[i].key.equals(k)) {
                    return entries[i];
                }
            }
            return null; // 404 Key not found
        } else {
            for (int i = 0; i < n.entryCount; i++) {
                if (i + 1 == n.entryCount || k.compareTo((Key) entries[i + 1].getKey()) < 0) {
                    return this.get(entries[i].child, k, h - 1);
                }
            }
        }

        return null;

    }

    public Value put(Key k, Value v) {
        if (k == null) {
            throw new IllegalArgumentException("key is null");
        }

        Entry testGet = this.get(this.root, k, this.height);
        if (testGet != null) {
            Object temp = testGet.getValue();
            Value result = null;
            if (temp instanceof URI) {
                try {
                    result = (Value) pm.deserialize((URI) temp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                result = (Value) temp;
            }

            testGet.val = v;
            return result;
        }

        Node newNode = this.put(this.root, k, v, this.height);
        this.n++;
        if (newNode == null) {
            return null;
        }

        // split the root:
        // Create a new node to be the root.
        // Set the old root to be new root's first entry.
        // Set the node returned from the call to put to be new root's second entry
        Node newRoot = new Node(2);
        newRoot.entries[0] = new Entry(this.root.entries[0].key, null, this.root);
        newRoot.entries[1] = new Entry(newNode.entries[0].key, null, newNode);
        this.root = newRoot;
        // a split at the root always increases the tree height by 1
        this.height++;

        return null;

    };

    private Node put(Node n, Key k, Value v, int h) {
        int j;
        Entry newEntry = new Entry(k, v, null);

        if (h == 0) {
            for (j = 0; j < n.entryCount; j++) {
                if (k.compareTo((Key) n.entries[j].getKey()) < 0) {
                    break;
                }
            }
        } else {
            for (j = 0; j < n.entryCount; j++) {
                if (j + 1 == n.entryCount || k.compareTo((Key) n.entries[j + 1].getKey()) < 0) {
                    Node newNode = this.put(n.entries[j++].child, k, v, height - 1);

                    if (newNode == null) {
                        return null;
                    }

                    newEntry.key = newNode.entries[0].key;
                    newEntry.val = null;
                    newEntry.child = newNode;
                    break;
                }
            }
        }

        // shift entries over one place to make room for new entry
        for (int i = n.entryCount; i > j; i--) {
            n.entries[i] = n.entries[i - 1];
        }

        // add new entry
        n.entries[j] = newEntry;
        n.entryCount++;
        if (n.entryCount < BTreeImpl.MAX) {
            // no structural changes needed in the tree
            // so just return null
            return null;
        } else {
            // will have to create new entry in the parent due
            // to the split, so return the new node, which is
            // the node for which the new entry will be created
            return this.split(n, height);
        }
    }

    private Node split(Node currentNode, int height) {
        Node newNode = new Node(BTreeImpl.MAX / 2);
        // by changing currentNode.entryCount, we will treat any value
        // at index higher than the new currentNode.entryCount as if
        // it doesn't exist
        currentNode.entryCount = BTreeImpl.MAX / 2;
        // copy top half of h into t
        for (int j = 0; j < BTreeImpl.MAX / 2; j++) {
            newNode.entries[j] = currentNode.entries[BTreeImpl.MAX / 2 + j];
        }
        // external node
        if (height == 0) {
            newNode.setNext(currentNode.getNext());
            newNode.setPrevious(currentNode);
            currentNode.setNext(newNode);
        }
        return newNode;
    }

    public void moveToDisk(Key k) throws Exception {
        this.pm.serialize((URI) k, (Document) this.get(k));
        Entry nodeToSerialize = this.get(this.root, k, this.height);
        nodeToSerialize.val = k;
    };

    @Override
    public void setPersistenceManager(PersistenceManager<Key, Value> pm) {
        this.pm = (DocumentPersistenceManager) pm;
    };

}

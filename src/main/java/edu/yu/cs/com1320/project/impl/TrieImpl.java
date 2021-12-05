package edu.yu.cs.com1320.project.impl;

import java.util.*;

import edu.yu.cs.com1320.project.Trie;

public class TrieImpl<Value> implements Trie<Value> {

    private static final int alphabetSize = 36; 
    private Node root; // root of trie

    public TrieImpl () {

    }
    
    
    @Override
    public void put(String key, Value val) {
        if(key == null) {
            throw new IllegalArgumentException();
        }
        String newKey = key.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");
        if(val == null) {
            // this.deleteAll(key);
            return;
        } 


        this.root = this.put(this.root, newKey, val, 0);

    }

    private Node put(Node x, String key, Value val, int d) {
        if(x == null) {
            x = new Node();
        }

        if(d == key.length()) {
            x.vals.add(val);
            return x;
        }

        char c = key.charAt(d);
        int index = Character.getNumericValue(c);
        
        x.links[index] = this.put(x.links[index], key, val, d + 1);

        return x;

    }

    @Override
    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        if(comparator == null || key == null) {
            throw new IllegalArgumentException();
        }

        String newKey = key.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");

        Node<Value> result = this.get(this.root, newKey, 0);
        List<Value> finalResults = new ArrayList<>();
        Set<Value> results = new HashSet<>();
        if(result != null) {
            results.addAll(result.vals);
            finalResults.addAll(results);
            finalResults.sort(comparator);
        }


        return finalResults;
    }


    private Node get(Node x, String key, int d) {
        if(x == null) {
            return null;
        }

        if(key.length() == d) {
            return x;
        }

        char c = key.charAt(d);
        int index = Character.getNumericValue(c);
        // System.out.println("Key = " + key + " d = " + d + " CHAR = " + c + " = " + (index));
        return this.get(x.links[index], key, d + 1);
    }






    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if(prefix == null || comparator == null) {
            throw new IllegalArgumentException();
        }
        String newPrefix = prefix.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");

        if(newPrefix == "") {
            return new ArrayList<Value>();
        }


        Queue<Value> results = new ArrayDeque<Value>();
        Node endOfPrefix = this.get(this.root, newPrefix, 0);

        if(endOfPrefix != null) {
            this.collect(endOfPrefix, new StringBuilder(newPrefix), results);
        }

        Map<Value, Integer> numberOfHits = new HashMap<Value, Integer>();
        for(Value doc : results) {
            if(numberOfHits.containsKey(doc)) {
                numberOfHits.put(doc, numberOfHits.get(doc) + 1);
            } else {
                numberOfHits.put(doc, 1);
            }
        }


        List<Value> finalResults = new ArrayList<Value>(numberOfHits.keySet());
        finalResults.sort(comparator);


        return finalResults;
    }

    private void collect(Node x, StringBuilder prefix, Queue<Value> results) {
        if(x.vals.size() > 0) {
            results.addAll(x.vals);
        }

        for(int i = 0; i <= 9; i++) {
            if(x.links[i] != null) {
                prefix.append(i);
                this.collect(x.links[i], prefix, results);
                prefix.deleteCharAt(prefix.length() -1);
            }
        }
        for(char i = 65; i < 65 + 26; i++) {
            if(x.links[Character.getNumericValue(i)] != null) {
                prefix.append(i);
                this.collect(x.links[Character.getNumericValue(i)], new StringBuilder(prefix), results);

                prefix.deleteCharAt(prefix.length() -1);
            }
        }


    }




    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if(prefix == null) {
            throw new IllegalArgumentException();
        }

        String newPrefix = prefix.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");
        // System.out.println("newPrefix = " + newPrefix );

        if(prefix.equals("")) {
            return new HashSet<Value>();
        }

        Set<Value> results = new HashSet<Value>();
        Node endOfPrefix = this.get(this.root, newPrefix, 0);
        this.deleteAllWithPrefix(endOfPrefix, new StringBuilder(newPrefix), results);

        return results;
    }

    private void deleteAllWithPrefix(Node x, StringBuilder prefix, Set<Value> results) {
        if(x == null) {
            return ;
        }

        // System.out.println("String = " + prefix );

        if(x.vals.size() > 0) {
            // System.out.println("String = " + prefix );
            this.delete(this.root, new String(prefix) , 0, results);
        }

        for(int i = 0; i <= 9; i++) {
            if(x.links[i] != null) {
                prefix.append(i);
                this.deleteAllWithPrefix(x.links[i], prefix, results);
                prefix.deleteCharAt(prefix.length() -1);
            }
        }
        for(char i = 65; i < 65 + 26; i++) {
            if(x.links[Character.getNumericValue(i)] != null) {
                prefix.append(i);
                this.deleteAllWithPrefix(x.links[Character.getNumericValue(i)], prefix, results);

                prefix.deleteCharAt(prefix.length() -1);
            }
        }

        // for(char c = 0; c < alphabetSize; c++) {
        //     // int index = Character.getNumericValue(c);
        //     if(x.links[c] != null) {
        //         prefix.append(c);
        //         System.out.println("appended char = " + (char)c );
        //         this.deleteAllWithPrefix(x.links[c], prefix, results);

        //         prefix.deleteCharAt(prefix.length() -1);
        //     }
        // }
    }


    @Override
    public Set<Value> deleteAll(String key) {
        if(key == null) {
            throw new IllegalArgumentException();
        }

        String newKey = key.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");
        // System.out.println("Key = " + newKey );
        Set<Value> results = new HashSet<Value>();
        this.root = delete(this.root, newKey, 0, results);
        return results;
    }

    private Node delete(Node x, String key, int d, Set<Value> results) {
        if(x == null) {
            return null;
        }

        if(d == key.length()) {
            results.addAll(x.vals);
            x.vals.clear();
        } else {
            char c = key.charAt(d);
            int index = Character.getNumericValue(c);
            // System.out.println("Key = " + key + " d = " + d + " CHAR = " + c + " = " + (index));
            x.links[index] = this.delete(x.links[index], key, d + 1, results);
        }

        if(x.vals.size() > 0) {
            return x;
        }


        for(int i = 0; i <= 9; i++) {
            if(x.links[i] != null) {
                return x;
            }
        }
        for(char i = 65; i < 65 + 26; i++) {
            if(x.links[Character.getNumericValue(i)] != null) {
                return x;
            }
        }

        return null;


    }

    @Override
    public Value delete(String key, Value val) {
        if(key == null || val == null) {
            throw new IllegalArgumentException();
        }

        String newKey = key.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");

        Node nodeToDelete = this.get(this.root, newKey, 0);

        if(nodeToDelete != null && nodeToDelete.vals.contains(val)) {
            Value result = val;
            for(int i = 0; i < nodeToDelete.vals.size(); i++) {
                Value temp = (Value)nodeToDelete.vals.get(i);
                if( temp.equals(val)) {
                    result = temp;
                }
            }
            // Object[] result = nodeToDelete.vals.toArray();
            nodeToDelete.vals.remove(val);
            if(nodeToDelete.vals.isEmpty()) {
                this.delete(this.root, newKey, 0, new HashSet<Value>());
            }
            return result;
        } else {
            return null;
        }

        
    }


    private class Node<Value> {
        protected List<Value> vals = new ArrayList<>();
        protected Node[] links = new Node[alphabetSize];

    }

    
}
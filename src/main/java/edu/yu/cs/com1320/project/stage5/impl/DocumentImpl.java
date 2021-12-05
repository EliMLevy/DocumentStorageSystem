package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.*;

import java.net.URI;
import java.util.*;

public class DocumentImpl implements Document {

    private URI uri;
    private String txt = null;
    private byte[] binaryData = null;
    private Map<String, Integer> wordCount = new HashMap<>();

    private long lastUsedTime;

    public DocumentImpl(URI uri, String txt) {
        if (uri == null || txt == null || uri.toString().equals("") || txt.equals("")) {
            throw new IllegalArgumentException();
        }

        String[] words = txt.split(" ");
        for(String word : words) {
            String newWord = word.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");
            if(wordCount.containsKey(newWord)) {
                wordCount.put(newWord, wordCount.get(newWord) + 1);
            } else {
                wordCount.put(newWord,  1);
            }
        }

        this.uri = uri;
        this.txt = txt;
    }

    public DocumentImpl(URI uri, byte[] binaryData) {
        if (uri == null || binaryData == null || uri.toString().equals("") || binaryData.length == 0) {
            throw new IllegalArgumentException();
        }

        this.uri = uri;
        this.binaryData = binaryData;
    }

    /**
     * @return content of text document
     */
    public String getDocumentTxt() {
        return this.txt;
    }

    /**
     * @return content of binary data document
     */
    public byte[] getDocumentBinaryData() {
        return this.binaryData;
    }

    /**
     * @return URI which uniquely identifies this document
     */
    public URI getKey() {
        return this.uri;
    }

    @Override
    public int hashCode() {
        int result = uri.hashCode();
        result = 31 * result + (this.txt != null ? this.txt.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(binaryData);
        return result;
    }


    @Override
    public boolean equals(Object o) {
        if(o == null) {
            return false;
        }
        return this.hashCode() == o.hashCode();
    }

    @Override
    public int wordCount(String word) {
        String newWord = word.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");
        if(wordCount.containsKey(newWord)) {
            return wordCount.get(newWord);
        } else {
            return 0;
        }
        // return 0;
    }

    @Override
    public Set<String> getWords() {
        
        return wordCount.keySet();
    }

    @Override
    public int compareTo(Document o) {
        return this.getLastUseTime() > o.getLastUseTime() ? 1 : -1;
    }

    @Override
    public long getLastUseTime() {
        return this.lastUsedTime;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.lastUsedTime = timeInNanoseconds;
        
    }

    @Override
    public Map<String, Integer> getWordMap() {
        return wordCount;
    }

    @Override
    public void setWordMap(Map<String, Integer> wordMap) {
        this.wordCount = wordMap;
    }
}

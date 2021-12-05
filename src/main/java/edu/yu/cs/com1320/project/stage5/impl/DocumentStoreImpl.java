package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.*;
import edu.yu.cs.com1320.project.stage5.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class DocumentStoreImpl implements DocumentStore {

    // private HashTableImpl<URI, DocumentImpl> table = new HashTableImpl<URI,
    // DocumentImpl>();

    private StackImpl<Undoable> commandStack = new StackImpl<>();

    private TrieImpl<URI> docTrie = new TrieImpl<>();

    private MinHeapImpl<FakeDoc> minHeap = new MinHeapImpl<>();

    private BTreeImpl<URI, Document> btree = new BTreeImpl<>();

    private int docLimit;
    private int byteLimit;
    private boolean docLimitSet = false;
    private boolean byteLimitSet = false;

    private int docCount = 0;
    private int byteCount = 0;

    private List<URI> urisWrittenToDisk = new ArrayList<>();

    public DocumentStoreImpl() {

    }

    public DocumentStoreImpl(File baseDir) {
        if(baseDir != null) {
            DocumentPersistenceManager pm = new DocumentPersistenceManager(baseDir);
            this.btree.setPersistenceManager(pm);
        }
    }

    private class FakeDoc implements Comparable<FakeDoc> {
        private URI uri;
        private BTree<URI, Document> btree;

        private FakeDoc(BTree<URI, Document> btree, URI uri) {
            this.uri = uri;
            this.btree = btree;
        }

        @Override
        public int compareTo(FakeDoc o) {
            if (this.uri.equals(o.uri)) {
                return 0;
            }
            // System.out.println("Comparing " + o.uri + " and " + this.uri);
            return this.getLastUseTime() > o.getLastUseTime() ? 1 : -1;
        }

        private long getLastUseTime() {
            // assert this.btree.get(this.uri) != null;
            return this.btree.get(this.uri).getLastUseTime();
        }

    }

    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input
     * @throws IllegalArgumentException if uri or format are null
     */
    public int putDocument(InputStream input, URI uri, DocumentStore.DocumentFormat format) throws IOException {
        if (format == null || uri == null) {
            throw new IllegalArgumentException();
        }
        

        Document prevDoc = null;
        Document docToPut = null;
        Function<Document, Boolean> undoPut;

        if (input != null) {
            byte[] byteData;
            try {
                byteData = input.readAllBytes();
            } catch (Exception e) {
                throw new IOException("Issue reading the input");
            }
            String text = new String(byteData);
            prevDoc = this.btree.get(uri);
            removeDocFromHeapAndTrie(prevDoc);
            if (format == DocumentStore.DocumentFormat.TXT) {
                docToPut = this.putTextDoc(text, uri);
            } else {
                docToPut = this.putBinaryDoc(byteData, uri);
            }
            
            List<URI> serializedURIs = this.clearMemory();
            undoPut = generateUndoPut(docToPut, prevDoc,serializedURIs);
            this.commandStack.push(new GenericCommand<Document>(this.btree.get(uri), undoPut));
        } else {
            prevDoc = this.btree.put(uri, null);

            undoPut = generateUndoDelete(prevDoc);
            this.commandStack.push(new GenericCommand<Document>(null, undoPut));
        }


        if (prevDoc == null) {
            return 0;
        }

        this.byteCount -= this.calcStorage(prevDoc);
        return prevDoc.hashCode();
    }

    private void removeDocFromHeapAndTrie(Document doc) {
        if (doc != null) {
            for (String word : doc.getWords()) {
                docTrie.delete(word, doc.getKey());
            }
            doc.setLastUseTime(0);
            this.minHeap.reHeapify(new FakeDoc(this.btree, doc.getKey()));
            this.minHeap.remove();
        }
    }

    private Function<Document, Boolean> generateUndoPut(Document newDoc, Document oldDoc, List<URI> serializedURIs) {
        Function<Document, Boolean> undoFunction = (docToUndo) -> {
            Document old = oldDoc;
            List<URI> serializedURIsToPutBack = serializedURIs;
            this.byteCount -= this.calcStorage(docToUndo); // Subtract the bytes of the newer doc
            if(oldDoc != null) {
                old.setLastUseTime(System.nanoTime()); // Set the time for the new doc
                this.minHeap.insert(new FakeDoc(this.btree, old.getKey()));
                btree.put(old.getKey(), (DocumentImpl) old); // Put the new doc back
                this.byteCount += this.calcStorage(old); // Add the bytes of the new doc
                for (String word : old.getWords()) { // Put all the words in the Trie
                    docTrie.put(word, old.getKey());
                }
            } else {
                docToUndo.setLastUseTime(0);
                this.minHeap.reHeapify(new FakeDoc(this.btree, docToUndo.getKey()));
                this.minHeap.remove();
                this.docCount--;
            }

            for(URI uri :serializedURIsToPutBack ) {
                Document doc = this.btree.get(uri);
                doc.setLastUseTime(10);
            }
            btree.put(docToUndo.getKey(), old);
            for (String word : docToUndo.getWords()) {
                docTrie.delete(word, docToUndo.getKey());
            }

            return true;
        };

        return undoFunction;
    }

    private Function<Document, Boolean> generateUndoDelete(Document deletedDoc) {
        Function<Document, Boolean> undoDelete = (docToPutBack) -> {
            Document deceasedDoc = deletedDoc;
            // Put words back in the Trie
            for (String word : deceasedDoc.getWords()) {
                docTrie.put(word, deceasedDoc.getKey());
            }
            if (this.btree.get(deceasedDoc.getKey()) == null) { // If there is no doc there now
                this.docCount++;
            } else {
                this.byteCount -= this.calcStorage(this.btree.get(deceasedDoc.getKey()));
            }
            this.byteCount += this.calcStorage(deceasedDoc);

            deceasedDoc.setLastUseTime(System.nanoTime()); // Update the last used time

            btree.put(deceasedDoc.getKey(), (DocumentImpl) deceasedDoc); // Put it into the table

            this.minHeap.insert(new FakeDoc(this.btree, deceasedDoc.getKey())); // Put it into the heap

            this.clearMemory(); // Make sure we stay under memory limits

            return true;
        };

        return undoDelete;
    }

    private Document putTextDoc(String text, URI uri) {

        DocumentImpl docToPut = new DocumentImpl(uri, text);
        for (String word : docToPut.getWords()) {
            docTrie.put(word, uri);
        }

        docToPut.setLastUseTime(System.nanoTime());
        Document result = btree.put(uri, docToPut);
        this.minHeap.insert(new FakeDoc(this.btree, uri));
        this.docCount++;
        this.byteCount += this.calcStorage(docToPut);
        return result;
    }

    private Document putBinaryDoc(byte[] data, URI uri) {

        DocumentImpl docToPut = new DocumentImpl(uri, data);
        docToPut.setLastUseTime(System.nanoTime());
        Document result = btree.put(uri, docToPut);
        this.minHeap.insert(new FakeDoc(this.btree, uri));
        this.docCount++;
        this.byteCount += this.calcStorage(docToPut);

        return result;

    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document
     */
    public Document getDocument(URI uri) {
        Document result = btree.get(uri);
        if(this.urisWrittenToDisk.contains(uri)) {
            this.docCount++;
            this.byteCount += this.calcStorage(result);
            result.setLastUseTime(System.nanoTime());
            this.minHeap.insert(new FakeDoc(this.btree, uri));
            this.urisWrittenToDisk.remove(uri);
        }


        if (result != null) {
            result.setLastUseTime(System.nanoTime());
            this.minHeap.reHeapify(new FakeDoc(this.btree, result.getKey()));
        }
        this.clearMemory();
        return result;
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with
     *         that URI
     */
    public boolean deleteDocument(URI uri) {
        Document docToDelete = getDocument(uri);

        // Remove doc from trie
        if (docToDelete != null) {
            for (String word : docToDelete.getWords()) {
                docTrie.delete(word, docToDelete.getKey());
            }

            //Remove from minheap
            docToDelete.setLastUseTime(0);
            this.minHeap.reHeapify(new FakeDoc(this.btree, docToDelete.getKey()));
            this.minHeap.remove();

            // Update memory counters
            this.docCount--;
            this.byteCount -= this.calcStorage(docToDelete);
        }

        // Create proper lambda function to put it back
        Function<Document, Boolean> undoDelete = this.generateUndoDelete(docToDelete);

        // Clear the space in the hashtable
        Document result = btree.put(uri, null);

        // Place the lambda on the commandStack
        this.commandStack.push(new GenericCommand<Document>(docToDelete, undoDelete));

        if (result != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void undo() throws IllegalStateException {
        Undoable commandToUndo = this.commandStack.pop();
        if (commandToUndo != null) {
            commandToUndo.undo();
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    public void undo(URI uri) throws IllegalStateException {
        Undoable currentCommand = this.commandStack.pop();

        StackImpl<Undoable> tempStack = new StackImpl<>();

        boolean found = false;
        boolean foundInSet = false;

        while (currentCommand != null && !found) { // Either the commandstack is empty or we found the command to undo
            if (currentCommand instanceof GenericCommand) { // Do a simple ceck if it is a single command
                GenericCommand<Document> genericCurrentCommand = (GenericCommand<Document>) currentCommand;
                Document temp = genericCurrentCommand.getTarget(); // The target of a generic command is the document

                if (temp != null && temp.getKey() != null && temp.getKey().equals(uri)) {
                    found = true;
                } else {
                    tempStack.push(currentCommand);
                    currentCommand = commandStack.pop();
                }
            } else {
                // If it is a command set
                CommandSet<URI> cmdSet = (CommandSet<URI>) currentCommand;

                if (cmdSet.undo(uri)) {// this will return true if it was in the set and false if not
                    found = true;
                    foundInSet = true;
                    if (cmdSet.size() != 0) {
                        tempStack.push(cmdSet);
                    }
                }

                if (!found) {
                    tempStack.push(currentCommand);
                    currentCommand = commandStack.pop();
                }
            }
        }

        if (currentCommand != null && !foundInSet) { // We found the command associate with the URI not in a set
            currentCommand.undo();
        }
        while (tempStack.peek() != null) { // Put back the other commands
            this.commandStack.push(tempStack.pop());
        }
        if (currentCommand == null) {
            throw new IllegalStateException();
        }
    }

    @Override
    public List<Document> search(String keyword) {
        String newKeyword = keyword.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");
        List<URI> resultsURIs = docTrie.getAllSorted(newKeyword, (e1, e2) -> {
            return this.btree.get(e2).wordCount(newKeyword) - this.btree.get(e1).wordCount(newKeyword);
        });

        List<Document> results = new LinkedList<>();
        for (URI uri : resultsURIs) {
            results.add(this.btree.get(uri));
        }

        for (Document doc : results) {
            doc.setLastUseTime(System.nanoTime());
            this.minHeap.reHeapify(new FakeDoc(this.btree, doc.getKey()));
        }

        this.clearMemory();

        return results;
    }

    @Override
    public List<Document> searchByPrefix(String keywordPrefix) {
        String newPrefix = keywordPrefix.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");

        List<URI> resultsURIs = docTrie.getAllWithPrefixSorted(newPrefix, (e1, e2) -> {
            int lengthOfPrefix = newPrefix.length();

            int appearancesInE1 = 0;
            for (String word : this.btree.get(e1).getWords()) {
                if (word.length() >= lengthOfPrefix && newPrefix.equals(word.substring(0, lengthOfPrefix))) {
                    appearancesInE1 += this.btree.get(e1).wordCount(word);
                }
            }

            int appearancesInE2 = 0;
            for (String word : this.btree.get(e2).getWords()) {
                if (word.length() >= lengthOfPrefix && newPrefix.equals(word.substring(0, lengthOfPrefix))) {
                    appearancesInE2 += this.btree.get(e2).wordCount(word);
                }
            }

            return appearancesInE2 - appearancesInE1;
        });

        List<Document> results = new LinkedList<>();
        for (URI uri : resultsURIs) {
            results.add(this.btree.get(uri));
        }

        long time = System.nanoTime();
        for (Document doc : results) {
            doc.setLastUseTime(time);
            this.minHeap.reHeapify(new FakeDoc(this.btree, doc.getKey()));
        }

        this.clearMemory();

        return results;
    }

    @Override
    public Set<URI> deleteAll(String keyword) {
        Set<URI> deletedURIs = new HashSet<>();
        String newKeyword = keyword.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");

        List<URI> docsToDeleteURIs = docTrie.getAllSorted(newKeyword, (e1, e2) -> {
            return this.btree.get(e2).wordCount(newKeyword) - this.btree.get(e1).wordCount(newKeyword);
        });

        List<Document> docsToDelete = new LinkedList<>();
        for (URI uri : docsToDeleteURIs) {
            docsToDelete.add(this.btree.get(uri));
        }

        CommandSet<URI> commandsToUndo = new CommandSet<>();
        for (Document doc : docsToDelete) {
            deletedURIs.add(doc.getKey());
            individualDelete(doc, commandsToUndo);

        }

        this.commandStack.push(commandsToUndo);

        return deletedURIs;
    }

    private void individualDelete(Document doc, CommandSet<URI> commands) {
        Document oldDoc = getDocument(doc.getKey());

        // Remove from Trie
        for (String word : oldDoc.getWords()) {
            docTrie.delete(word, oldDoc.getKey());
        }

        // Remove from heap
        doc.setLastUseTime(0);
        this.minHeap.reHeapify(new FakeDoc(this.btree, doc.getKey()));
        this.minHeap.remove();

        Function<URI, Boolean> undoDelete = (uriToPutBack) -> {
            Document save = oldDoc;
            for (String word : save.getWords()) {
                docTrie.put(word, save.getKey());
            }

            if (this.btree.get(uriToPutBack) == null) {
                this.docCount++;
            } else {
                this.byteCount -= this.calcStorage(this.btree.get(uriToPutBack));
            }

            this.byteCount += this.calcStorage(save);
            save.setLastUseTime(System.nanoTime());

            btree.put(uriToPutBack, (DocumentImpl) save);

            this.minHeap.insert(new FakeDoc(this.btree, save.getKey()));
            this.clearMemory();
            return true;
        };

        this.docCount--;
        this.byteCount -= this.calcStorage(doc);

        btree.put(doc.getKey(), null);

        commands.addCommand(new GenericCommand<URI>(doc.getKey(), undoDelete));

    }

    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<URI> deletedURIs = new HashSet<>();
        String newKeyword = keywordPrefix.toUpperCase().replaceAll("[^a-zA-Z0-9]", "");

        List<URI> docsToDeleteURIs = docTrie.getAllWithPrefixSorted(newKeyword, (e1, e2) -> {
            return this.btree.get(e2).wordCount(newKeyword) - this.btree.get(e1).wordCount(newKeyword);
        });

        List<Document> docsToDelete = new LinkedList<>();
        for (URI uri : docsToDeleteURIs) {
            docsToDelete.add(this.btree.get(uri));
        }

        CommandSet<URI> commandsToUndo = new CommandSet<>();
        for (Document doc : docsToDelete) {
            deletedURIs.add(doc.getKey());
            individualDelete(doc, commandsToUndo);
        }

        this.commandStack.push(commandsToUndo);

        return deletedURIs;
    }

    @Override
    public void setMaxDocumentCount(int limit) {
        this.docLimit = limit;
        this.docLimitSet = true;
        this.clearMemory();
    }

    @Override
    public void setMaxDocumentBytes(int limit) {
        this.byteLimit = limit;
        this.byteLimitSet = true;
        this.clearMemory();
    }

    private int calcStorage(Document doc) {
        if (doc.getDocumentTxt() != null) {
            return doc.getDocumentTxt().getBytes().length;
        } else {
            return doc.getDocumentBinaryData().length;
        }
    }

    private List<URI> clearMemory() {
        // System.out.println("Doc count = " + this.docCount);
        // System.out.println("Doc limit = " + this.docLimit);

        List<URI> serializedURIs = new ArrayList<>();
        while ((this.docCount > this.docLimit && this.docLimitSet)
                || (this.byteCount > this.byteLimit && this.byteLimitSet)) {

            URI oldest = this.minHeap.remove().uri;
            Document oldestDoc = this.btree.get(oldest);
            // System.out.println("oldest doc " + oldest);
            if (oldestDoc != null) {
                try {
                    urisWrittenToDisk.add(oldest);
                    this.btree.moveToDisk(oldest);
                    serializedURIs.add(oldest);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // Decrease docCount and byteCount
                this.docCount--;
                this.byteCount -= this.calcStorage(oldestDoc);

            }
        }

        return serializedURIs;
    }

}

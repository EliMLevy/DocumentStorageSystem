package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.*;

import org.json.JSONObject;

/**
 * created by the document store and given to the BTree via a call to
 * BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {

    private File baseDir;

    // private Gson gson = new Gson();
    private Gson gson = new GsonBuilder().registerTypeAdapter(DocumentImpl.class, new DocumentSerializer())
            .registerTypeAdapter(DocumentImpl.class, new DocumentDeserializer()).create();

    public DocumentPersistenceManager(File baseDir) {
        this.baseDir = baseDir;
    }

    private class DocumentSerializer implements JsonSerializer<DocumentImpl> {

        @Override
        public JsonElement serialize(DocumentImpl src, java.lang.reflect.Type typeOfSrc,
                JsonSerializationContext context) {

            JsonObject j = new JsonObject();
            if (src.getDocumentTxt() != null) {
                j.add("text", new JsonPrimitive(src.getDocumentTxt()));
            } else {
                j.add("text", new JsonPrimitive(""));
            }
            String bnry = "";
            byte[] b = src.getDocumentBinaryData();
            if (b != null) {
                for (int n : b) {
                    bnry += n + " ";
                }
            }
            j.add("bnry", new JsonPrimitive(bnry));
            j.add("uri", new JsonPrimitive(src.getKey().toString()));

            String wcm = "";
            Map<String, Integer> wordCountMap = src.getWordMap();
            if (wordCountMap != null) {
                for (String s : wordCountMap.keySet()) {
                    wcm += s + "-" + wordCountMap.get(s) + " ";
                }
            }
            j.add("wordCountMap", new JsonPrimitive(wcm));

            return j;
        }
    }

    public class DocumentDeserializer implements JsonDeserializer<DocumentImpl> {
        @Override
        public DocumentImpl deserialize(JsonElement json, java.lang.reflect.Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            DocumentImpl doc;

            String txt = jsonObject.get("text").getAsString();
            String uri = jsonObject.get("uri").getAsString();
            String[] wcm = jsonObject.get("wordCountMap").getAsString().split(" ");
            Map<String, Integer> wordCountMap = new HashMap<>();
            String[] bnry = jsonObject.get("bnry").getAsString().split(" ");
            byte[] bytes = new byte[bnry.length];
            if (bnry.length > 1) {
                for (int i = 0; i < bnry.length; i++) {
                    bytes[i] = (byte) Integer.parseInt(bnry[i]);
                }
            } else {
                for (String s : wcm) {
                    String[] pair = s.split("-");
                    wordCountMap.put(pair[0], Integer.parseInt(pair[1]));
                }
            }

            try {
                if (bnry.length < 1) {
                    doc = new DocumentImpl(new URI(uri), txt);
                } else {
                    doc = new DocumentImpl(new URI(uri), bytes);
                }

            } catch (Exception e) {
                doc = null;
                e.printStackTrace();
            }

            doc.setWordMap(wordCountMap);

            return doc;
        }
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        String json = gson.toJson(val);
        // Make file
        File newFile = new File(this.baseDir, uriToPath(uri));
        // Make directory
        newFile.mkdirs();
        // Write json to the file
        FileWriter myWriter = new FileWriter(newFile.getPath() + "\\" + uriToFileName(uri) + ".json");
        myWriter.write(json);
        myWriter.close();
    }

    @Override
    public Document deserialize(URI uri) throws IOException {

        Path target = Path.of(this.baseDir.getAbsolutePath(), uriToPath(uri), uriToFileName(uri) + ".json");

        String contents = Files.readString(target);

        Document result = gson.fromJson(contents, DocumentImpl.class);

        this.delete(uri);
        return result;

    }

    @Override
    public boolean delete(URI uri) throws IOException {
        try {
            Files.delete(Path.of(this.baseDir.getAbsolutePath(), uriToPath(uri), uriToFileName(uri) + ".json"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String uriToPath(URI uri) {
        String endOfPath = uri.getPath();
        String[] segs = endOfPath.split("/");
        String correctEnd;
        if (segs.length > 1) {
            correctEnd = "\\" + segs[0];
            for (int i = 1; i < segs.length - 1; i++) {
                correctEnd += "\\" + segs[i];
            }
        } else {
            correctEnd = "";
        }

        return uri.getHost() + correctEnd;
    }

    private String uriToFileName(URI uri) {
        String endOfPath = uri.getPath();
        String[] segs = endOfPath.split("/");

        return segs[segs.length - 1];
    }
}

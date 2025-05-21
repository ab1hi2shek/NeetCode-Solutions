package lld.word_search;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.*;

public class SearchEngine {

    private Map<String, Map<String, List<Integer>>> store;

    SearchEngine() {
        this.store = new HashMap<>();
    }

    private void buildIndex(String filename) {

        String fileContent = readFileContent(filename);
        System.out.println("fileContent = " + fileContent);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fileContent.length(); i++) {
            char c = fileContent.charAt(i);
            if (isAplhabetical(c)) {
                sb.append(c);
            } else {
                if (!sb.isEmpty()) {
                    addWordToStore(sb, i, filename);
                    sb = new StringBuilder();
                }
            }

        }

        if (!sb.isEmpty()) {
            addWordToStore(sb, fileContent.length(), filename);
        }
    }

    private void addWordToStore(StringBuilder sb, int currentFileIndex, String filename) {
        String word = sb.toString().toLowerCase();
        int index = currentFileIndex - word.length();

        store.putIfAbsent(word, new HashMap<>());
        store.get(word).putIfAbsent(filename, new ArrayList<>());
        store.get(word).get(filename).add(index);

        System.out.println("word = " + word + " index = " + index);
    }

    private List<String> searchWords(String word) {

        List<String> result = new ArrayList<>();
        if (store.containsKey(word)) {
            Map<String, List<Integer>> fileMap = store.get(word);
            for (Map.Entry<String, List<Integer>> entry : fileMap.entrySet()) {
                result.add(entry.getKey());
            }
        }

        return result;

    }

    private List<String> searchPhrase(String phrase) {

        List<String> result = new ArrayList<>();
        String[] words = phrase.split("\\W+");

        // If phrase is empty, return empty list for now.
        if (words.length == 0) {
            return result;
        }

        // If any word is not in map, meaning no file have this phrase, so return empty
        // list.
        for (String word : words) {
            if (!store.containsKey(word)) {
                return result;
            }
        }

        Map<String, Integer> temp = new HashMap<>();

        // Add first word filename and index to temp
        Map<String, List<Integer>> firstWordMap = store.get(words[0]);
        for (Map.Entry<String, List<Integer>> entry : firstWordMap.entrySet()) {
            temp.put(entry.getKey(), entry.getValue().get(0));
        }

        // Compare all other words
        for (int i = 1; i < words.length; i++) {
            Map<String, List<Integer>> remainingWordMap = store.get(words[i]);

            Iterator<String> it = temp.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();

                // Remove any filename from temp if it does not exist in remainingWordMap;
                if (!remainingWordMap.containsKey(key)) {
                    it.remove();
                }
                // if filename is there, get the lowest indices of matching words greater than
                // indices in temp;
                else {
                    List<Integer> indices = remainingWordMap.get(key);
                    int applicableIndex = getFirstElementGreaterThanInput(temp.get(key), indices);

                    if (applicableIndex == -1) {
                        it.remove(); // if no appropriate indices found, remove filename from temp map.
                    } else {
                        temp.put(key, applicableIndex); // if found, update the minum found index.
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> entry : temp.entrySet()) {
            result.add(entry.getKey());
        }

        return result;
    }

    private String readFileContent(String filename) {
        try {
            return Files.readString(Path.of(filename));
        } catch (Exception e) {
            System.out.println("Error while reading contents of file: " + filename);
            System.out.println(e);
            return "";
        }
    }

    private boolean isAplhabetical(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
    }

    private int getFirstElementGreaterThanInput(int input, List<Integer> indices) {
        for (int i = 0; i < indices.size(); i++) {
            if (indices.get(i) > input) {
                return indices.get(i);
            }
        }

        return -1;
    }

    public static void main(String[] args) {

        SearchEngine searchEngine = new SearchEngine();
        searchEngine.buildIndex("lld/word_search/file1.txt");
        searchEngine.buildIndex("lld/word_search/file2.txt");
        searchEngine.buildIndex("lld/word_search/file3.txt");
        searchEngine.buildIndex("lld/word_search/file4.txt");

        System.out.println(searchEngine.searchWords("chapter"));

        System.out.println(searchEngine.searchPhrase("abhishek is my"));
    }

}

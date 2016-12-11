import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by ravipatel on 4/17/16.
 */
public class Trie {
    TrieNode root;

    public Trie(char c, HashMap<Character, TrieNode> children, TrieNode parent) {
        root = new TrieNode(c, children, parent);
    }

    public Trie() {
        root = new TrieNode(' ', new HashMap<Character, TrieNode>());
    }


    public TrieNode get(String key) {
        TrieNode x = get(root, key, 0);
        if (x == null) {
            return null;
        }
        return x;
    }

    private TrieNode get(TrieNode x, String key, int d) {
        if (x == null) {
            return null;
        }
        if (d == key.length()) {
            return x;
        }
        char c = key.charAt(d);
        return get(x.possibleCharacters.get(c), key, d + 1);
    }



    public void put(String key, String id) {
        put(root, key, 0, id);
    }

    private void put(TrieNode x, String key, int d, String id) {
        if (x == null) {
            x = new TrieNode(' ', new HashMap<Character, TrieNode>());
        }
        if (d == key.length()) {
            x.isAWord = true;
            x.id.add(id);
        } else {
            char c = key.charAt(d);
            if (!x.possibleCharacters.containsKey(c)) {
                TrieNode toAdd = new TrieNode(c, new HashMap<Character, TrieNode>(), x);
                x.possibleCharacters.put(c, toAdd);
                x.childrenWords.add(key);
                put(x.possibleCharacters.get(c), key, d + 1, id);
            } else {
                x.childrenWords.add(key);
                put(x.possibleCharacters.get(c), key, d + 1, id);
            }
        }

        x.size += 1;
    }


    public ArrayList<String> autoComplete(String word) {
        TrieNode node = get(word);
        ArrayList<String> converter = new ArrayList<>(node.childrenWords);
        return converter;
    }





    /* got the structure and most of the implementation of the TrieST.java from Princeton */


}

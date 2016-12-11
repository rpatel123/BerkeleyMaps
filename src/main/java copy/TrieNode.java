import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by ravipatel on 4/17/16.
 */
public class TrieNode {
    char c;
    HashMap<Character, TrieNode> possibleCharacters;
//    HashMap<String, String> lowerToNormalCase;
    HashSet<String> childrenWords;
    TrieNode parent;
    int size;
    boolean isAWord;
    ArrayList<String> id;

    public TrieNode(char c, HashMap<Character, TrieNode> possibleCharacters, TrieNode parent) {
        this.c = c;
        this.possibleCharacters = possibleCharacters;
        this.parent = parent;
        size = 0;
        id = new ArrayList<>();
        childrenWords = new HashSet<>();
    }

    public TrieNode(char c, HashMap<Character, TrieNode> possibleCharacters) {
        this(c, possibleCharacters, null);
    }

    public boolean isLeaf() {
        return this.possibleCharacters.size() == 0;
    }

    public int size() {
        return size;
    }
}

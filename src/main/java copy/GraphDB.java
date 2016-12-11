import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.HashSet;

/**
 * Wraps the parsing functionality of the MapDBHandler as an example.
 * You may choose to add to the functionality of this class if you wish.
 * @author Alan Yao
 */
public class GraphDB {

    static HashMap<String, Node> graph = new HashMap<>();
    Node curr;
    ArrayList<Node> connector = new ArrayList();
    boolean connect = false;
    Trie trie;
    HashMap<String, String> lowerToNormalCase;
    HashMap<String, ArrayList<String>> getID;
    HashMap<String, Node> stringToNode;


    /**
     * Example constructor shows how to create and start an XML parser.
     * @param dbPath Path to the XML file to be parsed.
     */
    public GraphDB(String dbPath) {
        trie = new Trie();
        lowerToNormalCase = new HashMap<>();
        getID = new HashMap<>();
        stringToNode = new HashMap<>();
        try {
            File inputFile = new File(dbPath);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            MapDBHandler maphandler = new MapDBHandler(this);
            saxParser.parse(inputFile, maphandler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        HashSet<Node> nodesToDelete = new HashSet<>();
        for (Node node : graph.values()) {
            if (node.connection.size() == 0) {
                nodesToDelete.add(node);
            }
        }

        cleanHelper(nodesToDelete);
    }

    private void cleanHelper(HashSet<Node> toDelete) {
        for (Node node : toDelete) {
            graph.remove(node.id);
        }

    }
}

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ravipatel on 4/15/16.
 */
public class Node {
    String id;
    String lat;
    String lon;
    String name;
    Set<Node> connection;
    SearchNode searchNode;

    public Node(String id, String lat, String lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.name = "";
        connection = new HashSet<>();
        searchNode = null;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Node node = (Node) o;
//
//        if (id != null ? !id.equals(node.id) : node.id != null) return false;
//        if (lat != null ? !lat.equals(node.lat) : node.lat != null) return false;
//        if (lon != null ? !lon.equals(node.lon) : node.lon != null) return false;
//        return connection != null ? connection.equals(node.connection) : node.connection == null;
//
//    }

//    @Override
//    public int hashCode() {
//        return Integer.parseInt(id);
//    }
}

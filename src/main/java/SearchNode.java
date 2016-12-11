/**
 * Created by ravipatel on 4/16/16.
 */

public class SearchNode implements Comparable<SearchNode> {
    Node node;
    double totalDistanceFromStart;
    boolean marked;
    double priority;
    double euclideanDistance;
    SearchNode previous;

    public SearchNode(Node n, double dis, boolean visited, double euclDist, SearchNode prev) {
        node = n;
        node.searchNode = this;
        totalDistanceFromStart = dis;
        marked = visited;
        euclideanDistance = euclDist;
        priority = totalDistanceFromStart + euclideanDistance;
        previous = prev;
    }


    @Override
    public int compareTo(SearchNode s) {
        if (s == null) {
            return 0;
        }

        if (priority == s.priority) {
            return 0;
        } else if (priority > s.priority) {
            return 1;
        } else {
            return -1;
        }
    }

}

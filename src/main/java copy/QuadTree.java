import java.util.ArrayList;
import java.util.Map;


public class QuadTree {
    QTreeNode root;
    public QuadTree(double ullon, double ullat, double lrlon, double lrlat, String name) {
        root = new QTreeNode(ullon, ullat, lrlon, lrlat, name);
        root.makeChild(root, 0);
    }



    public void intersect(int i, QTreeNode n, Map<String,  Double> params, ArrayList<QTreeNode> list) {
        if (n.getDepth() > i) {
            return;
        }
       QTreeNode[] children = n.getChildren();
        for (QTreeNode n : children) {
            intersect(i, n, params, list);
        }
        if (i == n.getDepth()) {
            if (rightTiles(n, params)) {
                list.add(n);
            } else {
                return;
            }
        } else if (n.getDepth() >= 7) {
            if (rightTiles(n, params)) {
                list.add(n);
            else {
                return;
            }
        }

        System.out.print("i'm rout and im dumb");
    }



    public void intersectHelper(int i, Map<String,  Double> params, ArrayList<QTreeNode> list) {
        QTreeNode node = root;
        intersect(i, node, params, list);
    }

    public boolean rightTiles(QTreeNode node, Map<String,  Double> params) {
        double ullon = params.get("ullon");
        double ullat = params.get("ullat");
        double lrlon = params.get("lrlon");
        double lrlat = params.get("lrlat");
        if (node.getUllat() > lrlat) {
            return true;
        } else if (ullat > node.getLrlat()) {
            return true;
        } else if (node.getUllon() < lrlon) {
            return true;    
        } else if (ullon < node.getLrlon()) {
            return true;
        } else {
            return false;
    }

}

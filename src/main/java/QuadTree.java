import java.util.ArrayList;
import java.util.Map;

/**
 * Created by ravipatel on 4/11/16.
 */
public class QuadTree {
    QTreeNode root;

    public QuadTree(double ullon, double ullat, double lrlon, double lrlat, String name) {
        root = new QTreeNode(ullon, ullat, lrlon, lrlat, name);
        root.createChildren(root, 0);
    }


    public void helper(int count, Map<String,  Double> params, ArrayList<QTreeNode> list) {
        QTreeNode node = root;
        correctTiles(count, node, params, list);
    }

    public void correctTiles(int count, QTreeNode n, Map<String,  Double> params,
                             ArrayList<QTreeNode> list) {

        if (n != null) {
            if (n.getDepth() > count) {
                return;
            }

            QTreeNode[] checker = n.getChildren();

            for (QTreeNode newNode : checker) {
                correctTiles(count, newNode, params, list);
            }

            if ((n.getDepth() == count || n.getDepth() >= 7) && isTrue(n, params)) {
                list.add(n);
            }
        }

    }

    public boolean isTrue(QTreeNode node, Map<String,  Double> params) {
        double ullon = params.get("ullon");
        double ullat = params.get("ullat");
        double lrlon = params.get("lrlon");
        double lrlat = params.get("lrlat");


        if (node.getUllat() < lrlat || ullat < node.getLrlat()) {
            return false;
        }

        if (node.getUllon() > lrlon || ullon > node.getLrlon()) {
            return false;
        }

        return true;


    }




}

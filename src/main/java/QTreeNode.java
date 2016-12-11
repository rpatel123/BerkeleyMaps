/**
 * Created by ravipatel on 4/11/16.
 */
public class QTreeNode implements Comparable<QTreeNode> {
    private double ullon;
    private double ullat;
    private double lrlon;
    private double lrlat;
    private String imageName;
    private QTreeNode[] children;
    int depth;

    public QTreeNode(double ullon, double ullat, double lrlon, double lrlat, String imageName) {
        this.ullon = ullon;
        this.ullat = ullat;
        this.lrlon = lrlon;
        this.lrlat = lrlat;
        this.imageName = imageName;
        this.children = new QTreeNode[4];
        this.depth = imageName.length();
    }

    public void createChildren(QTreeNode parent, int d) {
        if (d >= 7) {
            return;
        }

        double midLon = (parent.lrlon + parent.ullon) / 2;
        double midLat = (parent.lrlat + parent.ullat) / 2;

        QTreeNode firstChild = new QTreeNode(parent.ullon, parent.ullat, midLon,
                midLat, parent.imageName + "1");
        QTreeNode secondChild = new QTreeNode(midLon, parent.ullat, parent.lrlon,
                midLat, parent.imageName + "2");
        QTreeNode thirdChild = new QTreeNode(parent.ullon, midLat, midLon,
                parent.lrlat, parent.imageName + "3");
        QTreeNode fourthChild = new QTreeNode(midLon, midLat, parent.lrlon,
                parent.lrlat, parent.imageName + "4");


        children[0] = firstChild;
        children[1] = secondChild;
        children[2] = thirdChild;
        children[3] = fourthChild;

        firstChild.createChildren(firstChild, d + 1);
        secondChild.createChildren(secondChild, d + 1);
        thirdChild.createChildren(thirdChild, d + 1);
        fourthChild.createChildren(fourthChild, d + 1);

    }


    public int getDepth() {
        return depth;
    }

    public double getUllon() {
        return ullon;
    }

    public double getUllat() {
        return ullat;
    }

    public double getLrlon() {
        return lrlon;
    }

    public double getLrlat() {
        return lrlat;
    }

    public String getImageName() {
        if (imageName.equals("")) {
            return "root";
        }
        return imageName;
    }

    public QTreeNode[] getChildren() {
        return children;
    }

    @Override
    public int compareTo(QTreeNode o) {
        if (this.getUllat() < o.getUllat()) {
            return 1;
        }
        if (this.getUllat() > o.getUllat()) {
            return -1;
        } else {
            return 0;
        }
    }

}

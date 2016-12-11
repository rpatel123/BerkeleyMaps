public class QTreeNode implements Comparable<QTreeNode> {
    private double ullon;
    private double ullat;
    private double lrlon;
    private double lrlat;
    private String name;
    private QTreeNode[] children;
    int d;

    public QTreeNode(double ullon, double ullat, double lrlon, double lrlat, String imageName) {
        this.ullon = ullon;
        this.ullat = ullat;
        this.lrlon = lrlon;
        this.lrlat = lrlat;
        this.name = imageName;
        this.d = imageName.length();
        this.children = new QTreeNode[4];

    }

    public void makeChild(QTreeNode parent, int d) {
        if (d < 7) {
            double midLon = (parent.lrlon + parent.ullon) / 2;
            double midLat = (parent.lrlat + parent.ullat) / 2;

            QTreeNode firstChild = new QTreeNode(parent.ullon, parent.ullat, midLon,
                    midLat, parent.name + "1");
            QTreeNode secondChild = new QTreeNode(midLon, parent.ullat, parent.lrlon,
                    midLat, parent.name + "2");
            QTreeNode thirdChild = new QTreeNode(parent.ullon, midLat, midLon,
                    parent.lrlat, parent.name + "3");
            QTreeNode fourthChild = new QTreeNode(midLon, midLat, parent.lrlon,
                    parent.lrlat, parent.name + "4");


            children[0] = firstChild;
            children[1] = secondChild;
            children[2] = thirdChild;
            children[3] = fourthChild;

            firstChild.makeChild(firstChild, d + 1);
            secondChild.makeChild(secondChild, d + 1);
            thirdChild.makeChild(thirdChild, d + 1);
            fourthChild.makeChild(fourthChild, d + 1);
        } else {
            return;
        }

    }

    public QTreeNode[] getChildren() {
        return children;
    }


}

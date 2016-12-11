import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.Collections;
import java.util.Base64;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Graphics2D;

/* Maven is used to pull in these dependencies. */
import com.google.gson.Gson;

import javax.imageio.ImageIO;

import static spark.Spark.*;

/**
 * This MapServer class is the entry point for running the JavaSpark web server for the BearMaps
 * application project, receiving API calls, handling the API call processing, and generating
 * requested images and routes.
 * @author Alan Yao
 */
public class MapServer {
    /**
     * The root upper left/lower right longitudes and latitudes represent the bounding box of
     * the root tile, as the images in the img/ folder are scraped.
     * Longitude == x-axis; latitude == y-axis.
     */
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    /** Each tile is 256x256 pixels. */
    public static final int TILE_SIZE = 256;
    /** HTTP failed response. */
    private static final int HALT_RESPONSE = 403;
    /** Route stroke information: typically roads are not more than 5px wide. */
    public static final float ROUTE_STROKE_WIDTH_PX = 5.0f;
    /** Route stroke information: Cyan with half transparency. */
    public static final Color ROUTE_STROKE_COLOR = new Color(108, 181, 230, 200);
    /** The tile images are in the IMG_ROOT folder. */
    private static final String IMG_ROOT = "img/";
    /**
     * The OSM XML file path. Downloaded from <a href="http://download.bbbike.org/osm/">here</a>
     * using custom region selection.
     **/
    private static final String OSM_DB_PATH = "berkeley.osm";
    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside getMapRaster(). <br>
     * ullat -> upper left corner latitude,<br> ullon -> upper left corner longitude, <br>
     * lrlat -> lower right corner latitude,<br> lrlon -> lower right corner longitude <br>
     * w -> user viewport window width in pixels,<br> h -> user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
        "lrlon", "w", "h"};
    /**
     * Each route request to the server will have the following parameters
     * as keys in the params map.<br>
     * start_lat -> start point latitude,<br> start_lon -> start point longitude,<br>
     * end_lat -> end point latitude, <br>end_lon -> end point longitude.
     **/
    private static final String[] REQUIRED_ROUTE_REQUEST_PARAMS = {"start_lat", "start_lon",
        "end_lat", "end_lon"};
    /* Define any static variables here. Do not define any instance variables of MapServer. */
    private static GraphDB g;
    static QuadTree root;
    private static Graphics graphics;
    private static LinkedList<Long> shortestPath = new LinkedList<>();
    static HashSet<Node> visitedWithAStar;


    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {
        g = new GraphDB(OSM_DB_PATH);
        root = new QuadTree(ROOT_ULLON, ROOT_ULLAT, ROOT_LRLON, ROOT_LRLAT, "");
        visitedWithAStar = new HashSet<>();
    }

    public static void main(String[] args) {
        initialize();
        staticFileLocation("/page");
        /* Allow for all origin requests (since this is not an authenticated server, we do not
         * care about CSRF).  */
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });

        /* Define the raster endpoint for HTTP GET requests. I use anonymous functions to define
         * the request handlers. */
        get("/raster", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_RASTER_REQUEST_PARAMS);
            /* The png image is written to the ByteArrayOutputStream */
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            /* getMapRaster() does almost all the work for this API call */
            Map<String, Object> rasteredImgParams = getMapRaster(params, os);
            /* On an image query success, add the image data to the response */
            if (rasteredImgParams.containsKey("query_success")
                    && (Boolean) rasteredImgParams.get("query_success")) {
                String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
                rasteredImgParams.put("b64_encoded_image_data", encodedImage);
            }
            /* Encode response to Json */
            Gson gson = new Gson();
            return gson.toJson(rasteredImgParams);
        });

        /* Define the routing endpoint for HTTP GET requests. */
        get("/route", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_ROUTE_REQUEST_PARAMS);
            LinkedList<Long> route = findAndSetRoute(params);
            return !route.isEmpty();
        });

        /* Define the API endpoint for clearing the current route. */
        get("/clear_route", (req, res) -> {
            clearRoute();
            return true;
        });

        /* Define the API endpoint for search */
        get("/search", (req, res) -> {
            Set<String> reqParams = req.queryParams();
            String term = req.queryParams("term");
            Gson gson = new Gson();
            /* Search for actual location data. */
            if (reqParams.contains("full")) {
                List<Map<String, Object>> data = getLocations(term);
                return gson.toJson(data);
            } else {
                /* Search for prefix matching strings. */
                List<String> matches = getLocationsByPrefix(term);
                return gson.toJson(matches);
            }
        });

        /* Define map application redirect */
        get("/", (request, response) -> {
            response.redirect("/map.html", 301);
            return true;
        });
    }

    /**
     * Validate & return a parameter map of the required request parameters.
     * Requires that all input parameters are doubles.
     * @param req HTTP Request
     * @param requiredParams TestParams to validate
     * @return A populated map of input parameter to it's numerical value.
     */
    private static HashMap<String, Double> getRequestParams(
            spark.Request req, String[] requiredParams) {
        Set<String> reqParams = req.queryParams();
        HashMap<String, Double> params = new HashMap<>();
        for (String param : requiredParams) {
            if (!reqParams.contains(param)) {
                halt(HALT_RESPONSE, "Request failed - parameters missing.");
            } else {
                try {
                    params.put(param, Double.parseDouble(req.queryParams(param)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    halt(HALT_RESPONSE, "Incorrect parameters - provide numbers.");
                }
            }
        }
        return params;
    }


    /**
     * Handles raster API calls, queries for tiles and rasters the full image. <br>
     * <p>
     *     The rastered photo must have the following properties:
     *     <ul>
     *         <li>Has dimensions of at least w by h, where w and h are the user viewport width
     *         and height.</li>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *         <li>If a current route exists, lines of width ROUTE_STROKE_WIDTH_PX and of color
     *         ROUTE_STROKE_COLOR are drawn between all nodes on the route in the rastered photo.
     *         </li>
     *     </ul>
     *     Additional image about the raster is returned and is to be included in the Json response.
     * </p>
     * @param params Map of the HTTP GET request's query parameters - the query bounding box and
     *               the user viewport width and height.
     * @param os     An OutputStream that the resulting png image should be written to.
     * @return A map of parameters for the Json response as specified:
     * "raster_ul_lon" -> Double, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Double, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Double, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Double, the bounding lower right latitude of the rastered image <br>
     * "raster_width"  -> Double, the width of the rastered image <br>
     * "raster_height" -> Double, the height of the rastered image <br>
     * "depth"         -> Double, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image string. <br>
     * "query_success" -> Boolean, whether an image was successfully rastered. <br>
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public static Map<String, Object> getMapRaster(Map<String, Double> params, OutputStream os)
            throws IOException {
        HashMap<String, Object> rasteredImageParams = new HashMap<>();
        ArrayList<QTreeNode> correctNode = new ArrayList<>();
        QTreeNode temp = root.root;
        double queryDPP = (params.get("lrlon") - params.get("ullon")) / params.get("w");
        double nodeDPP;
        int depth = 0;
        while (temp != null && depth <= 7) {
            nodeDPP = (temp.getLrlon() - temp.getUllon()) / TILE_SIZE;
            if (nodeDPP <= queryDPP) {
                break;
            } else {
                temp = temp.getChildren()[0];
                depth++;
            }
        }
        root.helper(depth, params, correctNode);
        Collections.sort(correctNode);

        int w = (int) Math.round(Math.abs(correctNode.get(0).getUllon()
                - correctNode.get((correctNode.size() - 1)).getLrlon())
                / (correctNode.get(0).getLrlon() - correctNode.get(0).getUllon())) * 256;
        int h = (int) Math.round(Math.abs(correctNode.get(0).getUllat()
                - correctNode.get(correctNode.size() - 1).getLrlat())
                / Math.abs(correctNode.get(0).getUllat() - correctNode.get(0).getLrlat())) * 256;

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        graphics = result.getGraphics();
        int x = 0;
        int y = 0;
        for (QTreeNode node : correctNode) {
            String image = node.getImageName();
            BufferedImage bImage = ImageIO.read(new File(IMG_ROOT + image + ".png"));
            if (x + bImage.getWidth() > result.getWidth()) {
                x = 0;
                y += bImage.getHeight();
            }
            graphics.drawImage(bImage, x, y, null);
            x += bImage.getWidth();
        }

        Stroke stroke = new BasicStroke(MapServer.ROUTE_STROKE_WIDTH_PX,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        ((Graphics2D) graphics).setStroke(stroke);
        graphics.setColor(MapServer.ROUTE_STROKE_COLOR);

        double rasteredULLon = correctNode.get(0).getUllon();
        double rasteredULLat = correctNode.get(0).getUllat();
        double rasteredLRLon = correctNode.get(correctNode.size() - 1).getLrlon();
        double rasteredLRLat = correctNode.get(correctNode.size() - 1).getLrlat();
        double rasteredW = Math.abs(rasteredULLon - rasteredLRLon) / result.getWidth();
        double rasteredHeight = Math.abs(rasteredULLat - rasteredLRLat) / result.getHeight();

        for (int i = 0; i < shortestPath.size() - 1; i++) {
            Node start = g.graph.get(shortestPath.get(i).toString());
            Node end = g.graph.get(shortestPath.get(i + 1).toString());
            int startX = (int) ((Double.parseDouble(start.lon) - rasteredULLon) / rasteredW);
            int startY = (int) ((rasteredULLat - (Double.parseDouble(start.lat)))
                    / rasteredHeight);
            int endX = (int) ((Double.parseDouble(end.lon) - rasteredULLon) / rasteredW);
            int endY = (int) ((rasteredULLat - (Double.parseDouble(end.lat))) / rasteredHeight);

            graphics.drawLine(startX, startY, endX, endY);
        }

        ImageIO.write(result, "png", os);
        rasteredImageParams.put("raster_ul_lon", correctNode.get(0).getUllon());
        rasteredImageParams.put("raster_ul_lat", correctNode.get(0).getUllat());
        rasteredImageParams.put("raster_lr_lon",
                correctNode.get(correctNode.size() - 1).getLrlon());
        rasteredImageParams.put("raster_lr_lat",
                correctNode.get(correctNode.size() - 1).getLrlat());
        rasteredImageParams.put("raster_width", w);
        rasteredImageParams.put("raster_height", h);
        rasteredImageParams.put("depth", correctNode.get(0).getDepth());
        rasteredImageParams.put("query_success", true);
        return rasteredImageParams;

        /* Got some of the above from piazza and stack overflow */
    }

    /**
     * Searches for the shortest route satisfying the input request parameters, sets it to be the
     * current route, and returns a <code>LinkedList</code> of the route's node ids for testing
     * purposes. <br>
     * The route should start from the closest node to the start point and end at the closest node
     * to the endpoint. Distance is defined as the euclidean between two points (lon1, lat1) and
     * (lon2, lat2).
     * @param params from the API call described in REQUIRED_ROUTE_REQUEST_PARAMS
     * @return A LinkedList of node ids from the start of the route to the end.
     */
    public static LinkedList<Long> findAndSetRoute(Map<String,
            Double> params) {
        HashSet<SearchNode> hasBeenAdded = new HashSet<>();
        for (Node newNode : visitedWithAStar) {
            newNode.searchNode = null;
        }
        visitedWithAStar = new HashSet<>();
        double startLon = params.get("start_lon");
        double startLat = params.get("start_lat");
        double endLon = params.get("end_lon");
        double endLat = params.get("end_lat");
        double minStartDistance = 10000;
        double newMinStartDistance;
        double minEndDistance = 10000;
        double newMinEndDistance;
        Node minStartNode = null;
        Node minEndNode = null;
        clearRoute();

        for (Node n : g.graph.values()) {
            newMinStartDistance = Math.sqrt(Math.pow(startLon - Double.parseDouble(n.lon), 2)
                    + Math.pow(startLat - Double.parseDouble(n.lat), 2));
            newMinEndDistance = Math.sqrt(Math.pow(endLon - Double.parseDouble(n.lon), 2)
                    + Math.pow(endLat - Double.parseDouble(n.lat), 2));
            if (newMinStartDistance < minStartDistance) {
                minStartDistance = newMinStartDistance;
                minStartNode = n;
            }
            if (newMinEndDistance < minEndDistance) {
                minEndDistance = newMinEndDistance;
                minEndNode = n;
            }
        }
        SearchNode finalnode = null;
        SearchNode curr = new SearchNode(minStartNode, 0, true,
                euclDist(minStartNode, minEndNode), null);
        PriorityQueue<SearchNode> priorityQueue = new PriorityQueue<>();
        priorityQueue.add(curr);
        hasBeenAdded.add(curr);
        visitedWithAStar.add(curr.node);
        while (!curr.node.id.equals(minEndNode.id)) {
            curr = priorityQueue.remove();
            hasBeenAdded.remove(curr);
            for (Node n : curr.node.connection) {
                curr.marked = true;
                double euclDist1 = euclDist(curr.node, n);
                double euclDist2 = euclDist(n, minEndNode);
                if (n.searchNode != null) {
                    if (hasBeenAdded.contains(n.searchNode) && !n.searchNode.marked) {

                        double newPriority = curr.totalDistanceFromStart
                                + euclDist1 + euclDist2;
                        if (newPriority < n.searchNode.priority) {
                            priorityQueue.remove(n.searchNode);
                            hasBeenAdded.remove(n.searchNode);
                            SearchNode nodeToAdd = new SearchNode(n, curr.totalDistanceFromStart
                                    + euclDist1,
                                    true, euclDist2, curr);
                            priorityQueue.add(nodeToAdd);
                            hasBeenAdded.add(nodeToAdd);
                            visitedWithAStar.add(nodeToAdd.node);
                        }
                    }
                } else {
                    SearchNode newNode = new SearchNode(n, curr.totalDistanceFromStart
                            + euclDist1, false,
                            euclDist2, curr);
                    priorityQueue.add(newNode);
                    hasBeenAdded.add(newNode);
                    visitedWithAStar.add(newNode.node);
                }
            }
        }

        finalnode = curr;
        while (finalnode != null) {
            shortestPath.addFirst(Long.parseLong(finalnode.node.id));
            finalnode = finalnode.previous;
        }
        return shortestPath;
    }

    public static double euclDist(Node curr, Node end) {
        return Math.sqrt(Math.pow(Double.parseDouble(end.lon) - Double.parseDouble(curr.lon), 2)
                + Math.pow(Double.parseDouble(end.lat) - Double.parseDouble(curr.lat), 2));
    }

    /**
     * Clear the current found route, if it exists.
     */
    public static void clearRoute() {
        shortestPath = new LinkedList<>();
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public static List<String> getLocationsByPrefix(String prefix) {
        List<String> lowerCaseWords = g.trie.autoComplete(prefix);
        List<String> listToReturn = new ArrayList<>();
        for (String s : lowerCaseWords) {
            String toAdd = g.lowerToNormalCase.get(s);
            listToReturn.add(toAdd);
        }
        return listToReturn;
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public static List<Map<String, Object>> getLocations(String locationName) {
        Map<String, Object> newMap;
        HashSet<String> newID = new HashSet<>();
        List<String> iterate = getLocationsByPrefix(locationName);
        iterate.add(g.lowerToNormalCase.get(locationName));

        List<Map<String, Object>> locations = new ArrayList<>();

//        for (int i = 0; i < iterate.size(); i++) {
        for (String string : iterate) {
            ArrayList<String> iDNames = g.getID.get(string);
//            TrieNode n = g.trie.get(iterate.get(i));

            String checker = g.lowerToNormalCase.get(locationName);
            if (checker.equals(string)) {
//            System.out.println(n.id);
                for (String s: iDNames) {

                    if (!newID.contains(s)) {
                        newMap = new HashMap<>();
                        Node characteristics = g.stringToNode.get(s);
                        String lat = characteristics.lat;
                        String lon = characteristics.lon;
                        String id = characteristics.id;
                        String name = g.lowerToNormalCase.get(characteristics.name);
//                    System.out.println(name);
                        newMap.put("lat", Double.parseDouble(lat));
                        newMap.put("lon", Double.parseDouble(lon));
                        newMap.put("name", name);
                        newMap.put("id", Long.parseLong(id));
                        newID.add(id);
//                    System.out.println(newMap);
                        locations.add(newMap);
                    }

                }
            }

        }
        return locations;

    }
}

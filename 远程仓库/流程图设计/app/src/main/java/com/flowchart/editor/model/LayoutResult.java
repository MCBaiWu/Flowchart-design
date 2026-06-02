package com.flowchart.editor.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 布局结果模型
 */
public class LayoutResult {
    private Map<String, NodePosition> nodePositions;
    private Map<String, List<Point>> edgePaths;
    private int canvasWidth;
    private int canvasHeight;

    public LayoutResult() {
        this.nodePositions = new HashMap<String, NodePosition>();
        this.edgePaths = new HashMap<String, List<Point>>();
        this.canvasWidth = 0;
        this.canvasHeight = 0;
    }

    public Map<String, NodePosition> getNodePositions() { return nodePositions; }
    public void setNodePositions(Map<String, NodePosition> nodePositions) { this.nodePositions = nodePositions; }

    public Map<String, List<Point>> getEdgePaths() { return edgePaths; }
    public void setEdgePaths(Map<String, List<Point>> edgePaths) { this.edgePaths = edgePaths; }

    public int getCanvasWidth() { return canvasWidth; }
    public void setCanvasWidth(int canvasWidth) { this.canvasWidth = canvasWidth; }

    public int getCanvasHeight() { return canvasHeight; }
    public void setCanvasHeight(int canvasHeight) { this.canvasHeight = canvasHeight; }

    public void setNodePosition(String nodeId, int x, int y) {
        this.nodePositions.put(nodeId, new NodePosition(x, y));
    }

    public NodePosition getNodePosition(String nodeId) {
        return this.nodePositions.get(nodeId);
    }

    public void setEdgePath(String edgeId, List<Point> path) {
        this.edgePaths.put(edgeId, path);
    }

    public List<Point> getEdgePath(String edgeId) {
        return this.edgePaths.get(edgeId);
    }

    public static class NodePosition {
        public int x;
        public int y;

        public NodePosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static LayoutResult fromJSON(JSONObject json) {
        LayoutResult result = new LayoutResult();
        try {
            JSONObject positionsJson = json.optJSONObject("nodePositions");
            if (positionsJson != null) {
                JSONArray keys = positionsJson.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String nodeId = keys.getString(i);
                        JSONObject posJson = positionsJson.getJSONObject(nodeId);
                        int x = posJson.getInt("x");
                        int y = posJson.getInt("y");
                        result.setNodePosition(nodeId, x, y);
                    }
                }
            }

            JSONObject pathsJson = json.optJSONObject("edgePaths");
            if (pathsJson != null) {
                JSONArray keys = pathsJson.names();
                if (keys != null) {
                    for (int i = 0; i < keys.length(); i++) {
                        String edgeId = keys.getString(i);
                        JSONArray pathArray = pathsJson.getJSONArray(edgeId);
                        List<Point> path = new ArrayList<Point>();
                        for (int j = 0; j < pathArray.length(); j++) {
                            JSONObject pointJson = pathArray.getJSONObject(j);
                            int x = pointJson.getInt("x");
                            int y = pointJson.getInt("y");
                            path.add(new Point(x, y));
                        }
                        result.setEdgePath(edgeId, path);
                    }
                }
            }

            result.setCanvasWidth(json.optInt("canvasWidth", 0));
            result.setCanvasHeight(json.optInt("canvasHeight", 0));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}

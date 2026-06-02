package com.flowchart.editor.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 边模型
 */
public class Edge {
    private String id;
    private String sourceId;
    private String targetId;
    private String label;
    private List<Point> path;

    public Edge(String id, String sourceId, String targetId) {
        this.id = id;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.label = "";
        this.path = new ArrayList<Point>();
    }

    public Edge(String id, String sourceId, String targetId, String label) {
        this.id = id;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.label = label;
        this.path = new ArrayList<Point>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public List<Point> getPath() { return path; }
    public void setPath(List<Point> path) { this.path = path; }

    public void addPathPoint(Point point) { this.path.add(point); }
    public void addPathPoint(int x, int y) { this.path.add(new Point(x, y)); }
    public void clearPath() { this.path.clear(); }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("sourceId", sourceId);
            json.put("targetId", targetId);
            json.put("label", label);
            JSONArray pathArray = new JSONArray();
            for (int i = 0; i < path.size(); i++) {
                Point p = path.get(i);
                JSONObject pointJson = new JSONObject();
                pointJson.put("x", p.x);
                pointJson.put("y", p.y);
                pathArray.put(pointJson);
            }
            json.put("path", pathArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static Edge fromJSON(JSONObject json) {
        try {
            String id = json.getString("id");
            String sourceId = json.getString("sourceId");
            String targetId = json.getString("targetId");
            String label = json.optString("label", "");
            Edge edge = new Edge(id, sourceId, targetId, label);
            JSONArray pathArray = json.optJSONArray("path");
            if (pathArray != null) {
                for (int i = 0; i < pathArray.length(); i++) {
                    JSONObject pointJson = pathArray.getJSONObject(i);
                    int x = pointJson.getInt("x");
                    int y = pointJson.getInt("y");
                    edge.addPathPoint(x, y);
                }
            }
            return edge;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

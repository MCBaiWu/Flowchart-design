package com.flowchart.editor.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 节点模型
 */
public class Node {
    private String id;
    private String label;
    private int width;
    private int height;
    private int x;
    private int y;
    private String type;

    public Node(String id, String label, int width, int height) {
        this.id = id;
        this.label = label;
        this.width = width;
        this.height = height;
        this.x = 0;
        this.y = 0;
        this.type = "normal";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getCenterX() { return x + width / 2; }
    public int getCenterY() { return y + height / 2; }
    public int getRight() { return x + width; }
    public int getBottom() { return y + height; }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("label", label);
            json.put("width", width);
            json.put("height", height);
            json.put("x", x);
            json.put("y", y);
            json.put("type", type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static Node fromJSON(JSONObject json) {
        try {
            String id = json.getString("id");
            String label = json.optString("label", id);
            int width = json.optInt("width", 100);
            int height = json.optInt("height", 60);
            Node node = new Node(id, label, width, height);
            node.setX(json.optInt("x", 0));
            node.setY(json.optInt("y", 0));
            node.setType(json.optString("type", "normal"));
            return node;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

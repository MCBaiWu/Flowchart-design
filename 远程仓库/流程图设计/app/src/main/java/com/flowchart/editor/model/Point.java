package com.flowchart.editor.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 点坐标模型
 */
public class Point {
    public int x;
    public int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("x", x);
            json.put("y", y);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static Point fromJSON(JSONObject json) {
        try {
            int x = json.getInt("x");
            int y = json.getInt("y");
            return new Point(x, y);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

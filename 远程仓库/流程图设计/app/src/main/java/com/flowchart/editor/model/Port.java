package com.flowchart.editor.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 端口模型
 */
public class Port {
    public static final int POSITION_LEFT = 0;
    public static final int POSITION_TOP = 1;
    public static final int POSITION_RIGHT = 2;
    public static final int POSITION_BOTTOM = 3;

    private String id;
    private String nodeId;
    private String label;
    private int position;

    public Port(String id, String nodeId, int position) {
        this.id = id;
        this.nodeId = nodeId;
        this.position = position;
        this.label = "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNodeId() { return nodeId; }
    public void setNodeId(String nodeId) { this.nodeId = nodeId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public int getRelativeX(int nodeWidth) {
        switch (position) {
            case POSITION_LEFT: return 0;
            case POSITION_RIGHT: return nodeWidth;
            case POSITION_TOP:
            case POSITION_BOTTOM: return nodeWidth / 2;
            default: return 0;
        }
    }

    public int getRelativeY(int nodeHeight) {
        switch (position) {
            case POSITION_TOP: return 0;
            case POSITION_BOTTOM: return nodeHeight;
            case POSITION_LEFT:
            case POSITION_RIGHT: return nodeHeight / 2;
            default: return 0;
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("nodeId", nodeId);
            json.put("label", label);
            json.put("position", position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}

package com.flowchart.editor.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程图模型
 */
public class FlowChart {
    private String id;
    private String name;
    private List<Node> nodes;
    private List<Edge> edges;
    private List<Port> ports;
    private Map<String, Node> nodeMap;
    private Map<String, Edge> edgeMap;

    public FlowChart(String id, String name) {
        this.id = id;
        this.name = name;
        this.nodes = new ArrayList<Node>();
        this.edges = new ArrayList<Edge>();
        this.ports = new ArrayList<Port>();
        this.nodeMap = new HashMap<String, Node>();
        this.edgeMap = new HashMap<String, Edge>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Node> getNodes() { return nodes; }
    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
        this.nodeMap.clear();
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            this.nodeMap.put(node.getId(), node);
        }
    }

    public void addNode(Node node) {
        this.nodes.add(node);
        this.nodeMap.put(node.getId(), node);
    }

    public Node getNode(String nodeId) {
        return this.nodeMap.get(nodeId);
    }

    public List<Edge> getEdges() { return edges; }
    public void setEdges(List<Edge> edges) {
        this.edges = edges;
        this.edgeMap.clear();
        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);
            this.edgeMap.put(edge.getId(), edge);
        }
    }

    public void addEdge(Edge edge) {
        this.edges.add(edge);
        this.edgeMap.put(edge.getId(), edge);
    }

    public Edge getEdge(String edgeId) {
        return this.edgeMap.get(edgeId);
    }

    public List<Port> getPorts() { return ports; }
    public void setPorts(List<Port> ports) { this.ports = ports; }
    public void addPort(Port port) { this.ports.add(port); }

    public void clear() {
        nodes.clear();
        edges.clear();
        ports.clear();
        nodeMap.clear();
        edgeMap.clear();
    }

    public void applyLayoutResult(LayoutResult result) {
        for (Map.Entry<String, LayoutResult.NodePosition> entry : result.getNodePositions().entrySet()) {
            Node node = nodeMap.get(entry.getKey());
            if (node != null) {
                LayoutResult.NodePosition pos = entry.getValue();
                node.setX(pos.x);
                node.setY(pos.y);
            }
        }

        for (Map.Entry<String, List<Point>> entry : result.getEdgePaths().entrySet()) {
            Edge edge = edgeMap.get(entry.getKey());
            if (edge != null) {
                edge.setPath(entry.getValue());
            }
        }
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("name", name);
            JSONArray nodesArray = new JSONArray();
            for (int i = 0; i < nodes.size(); i++) {
                nodesArray.put(nodes.get(i).toJSON());
            }
            json.put("nodes", nodesArray);
            JSONArray edgesArray = new JSONArray();
            for (int i = 0; i < edges.size(); i++) {
                edgesArray.put(edges.get(i).toJSON());
            }
            json.put("edges", edgesArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static FlowChart fromJSON(JSONObject json) {
        try {
            String id = json.getString("id");
            String name = json.optString("name", "Untitled");
            FlowChart flowChart = new FlowChart(id, name);
            JSONArray nodesArray = json.optJSONArray("nodes");
            if (nodesArray != null) {
                for (int i = 0; i < nodesArray.length(); i++) {
                    Node node = Node.fromJSON(nodesArray.getJSONObject(i));
                    if (node != null) {
                        flowChart.addNode(node);
                    }
                }
            }
            JSONArray edgesArray = json.optJSONArray("edges");
            if (edgesArray != null) {
                for (int i = 0; i < edgesArray.length(); i++) {
                    Edge edge = Edge.fromJSON(edgesArray.getJSONObject(i));
                    if (edge != null) {
                        flowChart.addEdge(edge);
                    }
                }
            }
            return flowChart;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}

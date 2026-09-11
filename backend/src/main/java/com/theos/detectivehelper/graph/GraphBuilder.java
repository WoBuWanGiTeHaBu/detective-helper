package com.theos.detectivehelper.graph;

import java.util.*;

/**
 * 图构建器
 */
public class GraphBuilder {

    private Map<String, GraphNode> nodes = new HashMap<>();
    private Map<String, GraphEdge> edges = new HashMap<>();
    private Map<String, List<String>> adjacencyList = new HashMap<>();

    /**
     * 添加节点
     */
    public GraphBuilder addNode(String id, String label, String type, Map<String, Object> properties) {
        GraphNode node = new GraphNode(id, label, type, properties);
        nodes.put(id, node);
        adjacencyList.putIfAbsent(id, new ArrayList<>());
        return this;
    }

    /**
     * 添加边
     */
    public GraphBuilder addEdge(String id, String source, String target, String label, String type, Map<String, Object> properties) {
        GraphEdge edge = new GraphEdge(id, source, target, label, type, properties);
        edges.put(id, edge);

        adjacencyList.putIfAbsent(source, new ArrayList<>());
        adjacencyList.putIfAbsent(target, new ArrayList<>());
        adjacencyList.get(source).add(target);

        return this;
    }

    /**
     * 移除节点
     */
    public GraphBuilder removeNode(String id) {
        nodes.remove(id);

        // 移除相关的边
        edges.entrySet().removeIf(entry -> {
            GraphEdge edge = entry.getValue();
            return edge.getSource().equals(id) || edge.getTarget().equals(id);
        });

        // 更新邻接表
        adjacencyList.remove(id);
        adjacencyList.values().forEach(list -> list.remove(id));

        return this;
    }

    /**
     * 移除边
     */
    public GraphBuilder removeEdge(String id) {
        GraphEdge edge = edges.remove(id);
        if (edge != null) {
            adjacencyList.get(edge.getSource()).remove(edge.getTarget());
        }
        return this;
    }

    /**
     * 获取节点
     */
    public GraphNode getNode(String id) {
        return nodes.get(id);
    }

    /**
     * 获取边
     */
    public GraphEdge getEdge(String id) {
        return edges.get(id);
    }

    /**
     * 获取所有节点
     */
    public Collection<GraphNode> getNodes() {
        return nodes.values();
    }

    /**
     * 获取所有边
     */
    public Collection<GraphEdge> getEdges() {
        return edges.values();
    }

    /**
     * 获取节点的邻接节点
     */
    public List<String> getAdjacencyNodes(String id) {
        return adjacencyList.getOrDefault(id, new ArrayList<>());
    }

    /**
     * 检查节点是否存在
     */
    public boolean hasNode(String id) {
        return nodes.containsKey(id);
    }

    /**
     * 检查边是否存在
     */
    public boolean hasEdge(String id) {
        return edges.containsKey(id);
    }

    /**
     * 清空图
     */
    public void clear() {
        nodes.clear();
        edges.clear();
        adjacencyList.clear();
    }

    /**
     * 构建JSON格式的图数据
     */
    public Map<String, Object> buildJson() {
        Map<String, Object> graphData = new HashMap<>();

        List<Map<String, Object>> nodesData = new ArrayList<>();
        for (GraphNode node : nodes.values()) {
            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("id", node.getId());
            nodeData.put("label", node.getLabel());
            nodeData.put("type", node.getType());
            nodeData.put("properties", node.getProperties());
            nodesData.add(nodeData);
        }

        List<Map<String, Object>> edgesData = new ArrayList<>();
        for (GraphEdge edge : edges.values()) {
            Map<String, Object> edgeData = new HashMap<>();
            edgeData.put("id", edge.getId());
            edgeData.put("source", edge.getSource());
            edgeData.put("target", edge.getTarget());
            edgeData.put("label", edge.getLabel());
            edgeData.put("type", edge.getType());
            edgeData.put("properties", edge.getProperties());
            edgesData.add(edgeData);
        }

        graphData.put("nodes", nodesData);
        graphData.put("edges", edgesData);

        return graphData;
    }

    /**
     * 图节点类
     */
    public static class GraphNode {
        private String id;
        private String label;
        private String type;
        private Map<String, Object> properties;

        public GraphNode(String id, String label, String type, Map<String, Object> properties) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.properties = properties != null ? properties : new HashMap<>();
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    }

    /**
     * 图边类
     */
    public static class GraphEdge {
        private String id;
        private String source;
        private String target;
        private String label;
        private String type;
        private Map<String, Object> properties;

        public GraphEdge(String id, String source, String target, String label, String type, Map<String, Object> properties) {
            this.id = id;
            this.source = source;
            this.target = target;
            this.label = label;
            this.type = type;
            this.properties = properties != null ? properties : new HashMap<>();
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Map<String, Object> getProperties() { return properties; }
        public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    }

}
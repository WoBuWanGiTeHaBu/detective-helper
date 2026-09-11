package com.theos.detectivehelper.graph;

import java.util.*;

/**
 * 路径查找器
 */
public class PathFinder {

    private GraphBuilder graphBuilder;

    public PathFinder(GraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }

    /**
     * 广度优先搜索查找最短路径
     */
    public List<String> findShortestPath(String start, String end) {
        if (!graphBuilder.hasNode(start) || !graphBuilder.hasNode(end)) {
            return Collections.emptyList();
        }

        Map<String, String> parent = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);
        parent.put(start, null);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(end)) {
                return buildPath(parent, start, end);
            }

            for (String neighbor : graphBuilder.getAdjacencyNodes(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        return Collections.emptyList(); // 未找到路径
    }

    /**
     * 查找所有路径
     */
    public List<List<String>> findAllPaths(String start, String end) {
        List<List<String>> allPaths = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<String> currentPath = new ArrayList<>();

        findAllPathsDFS(start, end, visited, currentPath, allPaths);
        return allPaths;
    }

    /**
     * 查找最短路径（按权重）
     */
    public List<String> findWeightedShortestPath(String start, String end) {
        Map<String, Double> distance = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();

        // 初始化距离
        for (var node : graphBuilder.getNodes()) {
            distance.put(node.getId(), Double.MAX_VALUE);
        }
        distance.put(start, 0.0);

        for (int i = 0; i < graphBuilder.getNodes().size(); i++) {
            String current = getMinDistanceNode(distance, visited);
            if (current == null || current.equals(end)) {
                break;
            }

            visited.add(current);

            for (String neighbor : graphBuilder.getAdjacencyNodes(current)) {
                if (!visited.contains(neighbor)) {
                    double newDistance = distance.get(current) + 1.0; // 默认权重为1
                    if (newDistance < distance.get(neighbor)) {
                        distance.put(neighbor, newDistance);
                        parent.put(neighbor, current);
                    }
                }
            }
        }

        if (distance.get(end) == Double.MAX_VALUE) {
            return Collections.emptyList();
        }

        return buildPath(parent, start, end);
    }

    /**
     * 查找节点间的所有中间节点
     */
    public Set<String> findIntermediateNodes(String start, String end) {
        Set<String> intermediates = new HashSet<>();

        List<String> shortestPath = findShortestPath(start, end);
        if (shortestPath.size() > 2) {
            intermediates.addAll(shortestPath.subList(1, shortestPath.size() - 1));
        }

        // 查找其他可能的中间节点
        List<List<String>> allPaths = findAllPaths(start, end);
        for (List<String> path : allPaths) {
            if (path.size() > 2) {
                intermediates.addAll(path.subList(1, path.size() - 1));
            }
        }

        return intermediates;
    }

    private void findAllPathsDFS(String current, String end, Set<String> visited,
                                   List<String> currentPath, List<List<String>> allPaths) {
        visited.add(current);
        currentPath.add(current);

        if (current.equals(end)) {
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            for (String neighbor : graphBuilder.getAdjacencyNodes(current)) {
                if (!visited.contains(neighbor)) {
                    findAllPathsDFS(neighbor, end, visited, currentPath, allPaths);
                }
            }
        }

        currentPath.remove(currentPath.size() - 1);
        visited.remove(current);
    }

    private String getMinDistanceNode(Map<String, Double> distance, Set<String> visited) {
        String minNode = null;
        double minDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Double> entry : distance.entrySet()) {
            if (!visited.contains(entry.getKey()) && entry.getValue() < minDistance) {
                minDistance = entry.getValue();
                minNode = entry.getKey();
            }
        }

        return minNode;
    }

    private List<String> buildPath(Map<String, String> parent, String start, String end) {
        List<String> path = new ArrayList<>();
        String current = end;

        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }

        if (path.isEmpty() || !path.get(0).equals(start)) {
            return Collections.emptyList();
        }

        return path;
    }

}
package com.theos.detectivehelper.graph;

import java.util.*;
import java.util.regex.*;

/**
 * 关系提取器
 */
public class RelationExtractor {

    private static final Pattern PERSON_PATTERN = Pattern.compile(
        "[张王李赵刘陈杨黄周吴徐孙胡朱高林何郭马罗梁宋郑谢韩唐冯于董萧程曹袁邓许傅沈曾彭吕苏卢蒋蔡贾丁魏薛叶阎余潘杜戴夏钟汪田任姜范方石姚谭廖邹熊金陆郝孔白崔康毛邱秦江史顾侯邵孟龙万段雷钱汤尹黎葛庞]",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern TIME_PATTERN = Pattern.compile(
        "\\d{4}年\\d{1,2}月\\d{1,2}日|" +
        "\\d{4}/\\d{1,2}/\\d{1,2}|" +
        "\\d{1,2}月\\d{1,2}日|" +
        "星期[一二三四五六七]|" +
        "周[一二三四五六日]",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern LOCATION_PATTERN = Pattern.compile(
        "[北上广深]京|上海市|北京市|广州市|深圳市|" +
        "([省市区县])([\\u4e00-\\u9fa5]+?)\\1|" +
        "[\\u4e00-\\u9fa5]{2,4}(市|区|县|省)",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private static final Pattern RELATION_PATTERN = Pattern.compile(
        "(认识|熟悉|朋友|同事|同学|邻居|亲戚|家人|父子|母子|兄弟|姐妹|夫妻|恋人|仇恨|威胁|攻击|杀害|帮助|合作|交易)"
    );

    /**
     * 从文本中提取人物实体
     */
    public List<GraphBuilder.GraphNode> extractPersons(String text) {
        List<GraphBuilder.GraphNode> persons = new ArrayList<>();
        Set<String> extractedPersons = new HashSet<>();

        Matcher matcher = PERSON_PATTERN.matcher(text);
        while (matcher.find()) {
            String person = matcher.group();
            if (!extractedPersons.contains(person)) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("text_position", matcher.start());

                GraphBuilder.GraphNode node = new GraphBuilder.GraphNode(
                    generateNodeId("person", person),
                    person,
                    "person",
                    properties
                );

                persons.add(node);
                extractedPersons.add(person);
            }
        }

        return persons;
    }

    /**
     * 从文本中提取时间实体
     */
    public List<GraphBuilder.GraphNode> extractTimes(String text) {
        List<GraphBuilder.GraphNode> times = new ArrayList<>();
        Set<String> extractedTimes = new HashSet<>();

        Matcher matcher = TIME_PATTERN.matcher(text);
        while (matcher.find()) {
            String time = matcher.group();
            if (!extractedTimes.contains(time)) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("text_position", matcher.start());

                GraphBuilder.GraphNode node = new GraphBuilder.GraphNode(
                    generateNodeId("time", time),
                    time,
                    "time",
                    properties
                );

                times.add(node);
                extractedTimes.add(time);
            }
        }

        return times;
    }

    /**
     * 从文本中提取地点实体
     */
    public List<GraphBuilder.GraphNode> extractLocations(String text) {
        List<GraphBuilder.GraphNode> locations = new ArrayList<>();
        Set<String> extractedLocations = new HashSet<>();

        Matcher matcher = LOCATION_PATTERN.matcher(text);
        while (matcher.find()) {
            String location = matcher.group();
            if (!extractedLocations.contains(location)) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("text_position", matcher.start());

                GraphBuilder.GraphNode node = new GraphBuilder.GraphNode(
                    generateNodeId("location", location),
                    location,
                    "location",
                    properties
                );

                locations.add(node);
                extractedLocations.add(location);
            }
        }

        return locations;
    }

    /**
     * 从文本中提取关系
     */
    public List<GraphBuilder.GraphEdge> extractRelations(String text, List<GraphBuilder.GraphNode> nodes) {
        List<GraphBuilder.GraphEdge> relations = new ArrayList<>();
        Map<String, GraphBuilder.GraphNode> nodeMap = new HashMap<>();

        // 建立节点映射
        for (GraphBuilder.GraphNode node : nodes) {
            nodeMap.put(node.getLabel(), node);
        }

        Matcher matcher = RELATION_PATTERN.matcher(text);
        while (matcher.find()) {
            String relation = matcher.group();

            // 查找关系前后的实体
            int pos = matcher.start();
            String beforeText = text.substring(Math.max(0, pos - 50), pos);
            String afterText = text.substring(Math.min(text.length(), pos + 1), Math.min(text.length(), pos + 51));

            String sourceEntity = findNearestEntity(beforeText, nodeMap);
            String targetEntity = findNearestEntity(afterText, nodeMap);

            if (sourceEntity != null && targetEntity != null && !sourceEntity.equals(targetEntity)) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("text_position", matcher.start());
                properties.put("source_text", beforeText);
                properties.put("target_text", afterText);

                GraphBuilder.GraphEdge edge = new GraphBuilder.GraphEdge(
                    generateEdgeId(sourceEntity, targetEntity, relation),
                    nodeMap.get(sourceEntity).getId(),
                    nodeMap.get(targetEntity).getId(),
                    relation,
                    "relation",
                    properties
                );

                relations.add(edge);
            }
        }

        return relations;
    }

    /**
     * 从文本中提取所有实体和关系
     */
    public GraphBuilder extractAll(String text) {
        GraphBuilder graphBuilder = new GraphBuilder();

        List<GraphBuilder.GraphNode> persons = extractPersons(text);
        List<GraphBuilder.GraphNode> times = extractTimes(text);
        List<GraphBuilder.GraphNode> locations = extractLocations(text);

        List<GraphBuilder.GraphNode> allNodes = new ArrayList<>();
        allNodes.addAll(persons);
        allNodes.addAll(times);
        allNodes.addAll(locations);

        // 添加节点到图
        for (GraphBuilder.GraphNode node : allNodes) {
            graphBuilder.addNode(node.getId(), node.getLabel(), node.getType(), node.getProperties());
        }

        // 提取关系
        List<GraphBuilder.GraphEdge> relations = extractRelations(text, allNodes);
        for (GraphBuilder.GraphEdge edge : relations) {
            graphBuilder.addEdge(edge.getId(), edge.getSource(), edge.getTarget(),
                                edge.getLabel(), edge.getType(), edge.getProperties());
        }

        return graphBuilder;
    }

    private String generateNodeId(String type, String label) {
        return type + "_" + label.hashCode();
    }

    private String generateEdgeId(String source, String target, String relation) {
        return relation + "_" + source + "_" + target;
    }

    private String findNearestEntity(String text, Map<String, GraphBuilder.GraphNode> nodeMap) {
        String nearestEntity = null;
        int minDistance = Integer.MAX_VALUE;

        for (String entity : nodeMap.keySet()) {
            int index = text.lastIndexOf(entity);
            if (index >= 0 && index < minDistance) {
                minDistance = index;
                nearestEntity = entity;
            }
        }

        return nearestEntity;
    }

}
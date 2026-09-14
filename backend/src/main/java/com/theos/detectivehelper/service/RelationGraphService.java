package com.theos.detectivehelper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.Page;
import com.theos.detectivehelper.domain.RelationGraph;
import com.theos.detectivehelper.dto.CanvasResponse;
import com.theos.detectivehelper.dto.RelationGraphCreateDTO;
import com.theos.detectivehelper.dto.RelationGraphExtractDTO;
import com.theos.detectivehelper.dto.RelationGraphUpdateDTO;
import com.theos.detectivehelper.repository.BookRepository;
import com.theos.detectivehelper.repository.PageRepository;
import com.theos.detectivehelper.repository.RelationGraphRepository;
import com.theos.detectivehelper.util.JsonUtils;
import com.theos.detectivehelper.vo.RelationGraphDetailVO;
import com.theos.detectivehelper.vo.RelationGraphVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 关系图服务
 * <p>
 * 关系图挂在案件书（book）下，图数据以 data 字段整体存库（{"nodes":[],"edges":[]}）。
 */
@Service
@Transactional
public class RelationGraphService {

    private final RelationGraphRepository relationGraphRepository;
    private final BookRepository bookRepository;
    private final PageRepository pageRepository;
    private final ObjectMapper objectMapper;
    private final BookService bookService;

    public RelationGraphService(RelationGraphRepository relationGraphRepository,
                                BookRepository bookRepository,
                                PageRepository pageRepository,
                                ObjectMapper objectMapper,
                                BookService bookService) {
        this.relationGraphRepository = relationGraphRepository;
        this.bookRepository = bookRepository;
        this.pageRepository = pageRepository;
        this.objectMapper = objectMapper;
        this.bookService = bookService;
    }

    /**
     * 创建关系图
     */
    public RelationGraphVO createRelationGraph(Long bookId, RelationGraphCreateDTO dto) {
        if (!bookRepository.findById(bookId).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        RelationGraph graph = new RelationGraph();
        graph.setBookId(bookId);
        graph.setName(dto.getName());

        RelationGraph savedGraph = relationGraphRepository.save(graph);
        bookService.touchContent(bookId);
        return toVO(savedGraph);
    }

    /**
     * 更新关系图元信息（仅名称）
     */
    public RelationGraphVO updateRelationGraph(Long id, RelationGraphUpdateDTO dto) {
        RelationGraph graph = relationGraphRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RELATION_GRAPH_NOT_FOUND));

        graph.setName(dto.getName());

        RelationGraph savedGraph = relationGraphRepository.save(graph);
        bookService.touchContent(graph.getBookId());
        return toVO(savedGraph);
    }

    /**
     * 删除关系图
     */
    public void deleteRelationGraph(Long id) {
        RelationGraph graph = relationGraphRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RELATION_GRAPH_NOT_FOUND));
        relationGraphRepository.deleteById(id);
        bookService.touchContent(graph.getBookId());
    }

    /**
     * 获取关系图详情
     */
    public RelationGraphDetailVO getRelationGraphById(Long id) {
        RelationGraph graph = relationGraphRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RELATION_GRAPH_NOT_FOUND));

        return toDetailVO(graph);
    }

    /**
     * 获取案件书下的所有关系图
     */
    public List<RelationGraphVO> getRelationGraphsByBookId(Long bookId) {
        if (!bookRepository.findById(bookId).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        return relationGraphRepository.findByBookId(bookId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 保存关系图数据（覆盖写入 data 字段）
     */
    public void saveRelationGraphData(Long id, String dataJson) {
        RelationGraph graph = relationGraphRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RELATION_GRAPH_NOT_FOUND));

        if (dataJson != null && !JsonUtils.isValidJson(dataJson)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "关系图数据不是合法 JSON");
        }

        graph.setData(dataJson);
        relationGraphRepository.save(graph);
        bookService.touchContent(graph.getBookId());
    }

    /**
     * 从画布提取关系图数据
     * <p>
     * 遍历案件书下所有页面的画布，把画布对象<b>原样映射</b>为节点、画布关系<b>原样映射</b>为边：
     * <ul>
     *   <li>{@code GraphNode.id} 就是画布对象的 id，不另起编号，前端可回指画布；</li>
     *   <li>节点的 type 直接透传画布对象的 type（person / event / thing），前端据此做「人物↔人物」过滤；</li>
     *   <li>edges 数量与画布 relationships 一一对应，不按端点丢弃——同一对对象允许多条关系，
     *       即使 objects 缺失端点（半成品数据）也保留边，保证「提取出的边 = 画布上的关系」；</li>
     *   <li>{@code objectTypes} / {@code relationTypes} 为空（或缺省）表示不过滤；
     *       传了则分别只保留指定类型的节点 / 边（此时边数会少于关系数，属预期）；</li>
     *   <li>书下无页面、画布为空或单页 JSON 损坏时返回 {@code nodes: [], edges: []}，不抛错。</li>
     * </ul>
     */
    public Map<String, Object> extractRelationGraph(Long bookId, RelationGraphExtractDTO dto) {
        if (!bookRepository.findById(bookId).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        Set<String> objectTypes = dto.getObjectTypes() == null ? Set.of() : new HashSet<>(dto.getObjectTypes());
        Set<String> relationTypes = dto.getRelationTypes() == null ? Set.of() : new HashSet<>(dto.getRelationTypes());

        Map<String, CanvasResponse.CanvasObject> objectsById = new LinkedHashMap<>();
        List<CanvasResponse.Relationship> relationships = new ArrayList<>();

        for (Page page : pageRepository.findByBookId(bookId)) {
            CanvasResponse canvas = decodeCanvas(page.getCanvasData());
            if (canvas.getObjects() != null) {
                for (CanvasResponse.CanvasObject object : canvas.getObjects()) {
                    if (object.getId() == null) {
                        continue;
                    }
                    if (objectTypes.isEmpty() || (object.getType() != null && objectTypes.contains(object.getType()))) {
                        objectsById.putIfAbsent(object.getId(), object);
                    }
                }
            }
            if (canvas.getRelationships() != null) {
                relationships.addAll(canvas.getRelationships());
            }
        }

        List<Map<String, Object>> nodes = objectsById.values().stream()
                .map(object -> {
                    Map<String, Object> node = new LinkedHashMap<String, Object>();
                    node.put("id", object.getId());
                    node.put("name", object.getName() != null ? object.getName() : object.getId());
                    node.put("type", object.getType());
                    return node;
                })
                .collect(Collectors.toList());

        Set<String> seenEdgeIds = new HashSet<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        for (CanvasResponse.Relationship relationship : relationships) {
            if (relationship.getId() == null || !seenEdgeIds.add(relationship.getId())) {
                continue;
            }
            if (!relationTypes.isEmpty()
                    && (relationship.getType() == null || !relationTypes.contains(relationship.getType()))) {
                continue;
            }
            Map<String, Object> edge = new LinkedHashMap<String, Object>();
            edge.put("id", relationship.getId());
            edge.put("source", relationship.getSource());
            edge.put("target", relationship.getTarget());
            edge.put("label", relationship.getLabel());
            edge.put("type", relationship.getType());
            edges.add(edge);
        }

        // 用户从画布提取出图，视为一次内容改动（与需求清单一致）
        bookService.touchContent(bookId);
        return Map.of("nodes", nodes, "edges", edges);
    }

    /** 画布 JSON 损坏或为空时按空画布处理，不让单页坏数据拖垮整次提取 */
    private CanvasResponse decodeCanvas(String canvasData) {
        if (canvasData == null || canvasData.isBlank()) {
            return new CanvasResponse();
        }
        try {
            return objectMapper.readValue(canvasData, CanvasResponse.class);
        } catch (Exception e) {
            return new CanvasResponse();
        }
    }

    private RelationGraphVO toVO(RelationGraph graph) {
        return new RelationGraphVO(
                graph.getId(),
                graph.getBookId(),
                graph.getName(),
                graph.getData(),
                graph.getCreatedAt(),
                graph.getUpdatedAt()
        );
    }

    private RelationGraphDetailVO toDetailVO(RelationGraph graph) {
        RelationGraphDetailVO detailVO = new RelationGraphDetailVO();
        detailVO.setId(graph.getId());
        detailVO.setBookId(graph.getBookId());
        detailVO.setName(graph.getName());
        detailVO.setData(graph.getData());
        detailVO.setCreatedAt(graph.getCreatedAt());
        detailVO.setUpdatedAt(graph.getUpdatedAt());

        // data 里的 nodes / edges 在响应中还原为 JSON 数组，而不是转义后的字符串
        detailVO.setNodes(parseJsonArray(graph.getData(), "nodes"));
        detailVO.setEdges(parseJsonArray(graph.getData(), "edges"));

        return detailVO;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String dataJson, String field) {
        if (dataJson == null || dataJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> data = JsonUtils.fromJson(dataJson, Map.class);
            Object value = data.get(field);
            if (value instanceof List) {
                return (List<Map<String, Object>>) value;
            }
        } catch (Exception ignored) {
            // 数据损坏时按空图处理，不影响元信息返回
        }
        return new ArrayList<>();
    }

}

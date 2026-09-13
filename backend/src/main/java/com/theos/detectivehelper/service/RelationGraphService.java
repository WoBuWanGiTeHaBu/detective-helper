package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.RelationGraph;
import com.theos.detectivehelper.dto.RelationGraphCreateDTO;
import com.theos.detectivehelper.dto.RelationGraphExtractDTO;
import com.theos.detectivehelper.dto.RelationGraphUpdateDTO;
import com.theos.detectivehelper.repository.BookRepository;
import com.theos.detectivehelper.repository.RelationGraphRepository;
import com.theos.detectivehelper.util.JsonUtils;
import com.theos.detectivehelper.vo.RelationGraphDetailVO;
import com.theos.detectivehelper.vo.RelationGraphVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public RelationGraphService(RelationGraphRepository relationGraphRepository, BookRepository bookRepository) {
        this.relationGraphRepository = relationGraphRepository;
        this.bookRepository = bookRepository;
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
        return toVO(savedGraph);
    }

    /**
     * 删除关系图
     */
    public void deleteRelationGraph(Long id) {
        if (!relationGraphRepository.findById(id).isPresent()) {
            throw new BusinessException(ErrorCode.RELATION_GRAPH_NOT_FOUND);
        }
        relationGraphRepository.deleteById(id);
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
    }

    /**
     * 从画布提取关系图数据
     * <p>
     * 目前返回占位数据，后续接入 RelationExtractor 的提取逻辑。
     */
    public Map<String, Object> extractRelationGraph(Long bookId, RelationGraphExtractDTO dto) {
        if (!bookRepository.findById(bookId).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        // TODO 接入 RelationExtractor：按 dto.getObjectTypes() / dto.getRelationTypes() 过滤画布对象与关系
        return Map.of("nodes", List.of(), "edges", List.of());
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

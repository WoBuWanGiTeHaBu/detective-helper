package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.RelationGraph;
import com.theos.detectivehelper.dto.RelationGraphCreateDTO;
import com.theos.detectivehelper.dto.RelationGraphExtractDTO;
import com.theos.detectivehelper.dto.RelationGraphUpdateDTO;
import com.theos.detectivehelper.graph.GraphBuilder;
import com.theos.detectivehelper.graph.RelationExtractor;
import com.theos.detectivehelper.repository.BookRepository;
import com.theos.detectivehelper.repository.RelationGraphRepository;
import com.theos.detectivehelper.util.JsonUtils;
import com.theos.detectivehelper.vo.RelationGraphDetailVO;
import com.theos.detectivehelper.vo.RelationGraphVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关系图服务
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
    public RelationGraphVO createRelationGraph(RelationGraphCreateDTO dto) {
        if (!bookRepository.findById(dto.getBookId()).isPresent()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.BOOK_NOT_FOUND);
        }

        RelationGraph graph = new RelationGraph();
        graph.setBookId(dto.getBookId());
        graph.setTitle(dto.getTitle());
        graph.setDescription(dto.getDescription());
        graph.setGraphData(dto.getGraphData());

        RelationGraph savedGraph = relationGraphRepository.save(graph);
        return toVO(savedGraph);
    }

    /**
     * 更新关系图
     */
    public RelationGraphVO updateRelationGraph(Long id, RelationGraphUpdateDTO dto) {
        RelationGraph graph = relationGraphRepository.findById(id)
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.RELATION_GRAPH_NOT_FOUND));

        graph.setTitle(dto.getTitle());
        graph.setDescription(dto.getDescription());
        graph.setGraphData(dto.getGraphData());

        RelationGraph savedGraph = relationGraphRepository.save(graph);
        return toVO(savedGraph);
    }

    /**
     * 删除关系图
     */
    public void deleteRelationGraph(Long id) {
        if (!relationGraphRepository.findById(id).isPresent()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.RELATION_GRAPH_NOT_FOUND);
        }
        relationGraphRepository.deleteById(id);
    }

    /**
     * 获取关系图详情
     */
    public RelationGraphDetailVO getRelationGraphById(Long id) {
        RelationGraph graph = relationGraphRepository.findById(id)
                .orElseThrow(() -> new BusinessException(com.theos.detectivehelper.common.ErrorCode.RELATION_GRAPH_NOT_FOUND));

        return toDetailVO(graph);
    }

    /**
     * 获取案件书的所有关系图
     */
    public List<RelationGraphVO> getRelationGraphsByBookId(Long bookId) {
        return relationGraphRepository.findByBookId(bookId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 从案件中提取关系图
     */
    public RelationGraphVO extractRelationGraph(RelationGraphExtractDTO dto) {
        if (!bookRepository.findById(dto.getBookId()).isPresent()) {
            throw new BusinessException(com.theos.detectivehelper.common.ErrorCode.BOOK_NOT_FOUND);
        }

        // 这里需要调用关系提取器
        // 暂时返回空图
        RelationGraph graph = new RelationGraph();
        graph.setBookId(dto.getBookId());
        graph.setTitle("自动生成的关系图");
        graph.setDescription("从案件内容中提取的关系图");
        graph.setGraphData("{\"nodes\":[],\"edges\":[]}");

        RelationGraph savedGraph = relationGraphRepository.save(graph);
        return toVO(savedGraph);
    }

    private RelationGraphVO toVO(RelationGraph graph) {
        return new RelationGraphVO(
                graph.getId(),
                graph.getBookId(),
                graph.getTitle(),
                graph.getDescription(),
                graph.getGraphData(),
                graph.getCreatedAt(),
                graph.getUpdatedAt()
        );
    }

    private RelationGraphDetailVO toDetailVO(RelationGraph graph) {
        RelationGraphDetailVO detailVO = new RelationGraphDetailVO();
        detailVO.setId(graph.getId());
        detailVO.setBookId(graph.getBookId());
        detailVO.setTitle(graph.getTitle());
        detailVO.setDescription(graph.getDescription());
        detailVO.setGraphData(graph.getGraphData());
        detailVO.setCreatedAt(graph.getCreatedAt());
        detailVO.setUpdatedAt(graph.getUpdatedAt());

        // 解析图数据
        if (graph.getGraphData() != null && !graph.getGraphData().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> graphData = JsonUtils.fromJson(graph.getGraphData(), Map.class);

                Object nodes = graphData.get("nodes");
                Object edges = graphData.get("edges");

                if (nodes instanceof List) {
                    detailVO.setNodeCount(((List<?>) nodes).size());
                }

                if (edges instanceof List) {
                    detailVO.setEdgeCount(((List<?>) edges).size());
                }

            } catch (Exception e) {
                detailVO.setNodeCount(0);
                detailVO.setEdgeCount(0);
            }
        } else {
            detailVO.setNodeCount(0);
            detailVO.setEdgeCount(0);
        }

        detailVO.setEntityTypes(List.of("人物", "物品", "事件"));
        detailVO.setRelationTypes(List.of("认识", "使用", "参与"));

        return detailVO;
    }

}
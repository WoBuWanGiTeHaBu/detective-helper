package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.domain.FamilyTree;
import com.theos.detectivehelper.dto.FamilyTreeCreateDTO;
import com.theos.detectivehelper.dto.FamilyTreeUpdateDTO;
import com.theos.detectivehelper.repository.BookRepository;
import com.theos.detectivehelper.repository.FamilyTreeRepository;
import com.theos.detectivehelper.util.JsonUtils;
import com.theos.detectivehelper.vo.FamilyTreeDetailVO;
import com.theos.detectivehelper.vo.FamilyTreeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 族谱图服务
 * <p>
 * 族谱图挂在案件书（book）下，图数据以 data 字段整体存库（{"members":[],"relations":[]}）。
 * 与关系图（RelationGraphService）完全同构：整体覆盖保存、以 id 为准、不去重不过滤未知字段。
 */
@Service
@Transactional
public class FamilyTreeService {

    private final FamilyTreeRepository familyTreeRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;

    public FamilyTreeService(FamilyTreeRepository familyTreeRepository,
                             BookRepository bookRepository,
                             BookService bookService) {
        this.familyTreeRepository = familyTreeRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
    }

    /**
     * 创建族谱图
     * <p>
     * 新建时 data 必须初始化为 {@code {"members":[],"relations":[]}}，不能是 null——
     * 否则前端重开时会拿到空图。
     */
    public FamilyTreeVO createFamilyTree(Long bookId, FamilyTreeCreateDTO dto) {
        if (!bookRepository.findById(bookId).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        FamilyTree tree = new FamilyTree();
        tree.setBookId(bookId);
        tree.setName(dto.getName());

        FamilyTree savedTree = familyTreeRepository.save(tree);
        bookService.touchContent(bookId);
        return toVO(savedTree);
    }

    /**
     * 更新族谱图元信息（仅名称）
     */
    public FamilyTreeVO updateFamilyTree(Long id, FamilyTreeUpdateDTO dto) {
        FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_TREE_NOT_FOUND));

        tree.setName(dto.getName());

        FamilyTree savedTree = familyTreeRepository.save(tree);
        bookService.touchContent(tree.getBookId());
        return toVO(savedTree);
    }

    /**
     * 删除族谱图
     */
    public void deleteFamilyTree(Long id) {
        FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_TREE_NOT_FOUND));
        familyTreeRepository.deleteById(id);
        bookService.touchContent(tree.getBookId());
    }

    /**
     * 获取族谱图详情
     */
    public FamilyTreeDetailVO getFamilyTreeById(Long id) {
        FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_TREE_NOT_FOUND));

        return toDetailVO(tree);
    }

    /**
     * 获取案件书下的所有族谱图
     */
    public List<FamilyTreeVO> getFamilyTreesByBookId(Long bookId) {
        if (!bookRepository.findById(bookId).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }
        return familyTreeRepository.findByBookId(bookId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 保存族谱图数据（覆盖写入 data 字段）
     * <p>
     * 请求体是原始 JSON 字符串，整体覆盖、以 id 为准，不去重、不过滤未知字段、不解析日期。
     */
    public void saveFamilyTreeData(Long id, String dataJson) {
        FamilyTree tree = familyTreeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAMILY_TREE_NOT_FOUND));

        if (dataJson != null && !JsonUtils.isValidJson(dataJson)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "族谱图数据不是合法 JSON");
        }

        tree.setData(dataJson);
        familyTreeRepository.save(tree);
        bookService.touchContent(tree.getBookId());
    }

    private FamilyTreeVO toVO(FamilyTree tree) {
        return new FamilyTreeVO(
                tree.getId(),
                tree.getBookId(),
                tree.getName(),
                tree.getData(),
                tree.getCreatedAt(),
                tree.getUpdatedAt()
        );
    }

    private FamilyTreeDetailVO toDetailVO(FamilyTree tree) {
        FamilyTreeDetailVO detailVO = new FamilyTreeDetailVO();
        detailVO.setId(tree.getId());
        detailVO.setBookId(tree.getBookId());
        detailVO.setName(tree.getName());
        detailVO.setData(tree.getData());
        detailVO.setCreatedAt(tree.getCreatedAt());
        detailVO.setUpdatedAt(tree.getUpdatedAt());

        // data 里的 members / relations 在响应中还原为 JSON 数组，而不是转义后的字符串
        detailVO.setMembers(parseJsonArray(tree.getData(), "members"));
        detailVO.setRelations(parseJsonArray(tree.getData(), "relations"));

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

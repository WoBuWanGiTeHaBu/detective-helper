package com.theos.detectivehelper.service;

import com.theos.detectivehelper.common.ErrorCode;
import com.theos.detectivehelper.common.exception.BusinessException;
import com.theos.detectivehelper.config.StorageProperties;
import com.theos.detectivehelper.domain.Book;
import com.theos.detectivehelper.domain.Event;
import com.theos.detectivehelper.domain.Page;
import com.theos.detectivehelper.domain.RelationGraph;
import com.theos.detectivehelper.dto.BookCreateDTO;
import com.theos.detectivehelper.dto.BookSortDTO;
import com.theos.detectivehelper.dto.BookUpdateDTO;
import com.theos.detectivehelper.repository.BookRepository;
import com.theos.detectivehelper.repository.EventRepository;
import com.theos.detectivehelper.repository.FamilyTreeRepository;
import com.theos.detectivehelper.repository.PageRepository;
import com.theos.detectivehelper.repository.RelationGraphRepository;
import com.theos.detectivehelper.vo.BookVO;
import com.theos.detectivehelper.vo.BookWorkspaceVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 案件书服务
 */
@Service
@Transactional
public class BookService {

    /** 封面大小上限：2MB（与 OpenAPI 文档一致） */
    private static final long MAX_COVER_SIZE = 2 * 1024 * 1024;

    /** 允许的封面格式：Content-Type → 文件扩展名 */
    private static final Map<String, String> ALLOWED_COVER_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    /** 扩展名白名单（按上传文件名兜底校验用） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final BookRepository bookRepository;
    private final EventRepository eventRepository;
    private final PageRepository pageRepository;
    private final RelationGraphRepository relationGraphRepository;
    private final FamilyTreeRepository familyTreeRepository;
    private final StorageProperties storage;

    public BookService(BookRepository bookRepository,
                       EventRepository eventRepository,
                       PageRepository pageRepository,
                       RelationGraphRepository relationGraphRepository,
                       FamilyTreeRepository familyTreeRepository,
                       StorageProperties storage) {
        this.bookRepository = bookRepository;
        this.eventRepository = eventRepository;
        this.pageRepository = pageRepository;
        this.relationGraphRepository = relationGraphRepository;
        this.familyTreeRepository = familyTreeRepository;
        this.storage = storage;
    }

    /** 封面目录（启动时已创建）。路径一律从配置取，不写相对路径 */
    private Path coversDir() {
        return storage.coversDir();
    }

    /**
     * 创建案件书
     */
    public BookVO createBook(BookCreateDTO dto) {
        Book book = new Book();
        book.setName(dto.getName());
        book.setCoverType(dto.getCoverType() != null ? dto.getCoverType() : "color");
        book.setCoverValue(dto.getCoverValue());
        book.setCoverText(dto.getCoverText());
        book.setSortOrder(getNextSortOrder());

        Book savedBook = bookRepository.save(book);
        return toVO(savedBook);
    }

    /**
     * 更新案件书
     */
    public BookVO updateBook(Long id, BookUpdateDTO dto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        // PATCH 语义：null = 不修改。封面上传后的补丁请求只带 cover 字段，不能把书名清掉；
        // 工作区只改名时也不能把图片封面抹掉。name 显式传了但为空白才算参数错误。
        if (dto.getName() != null) {
            if (dto.getName().isBlank()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "案件书名称不能为空");
            }
            book.setName(dto.getName().trim());
        }
        if (dto.getCoverType() != null) {
            book.setCoverType(dto.getCoverType());
        }
        if (dto.getCoverValue() != null) {
            book.setCoverValue(dto.getCoverValue());
        }
        if (dto.getCoverText() != null) {
            book.setCoverText(dto.getCoverText());
        }

        Book savedBook = bookRepository.save(book);
        return toVO(savedBook);
    }

    /**
     * 删除案件书（级联删除事件、页面、关系图、族谱图）
     */
    public void deleteBook(Long id) {
        if (!bookRepository.findById(id).isPresent()) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND);
        }

        for (Event event : eventRepository.findByBookId(id)) {
            pageRepository.deleteByEventId(event.getId());
        }
        eventRepository.deleteByBookId(id);
        relationGraphRepository.deleteByBookId(id);
        familyTreeRepository.deleteByBookId(id);
        bookRepository.deleteById(id);
    }

    /**
     * 获取案件书详情
     */
    public BookVO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));
        return toVO(book);
    }

    /**
     * 上传书籍封面图片（JPEG/PNG/WebP，≤2MB）
     * <p>
     * 文件存到 {@code data/covers/}，命名 {@code book_{id}_{时间戳}.{ext}}（带时间戳避免
     * 重传同名文件被浏览器缓存），书籍的 coverType 置为 image、coverValue 置为
     * {@code covers/xxx}（相对路径，前端经 /files/covers/** 静态映射访问）。
     * 重复上传时删除旧文件，避免 covers 目录无限增长。
     *
     * @return 形如 {@code {"coverValue":"covers/book_1_1726...webp"}}
     */
    public Map<String, String> uploadCover(Long id, MultipartFile file) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.getCode(), "封面文件不能为空");
        }
        if (file.getSize() > MAX_COVER_SIZE) {
            throw new BusinessException(ErrorCode.COVER_TOO_LARGE);
        }

        String extension = resolveCoverExtension(file);
        if (extension == null) {
            throw new BusinessException(ErrorCode.COVER_FORMAT_NOT_SUPPORTED);
        }

        String oldValue = book.getCoverValue();
        String filename = "book_" + id + "_" + System.currentTimeMillis() + "." + extension;
        String coverValue = "covers/" + filename;
        Path target = coversDir().resolve(filename);

        try {
            Files.createDirectories(coversDir());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR.getCode(), "封面保存失败：" + e.getMessage());
        }

        book.setCoverType("image");
        book.setCoverValue(coverValue);
        bookRepository.save(book);

        // 新文件落盘且书籍更新成功后再清理旧封面，失败不影响本次上传
        if (oldValue != null && oldValue.startsWith("covers/")) {
            try {
                Files.deleteIfExists(coversDir().resolve(oldValue.substring("covers/".length())));
            } catch (IOException ignored) {
            }
        }

        return Map.of("coverValue", coverValue);
    }

    /**
     * 从 Content-Type 优先、文件扩展名兜底确定封面扩展名；不在白名单内返回 null
     */
    private String resolveCoverExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            String ext = ALLOWED_COVER_TYPES.get(contentType.toLowerCase(Locale.ROOT));
            if (ext != null) {
                return ext;
            }
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0 && dot < name.length() - 1) {
                String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ALLOWED_EXTENSIONS.contains(ext)) {
                    return "jpeg".equals(ext) ? "jpg" : ext;
                }
            }
        }
        return null;
    }

    /**
     * 获取所有案件书
     */
    public List<BookVO> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取案件书工作空间：案件书 + 事件（含页面）+ 关系图
     */
    public BookWorkspaceVO getBookWorkspace(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND));

        List<BookWorkspaceVO.WorkspaceEventVO> events = new ArrayList<>();
        for (Event event : eventRepository.findByBookId(id)) {
            List<BookWorkspaceVO.WorkspacePageVO> pages = pageRepository.findByEventId(event.getId()).stream()
                    .map(this::toWorkspacePage)
                    .collect(Collectors.toList());

            events.add(new BookWorkspaceVO.WorkspaceEventVO(
                    event.getId(),
                    event.getName(),
                    event.getSortOrder(),
                    pages
            ));
        }

        List<BookWorkspaceVO.WorkspaceRelationGraphVO> relationGraphs = relationGraphRepository.findByBookId(id).stream()
                .map(this::toWorkspaceRelationGraph)
                .collect(Collectors.toList());

        return new BookWorkspaceVO(toVO(book), events, relationGraphs);
    }

    /**
     * 批量排序案件书
     */
    public void sortBooks(BookSortDTO dto) {
        List<Long> bookIds = dto.getBookIds();
        if (bookIds == null || bookIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_SORT_ORDER);
        }

        for (int i = 0; i < bookIds.size(); i++) {
            bookRepository.updateSortOrder(bookIds.get(i), i + 1);
        }
    }

    private int getNextSortOrder() {
        List<Book> books = bookRepository.findAll();
        return books.stream()
                .mapToInt(book -> book.getSortOrder() != null ? book.getSortOrder() : 0)
                .max()
                .orElse(0) + 1;
    }

    private BookWorkspaceVO.WorkspacePageVO toWorkspacePage(Page page) {
        return new BookWorkspaceVO.WorkspacePageVO(page.getId(), page.getName(), page.getSortOrder());
    }

    private BookWorkspaceVO.WorkspaceRelationGraphVO toWorkspaceRelationGraph(RelationGraph graph) {
        return new BookWorkspaceVO.WorkspaceRelationGraphVO(graph.getId(), graph.getName());
    }

    private BookVO toVO(Book book) {
        int eventCount = bookRepository.countEventsByBookId(book.getId());
        int pageCount = bookRepository.countPagesByBookId(book.getId());

        BookVO vo = new BookVO(
                book.getId(),
                book.getName(),
                book.getCoverType(),
                book.getCoverValue(),
                book.getCoverText(),
                book.getSortOrder(),
                eventCount,
                pageCount,
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
        vo.setContentUpdatedAt(book.getContentUpdatedAt());
        return vo;
    }

    /**
     * 刷新案件书的内容改动时间（contentUpdatedAt）
     * <p>
     * 由事件 / 页面 / 画布 / 关系图 / 族谱图的写接口在成功落库后调用；
     * 改书名、换封面、书架排序<b>不</b>调用（它们只影响 updatedAt）。
     */
    public void touchContent(Long bookId) {
        bookRepository.touchContent(bookId);
    }

}

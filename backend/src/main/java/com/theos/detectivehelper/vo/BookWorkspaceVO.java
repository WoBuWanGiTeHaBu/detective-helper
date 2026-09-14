package com.theos.detectivehelper.vo;

import lombok.Data;

import java.util.List;

/**
 * 案件书工作空间VO
 * <p>
 * 对应 GET /api/books/{id}/workspace 的响应结构：
 * book 为案件书元信息，events 内嵌各自的页面列表，relationGraphs 为关系图摘要。
 */
@Data
public class BookWorkspaceVO {

    private BookVO book;
    private List<WorkspaceEventVO> events;
    private List<WorkspaceRelationGraphVO> relationGraphs;

    public BookWorkspaceVO() {
    }

    public BookWorkspaceVO(BookVO book, List<WorkspaceEventVO> events, List<WorkspaceRelationGraphVO> relationGraphs) {
        this.book = book;
        this.events = events;
        this.relationGraphs = relationGraphs;
    }

    /**
     * 工作区事件（含页面列表）
     */

    @Data
    public static class WorkspaceEventVO {

        private Long id;
        private String name;
        private Integer sortOrder;
        private List<WorkspacePageVO> pages;

        public WorkspaceEventVO() {
        }

        public WorkspaceEventVO(Long id, String name, Integer sortOrder, List<WorkspacePageVO> pages) {
            this.id = id;
            this.name = name;
            this.sortOrder = sortOrder;
            this.pages = pages;
        }

    }

    /**
     * 工作区页面（仅目录信息，不含画布数据）
     */
    @Data
    public static class WorkspacePageVO {

        private Long id;
        private String name;
        private Integer sortOrder;

        public WorkspacePageVO() {
        }

        public WorkspacePageVO(Long id, String name, Integer sortOrder) {
            this.id = id;
            this.name = name;
            this.sortOrder = sortOrder;
        }

    }

    /**
     * 工作区关系图摘要
     */
    @Data
    public static class WorkspaceRelationGraphVO {

        private Long id;
        private String name;

        public WorkspaceRelationGraphVO() {
        }

        public WorkspaceRelationGraphVO(Long id, String name) {
            this.id = id;
            this.name = name;
        }

    }

}

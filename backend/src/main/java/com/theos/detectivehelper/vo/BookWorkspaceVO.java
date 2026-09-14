package com.theos.detectivehelper.vo;

import java.util.List;

/**
 * 案件书工作空间VO
 * <p>
 * 对应 GET /api/books/{id}/workspace 的响应结构：
 * book 为案件书元信息，events 内嵌各自的页面列表，relationGraphs 为关系图摘要。
 */
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

    // Getters and Setters
    public BookVO getBook() {
        return book;
    }

    public void setBook(BookVO book) {
        this.book = book;
    }

    public List<WorkspaceEventVO> getEvents() {
        return events;
    }

    public void setEvents(List<WorkspaceEventVO> events) {
        this.events = events;
    }

    public List<WorkspaceRelationGraphVO> getRelationGraphs() {
        return relationGraphs;
    }

    public void setRelationGraphs(List<WorkspaceRelationGraphVO> relationGraphs) {
        this.relationGraphs = relationGraphs;
    }

    //内部类
    /**
     * 工作区事件（含页面列表）
     */

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

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }

        public List<WorkspacePageVO> getPages() {
            return pages;
        }

        public void setPages(List<WorkspacePageVO> pages) {
            this.pages = pages;
        }
    }

    /**
     * 工作区页面（仅目录信息，不含画布数据）
     */
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

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    /**
     * 工作区关系图摘要
     */
    public static class WorkspaceRelationGraphVO {

        private Long id;
        private String name;

        public WorkspaceRelationGraphVO() {
        }

        public WorkspaceRelationGraphVO(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

}

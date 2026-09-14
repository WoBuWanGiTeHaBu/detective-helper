package com.theos.detectivehelper.dto;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 画布完整数据
 * <p>
 * 既是 {@code GET /api/pages/{id}/canvas} 的返回体，也是 {@code PUT /api/pages/{id}/canvas} 的请求体，
 * 两者结构完全一致，前端可以「取出来 → 改一部分 → 原样 PUT 回去」。
 * <p>
 * 设计约束：
 * <ul>
 *   <li>四个数组（objects / relationships / annotations / timelines）在读写两端都保证非 null，
 *       清空时传 {@code []} 而不是 null，避免「空画布被读成 null」。</li>
 *   <li>所有子节点继承 {@link Extensible}，未知字段通过 {@code @JsonAnySetter / @JsonAnyGetter}
 *       原样回填，保证 style / customFields / 各类扩展键无损往返。</li>
 *   <li>annotation.content 是 HTML 片段（如 {@code <p>xxx</p>}），按字符串整体存取，不做转义或截断。</li>
 *   <li>timeline.x / timeline.y 为面板在画布上的坐标，可空，缺省时前端按序自动落位。</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CanvasResponse {

    /** 画布背景：plain / grid / dot / line，缺省 plain */
    private String background;

    /** 画布纸面宽度，可空；前端固定尺寸纸张用，原样往返、无默认值 */
    private Double canvasWidth;
    /** 画布纸面高度，可空；前端固定尺寸纸张用，原样往返、无默认值 */
    private Double canvasHeight;

    private List<CanvasObject> objects;
    private List<Relationship> relationships;
    private List<Annotation> annotations;
    private List<Timeline> timelines;

    /**
     * 可携带未知字段的基类
     * <p>
     * Jackson 默认会丢弃没有对应属性的键，这会让前端新加的扩展字段（未来字段、临时字段）
     * 在「GET → PUT」一轮往返后悄悄丢失。这里用 any-setter/getter 把它们兜住并原样写回。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public abstract static class Extensible {

        private final Map<String, Object> extra = new LinkedHashMap<>();

        @JsonAnySetter
        public void putExtra(String key, Object value) {
            extra.put(key, value);
        }

        @JsonAnyGetter
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Map<String, Object> getExtra() {
            return extra;
        }
    }

    /** 画布对象（人物 / 事件 / 物品等节点） */
    @Data
    public static class CanvasObject extends Extensible {

        private String id;
        private String type;
        private String shape;
        private String name;
        private String description;
        private Double x;
        private Double y;
        private Double width;
        private Double height;
        /** 富文本内容，可含 HTML */
        private String text;
        /** 图片 data URL */
        private String image;
        /** 样式扩展，任意键值对，原样存取 */
        private Map<String, Object> style;
        /** 自定义字段，任意键值对，原样存取（空对象不能被写成 null） */
        private Map<String, Object> customFields;

    }

    /** 关系连线 */
    @Data
    public static class Relationship extends Extensible {

        private String id;
        private String source;
        private String target;
        private String type;
        private String label;
        private Map<String, Object> style;

    }

    /** 注解（便利贴式文本框），content 为 HTML 片段 */
    @Data
    public static class Annotation extends Extensible {

        private String id;
        private Double x;
        private Double y;
        private Double width;
        private Double height;
        private String content;
        private Map<String, Object> style;

    }

    /** 时间线面板 */
    @Data
    public static class Timeline extends Extensible {

        private String id;
        private String name;
        private String direction;
        private List<TimelinePoint> points;
        /** 面板左上角坐标，缺省时前端按序落位 */
        private Double x;
        /** 面板左上角坐标，缺省时前端按序落位 */
        private Double y;
        /** 横向面板宽度（direction=horizontal 时有效），缺省由前端兜底 */
        private Double width;
        /** 纵向面板高度（direction=vertical 时有效），缺省由前端兜底 */
        private Double height;
        private Map<String, Object> style;

    }

    /** 时间线上的时间点 */
    @Data
    public static class TimelinePoint extends Extensible {

        private String id;
        private String timeType;
        private String time;
        private String startTime;
        private String endTime;
        private String fuzzyDate;
        private String fuzzyPeriod;
        /** 后端解析出的真实区间起点，前端排布优先使用 */
        private String resolvedStart;
        /** 后端解析出的真实区间终点，前端排布优先使用 */
        private String resolvedEnd;
        private String label;
        private String description;
        private Map<String, Object> labelOffset;

    }
}

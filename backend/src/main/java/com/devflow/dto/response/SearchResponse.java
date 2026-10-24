package com.devflow.dto.response;

import com.devflow.entity.enums.SearchResourceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated global search response envelope containing results and query metadata")
public class SearchResponse extends PageResponse<SearchResult> {

    @Schema(description = "Normalized search query term", example = "authentication")
    private String query;

    @Schema(description = "Applied search resource type filter", example = "ALL")
    private SearchResourceType type;

    public SearchResponse() {
        super();
    }

    public SearchResponse(String query, SearchResourceType type, List<SearchResult> content, int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
        super(content, page, size, totalElements, totalPages, first, last);
        this.query = query;
        this.type = type;
    }

    public SearchResponse(String query, SearchResourceType type, PageResponse<SearchResult> pageResponse) {
        super(pageResponse.getContent(), pageResponse.getPage(), pageResponse.getSize(),
                pageResponse.getTotalElements(), pageResponse.getTotalPages(),
                pageResponse.isFirst(), pageResponse.isLast());
        this.query = query;
        this.type = type;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public SearchResourceType getType() {
        return type;
    }

    public void setType(SearchResourceType type) {
        this.type = type;
    }
}

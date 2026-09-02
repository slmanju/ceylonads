package com.slmanju.ceylonads.search.dto;

import org.springframework.data.domain.Sort;

public enum AdSortOption {

    NEWEST("newest", Sort.by(Sort.Direction.DESC, "createdAt")),
    OLDEST("oldest", Sort.by(Sort.Direction.ASC, "createdAt")),
    PRICE_ASC("price_asc", Sort.by(Sort.Direction.ASC, "price")),
    PRICE_DESC("price_desc", Sort.by(Sort.Direction.DESC, "price"));

    private final String param;
    private final Sort sort;

    AdSortOption(String param, Sort sort) {
        this.param = param;
        this.sort = sort;
    }

    public String param() {
        return param;
    }

    public Sort sort() {
        return sort;
    }

    /**
     * Unknown or missing values fall back to {@link #NEWEST} rather than rejecting the request,
     * matching the public search endpoint's tolerant query-param handling.
     */
    public static AdSortOption fromParam(String value) {
        if (value == null) {
            return NEWEST;
        }
        for (AdSortOption option : values()) {
            if (option.param.equalsIgnoreCase(value)) {
                return option;
            }
        }
        return NEWEST;
    }
}

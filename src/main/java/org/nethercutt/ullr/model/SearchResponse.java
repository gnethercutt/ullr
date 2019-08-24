package org.nethercutt.ullr.model;

import java.util.List;

import lombok.Data;

@Data
public class SearchResponse {
    @Data
    public static class DocumentMatch {
        private String doc;
        private float  score;
    }
    List<DocumentMatch> matches;
}
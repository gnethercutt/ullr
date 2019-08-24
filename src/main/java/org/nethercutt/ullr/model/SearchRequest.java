package org.nethercutt.ullr.model;

import lombok.Data;

@Data
public class SearchRequest {
    String index;
    String term;
}
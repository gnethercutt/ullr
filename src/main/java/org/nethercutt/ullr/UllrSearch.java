package org.nethercutt.ullr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.nethercutt.ullr.lucene.IndexProvider;
import org.nethercutt.ullr.lucene.Indexer;
import org.nethercutt.ullr.model.SearchRequest;
import org.nethercutt.ullr.model.SearchResponse;
import org.nethercutt.ullr.model.SearchResponse.DocumentMatch;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UllrSearch implements RequestHandler<SearchRequest, SearchResponse> {
    private IndexProvider indexProvider = new IndexProvider();
    
    @Override
	public SearchResponse handleRequest(SearchRequest request, Context context) {
        log.info("Search {} in index {}", request.getTerm(), request.getIndex());

        try {
            Indexer indexer = indexProvider.getIndex(request.getIndex());
            Query query = indexer.createQuery(request);
            TopDocs results = indexer.search(query, 100);
            
            List<SearchResponse.DocumentMatch> matches = new ArrayList<>();
            for (int i = 0; i < results.scoreDocs.length; i++) {
                DocumentMatch match = new SearchResponse.DocumentMatch();
                int docId = results.scoreDocs[i].doc;
                match.setDoc(indexer.doc(docId).get("path"));
                match.setScore(results.scoreDocs[i].score);
			    matches.add(match);
            }
            
            SearchResponse response = new SearchResponse();
            response.setMatches(matches);
            return response;
        } catch (ParseException | IOException e) {
            log.error("Failure to search", e);
        }
        return null;
    }
}

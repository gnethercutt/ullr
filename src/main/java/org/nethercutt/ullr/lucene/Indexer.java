package org.nethercutt.ullr.lucene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.nethercutt.ullr.io.FilesytemProvider;
import org.nethercutt.ullr.model.SearchRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Indexer implements AutoCloseable {
    private IndexReader reader;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private QueryParser parser;
    private Path indexPath;  

    public Indexer(String indexPath) throws IOException {
        this.indexPath = FilesytemProvider.toPath(indexPath);
        Directory indexDirectory = FSDirectory.open(this.indexPath);
        if (indexDirectory.listAll().length == 0) {
            log.info("Create new index");
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
            IndexWriter writer = new IndexWriter(indexDirectory, iwc);
            writer.close();
        }
        reader = DirectoryReader.open(indexDirectory);
        searcher = new IndexSearcher(reader);
        analyzer = new StandardAnalyzer();
        parser = new QueryParser("contents", analyzer);
    }

	public Query createQuery(SearchRequest request) throws ParseException {
        log.debug("createQuery for term: {}", request.getTerm());
		return parser.parse(request.getTerm());
	}

	public TopDocs search(Query query, int i) throws IOException {
		return searcher.search(query, i);
	}

	public Document doc(int docId) throws IOException {
		return searcher.doc(docId);
    }
    
    public void indexDocument(String docPathString) throws IOException {
        Path docPath = FilesytemProvider.toPath(docPathString);
        log.info("Add {} ({}) to index {}", docPathString, docPath, this.indexPath);

        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);

        IndexWriter writer = new IndexWriter(FSDirectory.open(this.indexPath), iwc);
        try (InputStream stream = Files.newInputStream(docPath)) {
            long lastModifiedTime = Files.getLastModifiedTime(docPath).toMillis();
            Document doc = new Document();
            Field pathField = new StringField("path", docPathString, Field.Store.YES);
            doc.add(pathField);
            doc.add(new LongPoint("modified", lastModifiedTime));
            doc.add(new TextField("contents", new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
            // TODO: consider adding the ETag / md5

            // TODO: update vs create
            log.info("Updating document {}", docPathString);
            writer.updateDocument(new Term("path", docPathString), doc);
            //writer.addDocument(doc);
            log.info("Updated document {}", docPathString);

            // TODO: eval optimization with a cost. I think this already happens in close().
            writer.forceMerge(1);
        } finally {
            writer.close();
            log.debug("Writer closed for {}", docPathString);
        }
    }

    public void removeDocument(String docPathString) throws IOException {
        log.info("Remove {} from index {}", docPathString, this.indexPath);

        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(OpenMode.APPEND);

        IndexWriter writer = new IndexWriter(FSDirectory.open(this.indexPath), iwc);
        try {
            writer.deleteDocuments(new Term("path", docPathString));
        } finally {
            writer.close();
            log.debug("Writer closed for {}", docPathString);
        }
    }

    @Override
    public void close() throws Exception {
        if (reader != null) {
            reader.close();
        }
    }
}
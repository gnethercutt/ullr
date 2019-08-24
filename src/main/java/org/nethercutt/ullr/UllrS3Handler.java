package org.nethercutt.ullr;

import java.io.IOException;
import java.util.Optional;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.Tag;

import org.nethercutt.ullr.config.ConfigUtils;
import org.nethercutt.ullr.lucene.IndexProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UllrS3Handler implements RequestHandler<S3Event, String> {

    private static final String DEFAULT_INDEX_BUCKET = "ullr-index";
    private static final String DEFAULT_INDEX = "ullr-indices/default";

    private IndexProvider indexProvider = new IndexProvider();

    public String handleRequest(S3Event s3event, Context context) {
        for (S3EventNotificationRecord record : s3event.getRecords()) {
            String srcBucket = record.getS3().getBucket().getName();

            // Object keys can have all kinds of unicode and non-ASCII characters we don't want
            String srcKey = record.getS3().getObject().getUrlDecodedKey();
            if (record.getEventName().equals("ObjectCreated:Put")) {
                AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
                // ObjectMetadata meta = s3Client.getObjectMetadata(srcBucket, srcKey);
                
                GetObjectTaggingResult tagsResult = s3Client.getObjectTagging(new GetObjectTaggingRequest(srcBucket, srcKey));
                Optional<Tag> indexTag = tagsResult.getTagSet().stream().filter(tag -> tag.getKey().equals("index")).findFirst();

                String targetIndex = (indexTag.isPresent()) ? indexTag.get().getValue() : DEFAULT_INDEX;
                String targetBucket = ConfigUtils.getStringProperty("indexBucket", DEFAULT_INDEX_BUCKET);
                String indexUri = String.format("s3://%s/%s/", targetBucket, targetIndex);
                String docUri = String.format("s3://%s/%s",srcBucket, srcKey);

                log.info("Add {} to index {}", docUri, indexUri);
                addDoc(indexUri, docUri);
            } else if (record.getEventName().equals("ObjectRemoved:Delete")) {
                // FIXME: tags aren't a great data source for deletes, unfortunately. Maybe directory convention instead.
                String targetIndex = DEFAULT_INDEX;
                String targetBucket = ConfigUtils.getStringProperty("indexBucket", DEFAULT_INDEX_BUCKET);
                String indexUri = String.format("s3://%s/%s/", targetBucket, targetIndex);
                String docUri = String.format("s3://%s/%s",srcBucket, srcKey);
                try {
                    indexProvider.getIndex(indexUri).removeDocument(docUri);
                } catch (IOException ioe) {
                    log.error("Failure to remove document ({})", docUri, ioe);
                }
            }
        }
        return "Ok";
    }

    private void addDoc(String indexUri, String docUri) {
        try {
            indexProvider.getIndex(indexUri).indexDocument(docUri);
        } catch (IOException ioe) {
            log.error("Failure to add document", ioe);
        }
    }
}
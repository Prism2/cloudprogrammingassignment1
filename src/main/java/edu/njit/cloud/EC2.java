package edu.njit.cloud;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;

public class EC2 {
    public final static String bucketName = "njit-cs-643";
    public final static String queueName = "picturequeue.fifo";
    public final static Region region = Region.US_EAST_1;
    public final static ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
    protected SqsClient sqsClient;
    protected String queueUrl;
    protected AmazonRekognition rekognitionClient;
    public EC2() {
        //Initialize SQS client
        sqsClient = SqsClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
        //Get SQS queue URL
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        queueUrl = sqsClient.getQueueUrl(getQueueRequest).queueUrl();
        //Rekognition client initialization
        rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();
    }
}

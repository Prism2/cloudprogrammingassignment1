package edu.njit.cloud;

import com.amazonaws.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.List;

public class EC2A extends EC2{
    private final S3Client s3;
    public EC2A() {
        super();
        s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();

    }
    public boolean isS3BucketObjectACar(String s3ObjectKey){
        DetectLabelsRequest request = new DetectLabelsRequest()
                .withImage(new Image().withS3Object(new S3Object().withName(s3ObjectKey).withBucket(bucketName)))
                .withMinConfidence(90F);
        try {
            DetectLabelsResult result = rekognitionClient.detectLabels(request);
            List<Label> labels = result.getLabels();

//            System.out.println("Detected labels for " + content.key() + "\n");
//            for (Label label : labels) {
//                System.out.println("Label: " + label.getName());
//            }

            boolean isLabeledCar = labels.stream()
                    .anyMatch(label -> "Car".equals(label.getName()));
            return isLabeledCar;
        } catch (AmazonRekognitionException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void sendSQSMessage(String body){
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(body)
                .messageGroupId("defaultgroup")
                .messageDeduplicationId(body)
                .build();
        sqsClient.sendMessage(sendMessageRequest);
    }
    public void detectCarsFromS3Bucket(){
        //Create Request to Iterate Through the Contents of the S3 Bucket
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .maxKeys(1)
                .build();
        ListObjectsV2Iterable listRes = s3.listObjectsV2Paginator(listReq);
        listRes.stream()
                .flatMap(r -> r.contents().stream())
                .forEach(content -> {
                    //check if S3 object is a car
                    if (isS3BucketObjectACar(content.key())){
                        //Send to SQS queue if there is a car in picture
                        sendSQSMessage(content.key());
                        //output the name of the car image to logs
                        System.out.println(content.key());
                    }
                });
        //Send -1 to the SQS queue signify the end
        sendSQSMessage("-1");
    }

    public static void main(String[] args) {
        EC2A ec2A = new EC2A();
        ec2A.detectCarsFromS3Bucket();
    }
}

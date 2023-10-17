package edu.njit.cloud;

import com.amazonaws.services.rekognition.model.*;
import software.amazon.awssdk.services.sqs.model.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class EC2B extends EC2{
    public EC2B() {
        super();
    }

    public void getTextFromPicturesInQueue(){
        //Create request to long poll from SQS queue
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .waitTimeSeconds(20)
                .build();
        boolean continueLooping = true;
        //loop to poll for messages from SQS queue
        while (continueLooping) {
            //check for new messages
            ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveRequest);
            //Create buffered writer to append the results to output.txt
            try (FileWriter fileWriter = new FileWriter("output.txt", true);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                 PrintWriter printWriter = new PrintWriter(bufferedWriter)) {
                for (Message message: receiveMessageResponse.messages()) {
                    //break loop if message equal -1 which signifies the end
                    if (message.body().equals("-1")){
                        continueLooping = false;
                        break;
                    }
                    //find text inside the S3Object referenced in the message
                    DetectTextRequest detectTextRequest = new DetectTextRequest()
                            .withImage(new Image().withS3Object(new S3Object().withName(message.body()).withBucket(bucketName)));
                    try {
                        DetectTextResult detectTextResult = rekognitionClient.detectText(detectTextRequest);
                        if (!detectTextResult.getTextDetections().isEmpty()) {
                            String outputText = "File: " + message.body() + " - " + detectTextResult.getTextDetections().stream().map(TextDetection::getDetectedText).collect(Collectors.joining(", "));
                            printWriter.write(outputText);
                            printWriter.write("\n");
                            System.out.println(outputText);
                        }
                    } catch (AmazonRekognitionException e) {
                        e.printStackTrace();
                    }
                    //delete message once done detecting text inside the referenced S3Object
                    DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build();
                    sqsClient.deleteMessage(deleteMessageRequest);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
    public static void main(String[] args) {
        EC2B ec2B = new EC2B();
        ec2B.getTextFromPicturesInQueue();

//        // Enable long polling when for request queue.
//        HashMap<QueueAttributeName, String> attributes = new HashMap<QueueAttributeName, String>();
//        attributes.put(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS, "20");
//        SetQueueAttributesRequest setAttrsRequest = SetQueueAttributesRequest.builder()
//                .queueUrl(queueUrl)
//                .attributes(attributes)
//                .build();
//        // Enable long polling on a message receipt.
//        sqsClient.setQueueAttributes(setAttrsRequest);





    }
}

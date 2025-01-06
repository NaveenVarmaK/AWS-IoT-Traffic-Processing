package emse.cps2.sqs;

import emse.cps2.config.EnvironmentConfig;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.File;
import java.nio.file.Paths;

public class ExportClient {

    public static void main(String[] args) {
        String queueURL = EnvironmentConfig.ExportConfig.getSqsQueueUrl();
        String localDirectory = EnvironmentConfig.ExportConfig.getLocalDirectory();
        Region awsRegion = Region.US_EAST_1; // Specify the AWS region

        // Initialize SQS client
        SqsClient sqsClient = SqsClient.builder().region(awsRegion).build();

        // Receive a message from the SQS queue
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueURL)
                .maxNumberOfMessages(1)  // Retrieve only one message at a time
                .waitTimeSeconds(10)     // Long polling for 10 seconds
                .build();

        ReceiveMessageResponse receiveResponse = sqsClient.receiveMessage(receiveRequest);

        if (receiveResponse.messages().isEmpty()) {
            System.out.println("No messages available in the queue.");
            return;
        }

        // Process the first message
        Message message = receiveResponse.messages().get(0);
        String messageBody = message.body();

        // Parse the bucket name and file name from the message
        String[] messageParts = messageBody.split(";");
        if (messageParts.length < 2) {
            System.out.println("Invalid message format.");
            return;
        }

        String bucketName = messageParts[0];
        String fileName = messageParts[1];

        System.out.println("Retrieved message: " + messageBody);
        System.out.println("Bucket: " + bucketName + ", File: " + fileName);

        // Initialize S3 client
        S3Client s3Client = S3Client.builder().region(awsRegion).build();

        // Download the file from S3
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            // Specify the local directory to save the file
            s3Client.getObject(getObjectRequest,
                    Paths.get(localDirectory + File.separator + fileName));

            System.out.println("File downloaded successfully to: " + localDirectory);
        } catch (Exception e) {
            System.err.println("Error downloading the file: " + e.getMessage());
            return;
        }

        // Delete the message from the SQS queue after processing
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueURL)
                .receiptHandle(message.receiptHandle())
                .build();

        sqsClient.deleteMessage(deleteRequest);
        System.out.println("Message deleted from the queue.");
    }
}


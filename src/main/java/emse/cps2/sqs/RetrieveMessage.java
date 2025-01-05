package emse.cps2.sqs;

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



public class RetrieveMessage {

    public static void main(String[] args) {
        // Declare the arguments inside the function
        String queueURL = "https://sqs.us-east-1.amazonaws.com/498637188134/messaging-app-queue"; // Replace with your SQS queue URL
        String localDirectory = "C:\\Users\\knave\\OneDrive\\Documents\\CPS2 M2\\Cloud_Edge\\AWS_Iot_Traffic_Project\\src\\main\\java\\emse\\cps2\\UploadClient"; // Replace with your desired local directory path
        Region awsRegion = Region.US_EAST_1; // Specify the AWS region
        String bucketName = "mylambdatestbucket2000";
        String fileName = "values.csv";

        // Initialize SQS client
        SqsClient sqsClient = SqsClient.builder().region(awsRegion).build();

        // Receive a message
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueURL)
                .maxNumberOfMessages(1)
                .waitTimeSeconds(10)
                .build();

        ReceiveMessageResponse receiveResponse = sqsClient.receiveMessage(receiveRequest);

        if (receiveResponse.messages().isEmpty()) {
            System.out.println("No messages available in the queue.");
            return;
        }

        // Process the first message
        Message message = receiveResponse.messages().get(0);
        String messageBody = message.body();

        // Parse bucket name and file name
        String[] messageParts = messageBody.split(";");
        if (messageParts.length < 2) {
            System.out.println("Invalid message format.");
            return;
        }

        bucketName = messageParts[0];
        fileName = messageParts[1];

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

            s3Client.getObject(getObjectRequest,
                    Paths.get(localDirectory + File.separator + fileName));

            System.out.println("File downloaded successfully to: " + localDirectory);
        } catch (Exception e) {
            System.err.println("Error downloading the file: " + e.getMessage());
            return;
        }

        // Delete the message from the queue
        DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                .queueUrl(queueURL)
                .receiptHandle(message.receiptHandle())
                .build();

        sqsClient.deleteMessage(deleteRequest);
        System.out.println("Message deleted from the queue.");
    }
    
}

package emse.cps2.lambda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.ByteArrayInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public class ConsolidatorWorker implements RequestHandler<S3Event, String> {

    @Override
    public String handleRequest(S3Event event, Context context) {
        // Get the S3 event record
        S3EventNotificationRecord record = event.getRecords().get(0);
        String bucketName = record.getS3().getBucket().getName();
        String fileKey = record.getS3().getObject().getUrlDecodedKey();

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        Map<String, List<DataEntry>> dataMap = new HashMap<>();

        try (S3Object s3Object = s3Client.getObject(bucketName, fileKey);
             InputStreamReader streamReader = new InputStreamReader(s3Object.getObjectContent(), StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            // Skip header
            String line = reader.readLine();

            // Process each line of the CSV file
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                String srcIP = fields[0];  // Src IP
                String dstIP = fields[1];  // Dst IP
                int flowDuration = Integer.parseInt(fields[3]);  // Total Flow Duration
                int totFwdPkts = Integer.parseInt(fields[4]);    // Total Forward Packets

                // Create a key for Src IP and Dst IP
                String key = srcIP + "-" + dstIP;

                // Store the data in a map for each unique Src IP and Dst IP combination
                dataMap.computeIfAbsent(key, k -> new ArrayList<>())
                        .add(new DataEntry(flowDuration, totFwdPkts));
            }

            // Prepare the output CSV content
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Src IP,Dst IP,Total Flow Duration,Total Forward Packets,Average Flow Duration,Average Forward Packets,Std Dev Flow Duration,Std Dev Forward Packets\n");

            // Iterate over the dataMap and calculate average and standard deviation
            for (Map.Entry<String, List<DataEntry>> entry : dataMap.entrySet()) {
                String key = entry.getKey();
                List<DataEntry> dataEntries = entry.getValue();
                String[] keyParts = key.split("-");
                String srcIP = keyParts[0];
                String dstIP = keyParts[1];

                // Calculate average and standard deviation for Flow Duration and Forward Packets
                double avgFlowDuration = calculateAverage(dataEntries, DataEntry::getFlowDuration);
                double avgTotFwdPkts = calculateAverage(dataEntries, DataEntry::getTotFwdPkts);
                double stdDevFlowDuration = calculateStdDev(dataEntries, DataEntry::getFlowDuration, avgFlowDuration);
                double stdDevTotFwdPkts = calculateStdDev(dataEntries, DataEntry::getTotFwdPkts, avgTotFwdPkts);

                // Append the calculated values to the CSV content
                csvContent.append(srcIP)
                        .append(",")
                        .append(dstIP)
                        .append(",")
                        .append(sum(dataEntries, DataEntry::getFlowDuration))
                        .append(",")
                        .append(sum(dataEntries, DataEntry::getTotFwdPkts))
                        .append(",")
                        .append(avgFlowDuration)
                        .append(",")
                        .append(avgTotFwdPkts)
                        .append(",")
                        .append(stdDevFlowDuration)
                        .append(",")
                        .append(stdDevTotFwdPkts)
                        .append("\n");
            }

            // Upload the result to S3
            String outputBucketName = "export-client-cps2";  // Replace with your target S3 bucket name
            String outputFileName = "consolidated_traffic_data.csv";  // your desired Output file name

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(csvContent.toString().getBytes(StandardCharsets.UTF_8));
            PutObjectRequest putObjectRequest = new PutObjectRequest(outputBucketName, outputFileName, byteArrayInputStream, null);
            s3Client.putObject(putObjectRequest);

            System.out.println("Consolidated CSV file uploaded successfully to S3");

            // Send a message to the SQS queue
            sendMessageToQueue(outputBucketName, outputFileName);

            // Publish a notification to the SNS topic
            publishNotification(outputBucketName, outputFileName);

        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }

        return "Processing Complete";
    }

    private void sendMessageToQueue(String bucketName, String fileName) {
        String queueURL = "https://sqs.us-east-1.amazonaws.com/498637188134/ExportClientQueue";  // Replace with your SQS queue URL
        Region region = Region.US_EAST_1;

        SqsClient sqsClient = SqsClient.builder().region(region).build();

        SendMessageRequest sendRequest = SendMessageRequest.builder()
                .queueUrl(queueURL)
                .messageBody(bucketName + ";" + fileName)
                .build();

        SendMessageResponse sqsResponse = sqsClient.sendMessage(sendRequest);

        System.out.println(
                sqsResponse.messageId() + " Message sent. Status is " + sqsResponse.sdkHttpResponse().statusCode());
    }

    private void publishNotification(String bucketName, String fileName) {
        String topicArn = "arn:aws:sns:us-east-1:498637188134:ConsolidatorWorker-SNS";  // Replace with your SNS topic ARN
        Region region = Region.US_EAST_1;

        SnsClient snsClient = SnsClient.builder().region(region).build();

        String message = "File processed successfully From ConsolidatorWorker and sended to.\nBucket: " + bucketName + "\nFile: " + fileName + "\n and It is ready for download. Please run the function ExportClient to download the file.";

        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .build();

        PublishResponse publishResponse = snsClient.publish(publishRequest);

        System.out.println(
                publishResponse.messageId() + " Notification sent. Status is " + publishResponse.sdkHttpResponse().statusCode());
    }

    // Helper methods for calculations
    private double calculateAverage(List<DataEntry> dataEntries, java.util.function.ToIntFunction<DataEntry> extractor) {
        return dataEntries.stream()
                .mapToInt(extractor)
                .average()
                .orElse(0);
    }

    private double calculateStdDev(List<DataEntry> dataEntries, java.util.function.ToIntFunction<DataEntry> extractor, double mean) {
        double variance = dataEntries.stream()
                .mapToDouble(entry -> Math.pow(extractor.applyAsInt(entry) - mean, 2))
                .average()
                .orElse(0);
        return Math.sqrt(variance);
    }

    private int sum(List<DataEntry> dataEntries, java.util.function.ToIntFunction<DataEntry> extractor) {
        return dataEntries.stream()
                .mapToInt(extractor)
                .sum();
    }

    // DataEntry class
    private static class DataEntry {
        private final int flowDuration;
        private final int totFwdPkts;

        public DataEntry(int flowDuration, int totFwdPkts) {
            this.flowDuration = flowDuration;
            this.totFwdPkts = totFwdPkts;
        }

        public int getFlowDuration() {
            return flowDuration;
        }

        public int getTotFwdPkts() {
            return totFwdPkts;
        }
    }
}

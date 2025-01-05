package emse.cps2.lambda;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
//implement logging

public class SummarizeWorker implements RequestHandler<S3Event, String> {

    public String handleRequest(S3Event event, Context context) {
        S3EventNotificationRecord record = event.getRecords().get(0);
        String bucketName = record.getS3().getBucket().getName();
        String fileKey = record.getS3().getObject().getUrlDecodedKey();

        AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
        Map<String, SummaryData> summaryMap = new HashMap<>();

        try (final S3Object s3Object = s3Client.getObject(bucketName, fileKey);
             final InputStreamReader streamReader = new InputStreamReader(s3Object.getObjectContent(), StandardCharsets.UTF_8);
             final BufferedReader reader = new BufferedReader(streamReader)) {

            // Skip header
            String line = reader.readLine();

            // Process each line of the CSV file
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");
                String srcIP = fields[1];  // Src IP
                String dstIP = fields[3];  // Dst IP
                String timestamp = fields[6];  // Timestamp (date is part of this field)
                String flowDuration = fields[7];  // Flow Duration
                String totFwdPkts = fields[8];  // Tot Fwd Pkts

                // Extract the date from the timestamp (assuming the format is MM/dd/yyyy HH:mm)
                String date = timestamp.split(" ")[0];

                // Create a unique key for the combination of Src IP, Dst IP, and Date
                String key = srcIP + "-" + dstIP + "-" + date;

                // Convert Flow Duration and Tot Fwd Pkts to integers
                int flowDurationInt = Integer.parseInt(flowDuration);
                int totFwdPktsInt = Integer.parseInt(totFwdPkts);

                // Update the summary data for this combination
                summaryMap.compute(key, (k, v) -> {
                    if (v == null) {
                        v = new SummaryData();
                    }
                    v.addFlowDuration(flowDurationInt);
                    v.addTotFwdPkts(totFwdPktsInt);
                    return v;
                });
            }

            // Prepare the CSV content
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("Src IP,Dst IP,Date,Total Flow Duration,Total Forward Packets\n");

            for (Map.Entry<String, SummaryData> entry : summaryMap.entrySet()) {
                String key = entry.getKey();
                SummaryData data = entry.getValue();
                String[] keyParts = key.split("-");
                String srcIP = keyParts[0];
                String dstIP = keyParts[1];
                String date = keyParts[2];

                csvContent.append(srcIP)
                        .append(",")
                        .append(dstIP)
                        .append(",")
                        .append(date)
                        .append(",")
                        .append(data.getTotalFlowDuration())
                        .append(",")
                        .append(data.getTotalTotFwdPkts())
                        .append("\n");
            }

            // Upload the CSV to another S3 bucket
            String outputBucketName = "summarizedtables-cps2"; 
            String outputFileName = "summarized_traffic_data.csv";  // Output file name in the S3 bucket

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(csvContent.toString().getBytes(StandardCharsets.UTF_8));
            PutObjectRequest putObjectRequest = new PutObjectRequest(outputBucketName, outputFileName, byteArrayInputStream, null);

            s3Client.putObject(putObjectRequest);

            System.out.println("CSV file uploaded successfully to S3");

            // Publish a notification to the SNS topic
            publishNotification(outputBucketName, outputFileName);

        } catch (final IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }

        return "Processing Complete";
    }

    private void publishNotification(String bucketName, String fileName) {
        String topicArn = "arn:aws:sns:us-east-1:498637188134:SummarizedWorker-SNS";  // Replace with your SNS topic ARN
        Region region = Region.US_EAST_1;

        SnsClient snsClient = SnsClient.builder().region(region).build();

        String message = "File processed successfully from SummarizedWorker to the.\nBucket: " + bucketName + "\nFile: " + fileName;

        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .message(message)
                .build();

        PublishResponse publishResponse = snsClient.publish(publishRequest);

        System.out.println(
                publishResponse.messageId() + " Notification sent. Status is " + publishResponse.sdkHttpResponse().statusCode());
    }

    // Helper class to store aggregated data for each Src IP, Dst IP, and Date combination
    private static class SummaryData {
        private int totalFlowDuration = 0;
        private int totalTotFwdPkts = 0;

        public void addFlowDuration(int flowDuration) {
            totalFlowDuration += flowDuration;
        }

        public void addTotFwdPkts(int totFwdPkts) {
            totalTotFwdPkts += totFwdPkts;
        }

        public int getTotalFlowDuration() {
            return totalFlowDuration;
        }

        public int getTotalTotFwdPkts() {
            return totalTotFwdPkts;
        }
    }
}

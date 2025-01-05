package emse.cps2.sns;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

public class PublishTopic {

  public static void main(String[] args) {
    Region region = Region.US_EAST_1;

    // Declare arguments directly in the code
    String topicARN = "arn:aws:sns:us-east-1:498637188134:MyFirstTopic"; // Replace with your actual Topic ARN
    String bucketName = "mylambdatestbucket2000"; // Replace with your actual bucket name
    String fileName = "values.csv"; // Replace with your actual file name

    try {
      SnsClient snsClient = SnsClient.builder().region(region).build();

      PublishRequest request = PublishRequest.builder()
          .message(bucketName + ";" + fileName)
          .topicArn(topicARN)
          .build();

      PublishResponse snsResponse = snsClient.publish(request);
      System.out.println(
          snsResponse.messageId() + " Message sent. Status is " + snsResponse.sdkHttpResponse().statusCode());

    } catch (SnsException e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }
  }
}
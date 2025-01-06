package emse.cps2.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.util.logging.Logger;

public class EnvironmentConfig {
    private static final Logger LOGGER = Logger.getLogger(EnvironmentConfig.class.getName());
    private static Dotenv dotenv;

    static {
      try {
          dotenv = Dotenv.configure()
                  .ignoreIfMissing()
                  .load();
      } catch (Exception e) {
          LOGGER.warning("Failed to load .env file: " + e.getMessage());
      }
  }

  public static String get(String key, String defaultValue) {
      String value = System.getenv(key);
      if (value == null && dotenv != null) {
          value = dotenv.get(key);
      }
      return value != null ? value : defaultValue;
  }

  public static class ConsolidatorConfig {
      public static String getOutputBucketName() {
          return get("CONSOLIDATOR_OUTPUT_BUCKET_NAME", "export-client-cps2");
      }

      public static String getOutputFileName() {
          return get("CONSOLIDATOR_OUTPUT_FILE_NAME", "consolidated_traffic_data.csv");
      }

      public static String getSqsQueueUrl() {
          return get("CONSOLIDATOR_SQS_QUEUE_URL", "https://sqs.us-east-1.amazonaws.com/498637188134/ExportClientQueue");
      }

      public static String getSnsTopicArn() {
          return get("CONSOLIDATOR_SNS_TOPIC_ARN", "arn:aws:sns:us-east-1:498637188134:ConsolidatorWorker-SNS");
      }
  }

  public static class SummarizerConfig {
      public static String getOutputBucketName() {
          return get("SUMMARIZER_OUTPUT_BUCKET_NAME", "summarizedtables-cps2");
      }

      public static String getOutputFileName() {
          return get("SUMMARIZER_OUTPUT_FILE_NAME", "summarized_traffic_data.csv");
      }

      public static String getSnsTopicArn() {
          return get("SUMMARIZER_SNS_TOPIC_ARN", "arn:aws:sns:us-east-1:498637188134:SummarizedWorker-SNS");
      }
  }

  public static class ExportConfig {
      public static String getSqsQueueUrl() {
          return get("EXPORT_CLIENT_SQS_QUEUE_URL", "https://sqs.us-east-1.amazonaws.com/498637188134/ExportClientQueue");
      }

      public static String getLocalDirectory() {
          return get("EXPORT_CLIENT_LOCAL_DIRECTORY", "C:\\Users\\knave\\OneDrive\\Documents\\CPS2 M2\\Cloud_Edge\\AWS_Iot_Traffic_Project\\src\\main\\S3RetrievedFiles");
      }

      public static String getAwsRegion() {
          return get("EXPORT_CLIENT_AWS_REGION", "us-east-1");
      }
  }

  public static class UploadConfig {
      public static String getBucketName() {
          return get("UPLOAD_S3_BUCKET_NAME", "upload-client-cps2");
      }

      public static String getFilePath() {
          return get("UPLOAD_S3_FILE_PATH", "C:\\Users\\knave\\OneDrive\\Documents\\CPS2 M2\\Cloud_Edge\\AWS_Iot_Traffic_Project\\src\\main\\resources\\data-20221207.csv");
      }
  }
}

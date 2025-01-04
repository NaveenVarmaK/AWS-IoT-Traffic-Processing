package emse.cps2.UploadClient;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.nio.file.Path;

public class UploadS3 {

    public static void main(String[] args) {
        // Declare the bucket name and file path directly in the code
        String bucketName = "upload-client-cps2";
        String filePath = "C:\\Users\\knave\\OneDrive\\Documents\\CPS2 M2\\Cloud_Edge\\group_project\\src\\main\\resources\\data-20221207.csv";

        S3Client s3 = S3Client.create();

        try {
            // Check if the bucket exists
            if (!doesBucketExist(s3, bucketName)) {
                System.out.println("Bucket does not exist. Creating bucket: " + bucketName);
                createBucket(s3, bucketName);
            }

            // Upload the file
            System.out.println("Uploading file: " + filePath);
            uploadFile(s3, bucketName, "values.csv", filePath);
            System.out.println("File uploaded successfully.");

        } catch (S3Exception e) {
            System.err.println("Error occurred: " + e.awsErrorDetails().errorMessage());
        } finally {
            s3.close();
        }
    }

    private static boolean doesBucketExist(S3Client s3, String bucketName) {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3.headBucket(request);
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    private static void createBucket(S3Client s3, String bucketName) {
        CreateBucketRequest request = CreateBucketRequest.builder()
                .bucket(bucketName)
                .build();
        s3.createBucket(request);
    }

    private static void uploadFile(S3Client s3, String bucketName, String key, String filePath) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3.putObject(request, Path.of(filePath));
    }
}

#AWS IoT Traffic Data Processing

## Project Overview

This project is designed to automate the process of uploading, summarizing, and consolidating IoT traffic data collected from geographically distributed IoT devices. The enterprise aims to analyze this traffic data to detect anomalies and bottlenecks, which will help guide infrastructure investment decisions. Instead of investing in permanent infrastructure, the enterprise uses a Public Cloud infrastructure that covers all geographical locations where the enterprise has a branch.

Each branch generates a comma-separated values (CSV) file containing IoT traffic data, which you can download from the [Dataset of Legitimate IoT Data (VARIoT)](https://www.data.gouv.fr/en/datasets/dataset-of-legitimate-iot-data/).

The data is processed using multiple AWS services, including Lambda, S3, SNS, and SQS, to automate the process of uploading, summarizing, and consolidating the data.

## Project Setup and Instructions

This project involves multiple AWS services to automate the process of uploading, summarizing, and consolidating data. Below are the steps to set up and run the project.

### 1. **UploadS3.java (Initial Setup)**

To begin, the project starts with the `UploadS3.java` file, which is responsible for uploading files to an S3 bucket. In this file, the following parameters are hardcoded:

- **Bucket Name**: `upload-client-cps2`
- **File Location**: The file is uploaded from a location inside the project’s `resources` folder. This path is hardcoded, and you will need to change it to match the location of your file.

To modify the location or the bucket name, open the `UploadS3.java` file and update the following variables:

```java
bucketName = "upload-client-cps2"; // Change to your desired bucket name
fileLocation = "path/to/your/file"; // Update with your file's path inside the resources folder
```

### 2. **SummarizeWorker Lambda Function**

Once the file is uploaded to the `upload-client-cps2` bucket, it triggers the `SummarizeWorker` Lambda function. This function summarizes the data and then uploads the summarized file to another S3 bucket.

- **Target Bucket**: `summarizedtables-cps2`
- **Summarized File Name**: The output file is named `summarized_traffic_data.csv`. You can modify this to any desired name.

In the `SummarizeWorker` Lambda function, you need to change the target bucket and file name as per your requirement.

### 3. **ConsolidatorWorker Lambda Function**

The `ConsolidatorWorker` Lambda function is triggered by the `summarizedtables-cps2` bucket. It processes the summarized CSV file, applying statistical operations such as standard deviation and average.

- **SNS Topic**: The result is published to an SNS topic named `ConsolidatorWorker-SNS`. You can subscribe to this topic using your email or any other desired method. If you want to change the subscription, go to the SNS console and modify the subscription settings.

The Lambda function sends a notification once the file is processed and ready for download.

### 4. **SQS Integration (ExportClientQueue)**

The project also uses an SQS queue named `ExportClientQueue`. This queue stores the bucket name and file name, which are used to retrieve the file later.

- **Queue Name**: `ExportClientQueue`
- **File Location**: The `ExportClient.java` file downloads the file from the queue and saves it to a folder. You can modify the folder location by changing the path in the `ExportClient.java` file.

To change the folder location, update the path where the file is saved in the `ExportClient.java` file.

### 5. **Disclaimer**

Please note the following:

- **Hardcoded Values**: The bucket names, SNS topic names, queue URLs, and file names are mostly hardcoded in the project. You will need to change them to match your own setup.
- **Permissions**: Currently, the project uses limited permissions (via a LabRole). In the future, it is planned to implement an SQS queue between the `UploadS3.java` and `SummarizeWorker` functions, as well as between `SummarizeWorker` and `ConsolidatorWorker`. This will ensure that the Lambda functions can retrieve files from the queue if they are busy. To implement this, you will need to enable full access to SQS for Lambda functions. Please note that this requires appropriate IAM permissions.

### 6. **Future Improvements**

- **SQS Queue Between Functions**: There is a plan to implement SQS queues between the Lambda functions to improve reliability and allow for better handling of busy Lambda functions. This will require setting up permissions for Lambda to have full access to SQS.

### Data Source

The IoT traffic data used in this project is collected from geographically distributed IoT devices. Each branch of the enterprise generates a CSV file containing the traffic data. This data can be downloaded from the following location:

[Dataset of Legitimate IoT Data (VARIoT)](https://www.data.gouv.fr/en/datasets/dataset-of-legitimate-iot-data/)

The CSV file contains data such as traffic volumes, device IDs, timestamps, and other relevant information for analyzing the IoT traffic.

---

Feel free to modify the project parameters such as bucket names, file locations, and SNS subscriptions to suit your environment. If you have any questions or need further assistance, don't hesitate to reach out.

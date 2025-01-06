# AWS IoT Traffic Data Processing

## Project Overview

This project implements a cloud-based system for analyzing IoT traffic data using AWS services. The system processes CSV files containing IoT traffic data, summarizes the traffic patterns, and provides statistical analysis to help detect anomalies and bottlenecks.

Each branch generates a comma-separated values (CSV) file containing IoT traffic data, which you can download from the [Dataset of Legitimate IoT Data (VARIoT)](https://www.data.gouv.fr/en/datasets/dataset-of-legitimate-iot-data/).

The data is processed using multiple AWS services, including Lambda, S3, SNS, and SQS, to automate the process of uploading, summarizing, and consolidating the data.

## Prerequisites

- Java 17
- Maven 3.9.5+
- AWS Account (AWS Academy account works)
- AWS CLI installed
- VS Code with following extensions:
  - Extension Pack for Java
  - AWS Toolkit
  - Maven for Java

## Project Structure

```bash
AWS-IoT-Traffic-Processing/
├── src/
│   ├── main/
│   │   ├── java/emse/cps2/
│   │   │   ├── UploadClient/
│   │   │   ├── lambda/
│   │   │   └── sqs/
│   │   └── resources/
│   │   └── S3RetrievedFiles/
│   └── test/
│       └── java/
├── pom.xml
└── README.md
```

## How The System Works: Step by Step

### Workflow Overview

The system implements a serverless data processing pipeline using AWS services, processing IoT traffic data through multiple stages:

1. **File Upload (UploadS3.java)**

- **Primary Function**: Upload CSV files to AWS S3
- **Configuration**:
  - Initial Bucket: `upload-client-cps2-[yourname]`
  - Data Source: Project's `resources` folder
  - File Type: CSV format IoT traffic data

2. **Data Summarization (SummarizeWorker Lambda)**

- **Trigger**: S3 upload event from initial bucket
- **Operations**:
  - Processes raw IoT traffic data
  - Summarizes traffic between source and destination devices
  - Calculates total flow duration and forward packets
- **Output**: Writes to `summarizedtables-cps2-[yourname]` bucket as `summarized_traffic_data.csv`

3. **Data Consolidation and Statistical Analysis (ConsolidatorWorker Lambda)**

- **Trigger**: S3 event from summarized data bucket
- **Operations**:
  - Performs statistical analysis (average, standard deviation)
  - Updates device traffic patterns
  - Generates consolidated metrics
- **Notifications**:
  - Publishes results to `ConsolidatorWorker-SNS` topic
  - Enables email notifications for process completion

4. **Export Process and SQS Integration (ExportClient)**

- **Queue Integration**: Monitors `ExportClientQueue` for processed files
- **Operations**:
  - Retrieves bucket and file information from SQS
  - Downloads processed data to specified local directory
  - Manages file retrieval states

### Data Flow Map

```bash
Raw CSV → Upload Bucket → Summary Processing → Analysis → Export Queue → Local Storage
[UploadS3.java] → [S3] → [SummarizeWorker] → [ConsolidatorWorker] → [SQS] → [ExportClient]
```

### Key Components

- **S3 Buckets**: 3 buckets for different data stages
- **Lambda Functions**: 2 serverless processors
- **Messaging**: SNS for notifications, SQS for export management
- **Client Applications**: Upload and export handlers

## Project Setup and Instructions

### 1. Initial Project Setup

```bash
# Clone the repository
git clone https://github.com/NaveenVarmaK/AWS-IoT-Traffic-Processing.git
cd AWS-IoT-Traffic-Processing

# Build the project
mvn clean install

# Install AWS CLI on MacOS if not installed
brew install awscli

# For windows run
msiexec.exe /i https://awscli.amazonaws.com/AWSCLIV2.msi

# Or download the AWS CLI MSI installer for Windows directly from this link
https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html.
```

### 1. AWS Environment Setup

1. Log into AWS Academy
2. Start your Lab
3. Get AWS credentials from AWS Academy
4. Configure AWS CLI: You have two options to configure AWS credentials:

#### Option 1: Using AWS Configure

```bash
   aws configure
   # Enter AWS Access Key ID
   # Enter AWS Secret Access Key
   # Region: us-east-1
   # Output format: json
```

#### Option 2: Manual Credentials File

Create or update `~/.aws/credentials` with:

```ini
[default]
aws_access_key_id = your_access_key
aws_secret_access_key = your_secret_key
region = us-east-1
```

### 3. AWS Resources Setup

Create necessary S3 buckets:

- `[yourname]`: choose your unique identifier for the S3 buckets

```bash
aws s3 mb s3://upload-client-cps2-[yourname]
aws s3 mb s3://summarizedtables-cps2-[yourname]
aws s3 mb s3://export-client-cps2-[yourname]
```

Create SQS queue:

```bash
aws sqs create-queue --queue-name ExportClientQueue
```

Create SNS topics:

```bash
aws sns create-topic --name ConsolidatorWorker-SNS
aws sns create-topic --name SummarizedWorker-SNS
```

### 4. Environment Variables Setup

The following environment variables are used throughout the project:

#### ConsolidatorWorker Variables

File: `src/main/java/emse/cps2/lambda/ConsolidatorWorker.java`

| Variable Name                     | Value                                                                     | Description                             |
| --------------------------------- | ------------------------------------------------------------------------- | --------------------------------------- |
| `CONSOLIDATOR_OUTPUT_BUCKET_NAME` | "export-client-cps2-[yourname]"                                           | S3 bucket for storing consolidated data |
| `CONSOLIDATOR_OUTPUT_FILE_NAME`   | "consolidated_traffic_data.csv"                                           | Name of the output consolidated file    |
| `CONSOLIDATOR_SQS_QUEUE_URL`      | "https://sqs.us-east-1.amazonaws.com/[YOUR-ACCOUNT-ID]/ExportClientQueue" | SQS queue URL for message handling      |
| `CONSOLIDATOR_SNS_TOPIC_ARN`      | "arn:aws:sns:us-east-1:[YOUR-ACCOUNT-ID]:ConsolidatorWorker-SNS"          | SNS topic ARN for notifications         |

#### SummarizeWorker Variables

File: `src/main/java/emse/cps2/lambda/SummarizeWorker.java`

| Variable Name                   | Value                                                          | Description                           |
| ------------------------------- | -------------------------------------------------------------- | ------------------------------------- |
| `SUMMARIZER_OUTPUT_BUCKET_NAME` | "summarizedtables-cps2-[yourname]"                             | S3 bucket for storing summarized data |
| `SUMMARIZER_OUTPUT_FILE_NAME`   | "summarized_traffic_data.csv"                                  | Name of the output summary file       |
| `SUMMARIZER_SNS_TOPIC_ARN`      | "arn:aws:sns:us-east-1:[YOUR-ACCOUNT-ID]:SummarizedWorker-SNS" | SNS topic ARN for notifications       |

#### ExportClient Variables

File: `src/main/java/emse/cps2/sqs/ExportClient.java`

| Variable Name                   | Value                                                                     | Description                           |
| ------------------------------- | ------------------------------------------------------------------------- | ------------------------------------- |
| `EXPORT_CLIENT_SQS_QUEUE_URL`   | "https://sqs.us-east-1.amazonaws.com/[YOUR-ACCOUNT-ID]/ExportClientQueue" | SQS queue URL for retrieving messages |
| `EXPORT_CLIENT_LOCAL_DIRECTORY` | "/path/to/your/local/directory"                                           | Local directory for downloaded files  |
| `EXPORT_CLIENT_AWS_REGION`      | "us-east-1"                                                               | AWS region for operations             |

#### UploadClient Variables

File: `src/main/java/emse/cps2/UploadClient/UploadS3.java`

| Variable Name           | Value                                  | Description                      |
| ----------------------- | -------------------------------------- | -------------------------------- |
| `UPLOAD_S3_BUCKET_NAME` | "upload-client-cps2-[yourname]"        | S3 bucket for uploading raw data |
| `UPLOAD_S3_FILE_PATH`   | "src/main/resources/data-20221207.csv" | Path to the input data file      |

### Create a `.env` file in your project root and copy the following configuration

Replace the following placeholders:

- `[yourname]`: Your identifier for the S3 buckets
- `/path/to/your/local/directory`: Your local directory path for downloaded files
- `[YOUR-ACCOUNT-ID]`: Your AWS account ID

To get your AWS account ID run:

```bash
aws sts get-caller-identity --query "Account" --output text
```

Copy and paste the code into your `.env` file, and replace the placeholders

```bash
# ConsolidatorWorker Variables
CONSOLIDATOR_OUTPUT_BUCKET_NAME="export-client-cps2-[yourname]"
CONSOLIDATOR_OUTPUT_FILE_NAME="consolidated_traffic_data.csv"
CONSOLIDATOR_SQS_QUEUE_URL="https://sqs.us-east-1.amazonaws.com/[YOUR-ACCOUNT-ID]/ExportClientQueue"
CONSOLIDATOR_SNS_TOPIC_ARN="arn:aws:sns:us-east-1:[YOUR-ACCOUNT-ID]:ConsolidatorWorker-SNS"

# SummarizeWorker Variables
SUMMARIZER_OUTPUT_BUCKET_NAME="summarizedtables-cps2-[yourname]"
SUMMARIZER_OUTPUT_FILE_NAME="summarized_traffic_data.csv"
SUMMARIZER_SNS_TOPIC_ARN="arn:aws:sns:us-east-1:[YOUR-ACCOUNT-ID]:SummarizedWorker-SNS"

# ExportClient Variables
EXPORT_CLIENT_SQS_QUEUE_URL="https://sqs.us-east-1.amazonaws.com/[YOUR-ACCOUNT-ID]/ExportClientQueue"
EXPORT_CLIENT_LOCAL_DIRECTORY="/path/to/your/local/directory"
EXPORT_CLIENT_AWS_REGION="us-east-1"

# UploadClient Variables
UPLOAD_S3_BUCKET_NAME="upload-client-cps2-[yourname]"
UPLOAD_S3_FILE_PATH="src/main/resources/data-20221207.csv"
```

### 5. Lambda Functions Setup

#### SummarizeWorker Lambda

1. Go to AWS Lambda console
2. Create new function:

   - Author from scratch
   - Name: SummarizeWorker
   - Runtime: Java 17
   - Architecture: x86_64
   - Execution role: Use existing role (LabRole)

3. Configure trigger:

   - Add trigger: S3
   - Bucket: upload-client-cps2-[yourname]
   - Event type: All object create events
   - Suffix: .csv

4. Upload code:

   - Upload the compiled JAR file `(target/aws-cloud-1.0-SNAPSHOT.jar)`
   - Set handler: emse.cps2.lambda.SummarizeWorker::handleRequest

5. Configure settings:
   - Memory: 512 MB
   - Timeout: 5 minutes
   - Environment variables:
     - SUMMARIZER_OUTPUT_BUCKET_NAME=summarizedtables-cps2-[yourname]
     - SUMMARIZER_OUTPUT_FILE_NAME=summarized_traffic_data.csv
     - SUMMARIZER_SNS_TOPIC_ARN=[your-sns-topic-arn]

#### ConsolidatorWorker Lambda

Follow the same steps but:

- Function name: ConsolidatorWorker
- Handler: emse.cps2.lambda.ConsolidatorWorker::handleRequest
- Environment variables:
  - CONSOLIDATOR_OUTPUT_BUCKET_NAME=export-client-cps2-[yourname]
  - CONSOLIDATOR_OUTPUT_FILE_NAME=consolidated_traffic_data.csv
  - CONSOLIDATOR_SQS_QUEUE_URL=[your-sqs-queue-url]
  - CONSOLIDATOR_SNS_TOPIC_ARN=[your-sns-topic-arn]

### 6. Testing the Setup

#### Test Upload Client

```bash
# Ensure you have a test CSV file in your resources directory
java -cp target/aws-cloud-1.0-SNAPSHOT.jar emse.cps2.UploadClient.UploadS3
```

#### Test Export Client

```bash
java -cp target/aws-cloud-1.0-SNAPSHOT.jar emse.cps2.sqs.ExportClient
```

### 7. Monitoring and Troubleshooting

- Check CloudWatch logs for Lambda function execution logs
- Monitor SQS queue for message processing
- Check SNS topics for notifications
- S3 buckets for file processing status

### 8. Performance Comparison

The project includes two implementations:

1. Lambda-based implementation (serverless)
2. EC2-based implementation (traditional server)

Performance metrics between these implementations can be found in the project documentation.

1. **CloudWatch Metrics**:

   | Metric           | Description                   |
   | ---------------- | ----------------------------- |
   | Invocation count | Number of function executions |
   | Error count      | Failed executions             |
   | Duration         | Execution time                |
   | Memory usage     | Memory consumption            |

2. **Custom Metrics**:

   | Metric            | Description                     |
   | ----------------- | ------------------------------- |
   | Records processed | Number of records in CSV        |
   | Processing time   | Time to process each file       |
   | Success rate      | Successful vs failed processing |

## Architecture

The system follows this processing flow:

1. Upload Client → S3 Raw Data Bucket
2. S3 Trigger → SummarizeWorker Lambda
3. SummarizeWorker → S3 Processed Data
4. S3 Trigger → ConsolidatorWorker Lambda
5. ConsolidatorWorker → Final Results
6. Export Client → Download Results

## Error Handling

- Failed processes are logged to CloudWatch
- Retry mechanism implemented for transient failures
- Dead Letter Queue (DLQ) for unprocessable messages

## Security Considerations

- Using AWS IAM roles for access control
- Data encryption in transit and at rest
- No hardcoded credentials
- Secure configuration management

## Disclaimer

- **Permissions**: Currently, the project uses limited permissions (via a LabRole). In the future, it is planned to implement an SQS queue between the `UploadS3.java` and `SummarizeWorker` functions, as well as between `SummarizeWorker` and `ConsolidatorWorker`. This will ensure that the Lambda functions can retrieve files from the queue if they are busy. To implement this, you will need to enable full access to SQS for Lambda functions. Please note that this requires appropriate IAM permissions.

## Known Issues & Limitations

- AWS Academy environment has limited IAM permissions
- Session timeouts require re-authentication
- Maximum file size limitations for Lambda processing

## Performance Optimization

- Batch processing implementation
- Caching layer addition
- Resource utilization optimization
- Code optimization

## Future Improvements

- **SQS Queue Between Functions**: There is a plan to implement SQS queues between the Lambda functions to improve reliability and allow for better handling of busy Lambda functions. This will require setting up permissions for Lambda to have full access to SQS.

### Data Source

The IoT traffic data used in this project is collected from geographically distributed IoT devices. Each branch of the enterprise generates a CSV file containing the traffic data. This data can be downloaded from the following location:

[Dataset of Legitimate IoT Data (VARIoT)](https://www.data.gouv.fr/en/datasets/dataset-of-legitimate-iot-data/)

The CSV file contains data such as traffic volumes, device IDs, timestamps, and other relevant information for analyzing the IoT traffic.

---

Feel free to modify the project parameters such as bucket names, file locations, and SNS subscriptions to suit your environment. If you have any questions or need further assistance, don't hesitate to reach out.

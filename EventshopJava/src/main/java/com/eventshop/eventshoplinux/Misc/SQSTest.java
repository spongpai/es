package com.eventshop.eventshoplinux.Misc;

/**
 * Created by siripen on 2/27/16.
 */
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

/**
 * This sample demonstrates how to make basic requests to Amazon SQS using the
 * AWS SDK for Java.
 * <p>
 * <b>Prerequisites:</b> You must have a valid Amazon Web
 * Services developer account, and be signed up to use Amazon SQS. For more
 * information on Amazon SQS, see http://aws.amazon.com/sqs.
 * <p>
 * Fill in your AWS access credentials in the provided credentials file
 * template, and be sure to move the file to the default location
 * (~/.aws/credentials) where the sample code will load the credentials from.
 * <p>
 * <b>WARNING:</b> To avoid accidental leakage of your credentials, DO NOT keep
 * the credentials file in your source directory.
 */
public class SQSTest {

    public static void main(String[] args) throws Exception {

        /*
         * The ProfileCredentialsProvider will return your [default]
         * credential profile by reading from the credentials file located at
         * (~/.aws/credentials).
         */
        AWSCredentials credentials = null;
        try {
            //String profile = "siripen";
            String profile = "default";
            credentials = new ProfileCredentialsProvider(profile).getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        AmazonSQS sqs = new AmazonSQSClient(credentials);
        Region usWest2 = Region.getRegion(Regions.US_WEST_2);
        sqs.setRegion(usWest2);


        System.out.println("===========================================");
        System.out.println("Getting Started with Amazon SQS");
        System.out.println("===========================================\n");

        try {
            // Create a queue
            //System.out.println("Creating a new SQS queue called MyQueue.\n");
            //CreateQueueRequest createQueueRequest = new CreateQueueRequest("io-krumbs-sdk-mediajson_siripen");
            //String myQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
            String myQueueUrl = "'";

            // List queues
            System.out.println("Listing all queues in your account.\n");
            //String myQueueUrl = "";
            for (String queueUrl : sqs.listQueues().getQueueUrls()) {
                System.out.println("  QueueUrl: " + queueUrl);
                myQueueUrl = queueUrl;
            }
            System.out.println();
            /*
  QueueUrl: https://sqs.us-west-2.amazonaws.com/636414996916/io-krumbs-sdk-mediajson
  QueueUrl: https://sqs.us-west-2.amazonaws.com/636414996916/io-krumbs-sdk-mediajson_multithemedkrumbs
  QueueUrl: https://sqs.us-west-2.amazonaws.com/636414996916/io-krumbs-sdk-mediajson_places-ngss-uci
  QueueUrl: https://sqs.us-west-2.amazonaws.com/636414996916/io-krumbs-sdk-mediajson_siripen
  QueueUrl: https://sqs.us-west-2.amazonaws.com/636414996916/io-krumbs-sdk-mediajson_smartcities-pspl-in
  QueueUrl: https://sqs.us-west-2.amazonaws.com/636414996916/io-krumbs-sdk-mediajson_uciconnect-ngss-uci
  QueueUrl: https://sqs.us-west-2.amazonaws.com/636414996916/io-krumbs-sdk-mediajson_uciphotos-ngss-uci
  QueueUrl: https://sqs.us-west-2.amazonaws.com/636414996916/io-krumbs-sdk-mediajson_viznotes-ngss-uci
             */
            //System.exit(0);

            myQueueUrl = "https://sqs.us-west-2.amazonaws.com/636414996916/io-krumbs-sdk-mediajson_siripen";  // krumbs aws
            //myQueueUrl = "https://sqs.us-west-2.amazonaws.com/139637734145/MyQueue";  // siripen aws

            // get queue attributes
            GetQueueAttributesRequest qar = new GetQueueAttributesRequest( myQueueUrl );
            qar.setAttributeNames( Arrays.asList( "ApproximateNumberOfMessages"));
            Map map = sqs.getQueueAttributes( qar ).getAttributes();
            Iterator it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                System.out.println(pair.getKey() + " = " + pair.getValue());
                it.remove(); // avoids a ConcurrentModificationException
            }
            //System.exit(0);
            // Send a message
            /*
            System.out.println("Sending a message to MyQueue.\n");
            String filePath = "http://eventshop.ics.uci.edu/krumbsList.json";
            URL url = new URL(filePath);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream()));
            StringBuilder str = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                str.append(inputLine);
            in.close();
            JsonParser parser = new JsonParser();
            JsonArray krumbsArr = parser.parse(str.toString()).getAsJsonArray();
            for(int i = 0; i < krumbsArr.size(); i++)
                sqs.sendMessage(new SendMessageRequest(myQueueUrl, krumbsArr.get(i).getAsJsonObject().toString()));
            System.exit(0);
            */

            // Receive messages
            System.out.println("Receiving messages from MyQueue.\n");
            //myQueueUrl = "https://sqs.us-west-1.amazonaws.com/139637734145/MyFirstQueue";
            int count = 0;
            while(true && count < 10){


                ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl);
                List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();

                try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("/tmp/test_sqs.txt", true)))) {

                   for (Message message : messages) {
                        System.out.println("  Message");
                        System.out.println("    MessageId:     " + message.getMessageId());
                        System.out.println("    ReceiptHandle: " + message.getReceiptHandle());
                        System.out.println("    MD5OfBody:     " + message.getMD5OfBody());
                        System.out.println("    Body:          " + message.getBody());
                        for (Entry<String, String> entry : message.getAttributes().entrySet()) {
                            System.out.println("  Attribute");
                            System.out.println("    Name:  " + entry.getKey());
                            System.out.println("    Value: " + entry.getValue());
                        }
                        out.println(message.getBody());
                        //break;
                        count++;
                    }
                }catch (IOException e) {
                    //exception handling left as an exercise for the reader
                }
            }
            System.out.println();
            //System.exit(0);


            // Delete a message
            //System.out.println("Deleting a message.\n");
            //String messageReceiptHandle = messages.get(0).getReceiptHandle();
            //sqs.deleteMessage(new DeleteMessageRequest(myQueueUrl, messageReceiptHandle));

            // Delete a queue
            //System.out.println("Deleting the test queue.\n");
            //sqs.deleteQueue(new DeleteQueueRequest(myQueueUrl));
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it " +
                    "to Amazon SQS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered " +
                    "a serious internal problem while trying to communicate with SQS, such as not " +
                    "being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }
}

package com.eventshop.eventshoplinux.camel;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.eventshop.eventshoplinux.domain.datasource.DataSource;
import com.eventshop.eventshoplinux.lifecycle.DynamicRouteAdder;
import com.eventshop.eventshoplinux.util.commonUtil.CommonUtil;
import com.eventshop.eventshoplinux.util.commonUtil.Config;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by nandhiniv on 7/26/15.
 */
public class DataFormatRoute extends RouteBuilder {
    private final static Logger LOGGER = LoggerFactory.getLogger(DataFormatRoute.class);

    @Override
    public void configure() throws Exception {
        /**
         * Route to read the file and send to {} endpoint
         */
        from("direct:readFromFile")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        DataSource ds = exchange.getIn().getHeader("datasource", DataSource.class);
                        String filePath = ds.getUrl();
                        URL url = new URL(filePath);

                        exchange.getOut().setBody(url.openStream());
                    }
                })
                .to("direct:dataType");

        from("direct:readFromSQS")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        DataSource ds = exchange.getIn().getHeader("datasource", DataSource.class);
                        JsonParser parser = new JsonParser();
                        JsonObject jObj = parser.parse(ds.getWrapper().getWrprKeyValue()).getAsJsonObject();
                        String profile ="default";
                        if(jObj.has("aws_profile"))
                            profile = jObj.get("aws_profile").getAsString();
                        //long wait = 300000L;    // wait for 5 mins and try again

                        AWSCredentials credentials = null;
                        try {
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

                        String queueUrl = ds.getUrl();

                        //String filePath = ds.getUrl();
                        //URL url = new URL(filePath);
                        LOGGER.info("read from SQS");
                        //JsonArray messageArray = new JsonArray();
                        //JsonParser parser = new JsonParser();

                        boolean emptyQueye = false;
                        List<String> messagesBody = new ArrayList<String>();
                        while( (messagesBody.size() == 0) || !emptyQueye) {
                            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
                            List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
                            if(messages.size() > 0) {
                                try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(Config.getProperty("tempDir") + "ds" + ds.getSrcID() + ".txt", true)))) {
                                    for (Message message : messages) {
                                        out.println(message.getBody());
                                        messagesBody.add(message.getBody());
                                        //LOGGER.info("get message body" + message.getBody());
                                        // delete message from queue after received and wrote to file
                                        String messageReceiptHandle = message.getReceiptHandle();
                                        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));

                                    }
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
                            } else{
                                emptyQueye = true;
                                if(messagesBody.size() == 0) {
                                    LOGGER.info("seems to be no message in queue");
                                    break;
                                    //Thread.sleep(300000);   // sleep for 5 minutes before trying again
                                } else{
                                    LOGGER.info("seems to be no more message in the queue, let populate messages we have so far into kafka");
                                    break;
                                }
                            }
                        }

                        if(messagesBody.size() == 1){
                            exchange.getOut().setHeader("sqsList", false);
                            LOGGER.info("send one message from sws: ");// + messagesBody.get(0));
                            exchange.getOut().setBody(messagesBody.get(0));

                        } else if(messagesBody.size() > 1){
                            exchange.getOut().setHeader("sqsList", true);
                            String joined = "[" + String.join(",", messagesBody) + "]";
                            LOGGER.info("send list of messages from sqs: " + joined);
                            exchange.getOut().setBody(joined);
                        } else{
                            exchange.getOut().setHeader("sqsList", false);
                            LOGGER.info("no message from sws: ");
                            exchange.getOut().setBody("{}");
                        }
                    }
                })
                .to("direct:dataType");

        from("direct:readFromRest")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        DataSource ds = exchange.getIn().getHeader("datasource", DataSource.class);

                        String wrapperKeyValue = ds.getWrapper().getWrprKeyValue();
                        JsonParser parser = new JsonParser();
                        JsonObject wrapperObj = (JsonObject)parser.parse(wrapperKeyValue);
                        String baseUrl = ds.getUrl().replace("\"", "");
                        if (wrapperObj.has("queryParams")) {
                            baseUrl = baseUrl + "?";
                            JsonObject queryParams = (JsonObject) wrapperObj.getAsJsonArray("queryParams").get(0);

                            Set<Map.Entry<String, JsonElement>> entrySet = queryParams.entrySet();
                            for (Map.Entry<String, JsonElement> entry : entrySet) {
                                LOGGER.debug(entry.getKey() + " : " + entry.getValue());
                                baseUrl = baseUrl + entry.getKey() + "=" + entry.getValue().getAsString().replaceAll("\"", "") + "&";
                            }
                        }

                        URL obj = new URL(baseUrl);
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                        // optional default is GET
                        con.setRequestMethod("GET");

                        //add request header

                        int responseCode = con.getResponseCode();
                        LOGGER.info("\nSending 'GET' request to URL : " + baseUrl);
                        LOGGER.info("Response Code : " + responseCode);

                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(con.getInputStream()));
                        String inputLine;
                        StringBuffer response = new StringBuffer();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        //print result
                        LOGGER.debug(response.toString());


                        exchange.getOut().setBody(response);
                    }
                })
                .to("direct:dataType");

        from("direct:toTwitterSearchAndStreaming")
                .to("direct:dataType");
//                .to("direct:kafkaConsumerCreation")
//                .to("direct:twitterSearch")
        //This should be changed to twitterMongo
        ;


        from("direct:dataType")
                .to("direct:checkDirectLoad")
                .to("direct:kafkaConsumerCreation")
                .to("direct:mongoCollectionCreation")
                .choice()
                .when(header("directLoad").isNotEqualTo("true"))
                .choice()
                .when(header("dsType").isEqualTo("csvField"))
                .to("direct:toCSVPath")
                .when(header("dsType").isEqualTo("xml"))
                .to("direct:toXPath")
                .when(header("dsType").isEqualTo("json"))
                .to("direct:toJsonPath")
                .when(header("dsType").isEqualTo("mjson"))
                .to("direct:toMediaJsonPath")
                .when(header("dsType").isEqualTo("Twitter"))
                .to("direct:twitterSearch")
                .when(header("dsType").isEqualTo("visual"))
                .to("direct:toVisual")
                .end()
                .end()

                .delay(Integer.parseInt(Config.getProperty("mongoReadDelay")))
                .to("direct:commonQueryMongo")

        ;


        from("direct:checkDirectLoad")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        exchange.getOut().setBody(exchange.getIn().getBody());
                        DataSource ds = exchange.getIn().getHeader("datasource", DataSource.class);
                        JsonParser parser = new JsonParser();
                        System.out.println("***********************************************************\n" + ds.getWrapper().getWrprKeyValue());
                        JsonObject jObj = parser.parse(ds.getWrapper().getWrprKeyValue().trim()).getAsJsonObject();
                        boolean isDirectLoad=false;
                        if (jObj.has("directLoad")){
                            isDirectLoad= jObj.get("directLoad").getAsBoolean();
                        }

                        if (isDirectLoad){
                            exchange.getOut().setHeader("directLoad", true);
                        }
                    }
                });


        from("direct:mongoCollectionCreation")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {

                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        exchange.getOut().setBody(exchange.getIn().getBody());
                        DataSource ds = exchange.getIn().getHeader("datasource", DataSource.class);
                        String dsId = ds.getSrcID();
                        //DB db = checkConnection();
                        DB db = CommonUtil.connectMongoDB();
                        LOGGER.info("Trying to index...");
                        boolean collectionExists = db.collectionExists("ds" + dsId);
                        if (collectionExists == false) {
                            try {
                                db.createCollection("ds" + dsId, null);

                                DBCollection col = db.getCollection("ds" + ds.getSrcID());
                                col.createIndex(new BasicDBObject("timeStamp", -1));
                                //col.ensureIndex(new BasicDBObject("timeStamp",-1),"sparse",true);

                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            LOGGER.info("Indexing done... ");
                        } else {
                            LOGGER.info("No indexing needed...");
                        }

                    }
            });


/**
 * Create kafka consumet and redirect to dsType endpoint
 */
        from("direct:kafkaConsumerCreation")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        exchange.getOut().setBody(exchange.getIn().getBody());

                        DataSource ds = exchange.getIn().getHeader("datasource", DataSource.class);
                        String dsId = "ds" + ds.srcID;
                        String uri = "kafka:" + Config.getProperty("kafkaURI") + "?topic=" + dsId + "&zookeeperHost="
                                + Config.getProperty("zkHostName") + "&zookeeperPort=" + Config.getProperty("zkPort")
                                + "&batchSize=" + Integer.parseInt(Config.getProperty("kafkaReadBatchSize")) + "&groupId=group1";
                        CamelContext context = getContext();
                        LOGGER.info("Kafka Started at : "+ new Date());
                        //Check if already exists
                        if (context.hasEndpoint(uri) == null) {
                            String to;
//                            .when(header("directLoad").isEqualTo("true"))
                            Boolean directLoad = exchange.getIn().getHeader("directLoad", Boolean.class);
                            if (directLoad != null) {
                                if (directLoad) {
                                    to = "direct:" + "directLoad";
                                    exchange.getOut().setHeader("dsType", "directLoad");
                                    context.addRoutes(new DynamicRouteAdder(context, uri, to));

                                }
                            } else {
                                to = "direct:" + exchange.getIn().getHeader("dsType");
                                System.out.println("direct from kafka consumer to mongodb: dsType:" + to);
                                context.addRoutes(new DynamicRouteAdder(context, uri, to));

                            }
                        }
                    }
                });
    }
    /*
    private static MongoClient mongoClient = null;
    private static DB db = null;
    private static DB checkConnection() throws UnknownHostException {
        if(mongoClient == null)
            mongoClient = new MongoClient(Config.getProperty("mongoHost"), Integer.parseInt(Config.getProperty("mongoPort")));
        if(db == null){
            db = (mongoClient.getDB(Config.getProperty("mongoDB")));
        }
        return db;
    }
    private static void closeMongoClient(){
        if(mongoClient != null)
            mongoClient.close();
    }
    */
}

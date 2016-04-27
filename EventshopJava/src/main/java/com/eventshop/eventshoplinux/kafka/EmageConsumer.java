package com.eventshop.eventshoplinux.kafka;

/**
 * Created by siripen on 3/10/16.
 */

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.message.MessageAndOffset;

public class EmageConsumer extends  Thread {
    //final static String clientId = "SimpleConsumerDemoClient";
    final static String TOPIC = "emagepy";
    ConsumerConnector consumerConnector;
    Map<String, Integer> topicCountMap;
    KafkaStream<byte[], byte[]> stream;

    public static void main(String[] argv) throws UnsupportedEncodingException {
        EmageConsumer helloKafkaConsumer = new EmageConsumer();
        helloKafkaConsumer.start();
    }

    public EmageConsumer(){
        Properties properties = new Properties();
        properties.put("zookeeper.connect","localhost:2181");
        properties.put("group.id","test-group");
        ConsumerConfig consumerConfig = new ConsumerConfig(properties);
        consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);
        topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(TOPIC, new Integer(1));
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumerConnector.createMessageStreams(topicCountMap);
        stream =  consumerMap.get(TOPIC).get(0);
    }

    public EmageConsumer(String topic){
        Properties properties = new Properties();
        properties.put("zookeeper.connect","localhost:2181");
        properties.put("group.id","test-group");
        ConsumerConfig consumerConfig = new ConsumerConfig(properties);
        consumerConnector = Consumer.createJavaConsumerConnector(consumerConfig);
        topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, new Integer(1));
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumerConnector.createMessageStreams(topicCountMap);
        stream =  consumerMap.get(topic).get(0);

    }

    @Override
    public void run() {
        ConsumerIterator<byte[], byte[]> it = stream.iterator();
        while(it.hasNext()) {
            String msg = new String(it.next().message());
            //long produce = Long.parseLong(msg.substring(msg.length() - 13));
            JsonParser parser = new JsonParser();
            JsonObject msg_json = parser.parse(msg).getAsJsonObject();
            long produce = msg_json.get("endTime").getAsLong();
            long receive = System.currentTimeMillis();
            long delay = receive - produce;
            System.out.println(msg + " " + produce + " " + receive + " " + delay);
        }

    }

    private static void printMessages(ByteBufferMessageSet messageSet) throws UnsupportedEncodingException {
        for(MessageAndOffset messageAndOffset: messageSet) {
            ByteBuffer payload = messageAndOffset.message().payload();
            byte[] bytes = new byte[payload.limit()];
            payload.get(bytes);
            System.out.println(new String(bytes, "UTF-8"));
        }
    }
}
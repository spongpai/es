package com.eventshop.eventshoplinux.kafka;

import com.eventshop.eventshoplinux.domain.common.FrameParameters;
import com.eventshop.eventshoplinux.domain.datasource.emage.Emage;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Random;

/**
 * Created by siripen on 3/14/16.
 */
public class EmageTest {

    Producer<String,String> producer;

    public EmageTest(){
        Properties properties = new Properties();
        properties.put("metadata.broker.list","localhost:9092");
        properties.put("serializer.class","kafka.serializer.StringEncoder");
        ProducerConfig producerConfig = new ProducerConfig(properties);
        producer = new Producer<String, String>(producerConfig);
    }

    public void produce(String topic, Emage emage){

        KeyedMessage<String, String> message =new KeyedMessage<String, String>
                    (topic, emage.toJson().toString());
        producer.send(message);

    }


    public static void main(String[] args){
        String inputTopic = "emagetest";
        String outputTopic = "emagepy";

        EmageConsumer consumer = new EmageConsumer(outputTopic);
        consumer.start();
        EmageProducer producer = new EmageProducer();

        FrameParameters params = new FrameParameters();
        params.setDefaultValues();
        Emage emage = new Emage(params, "testpy");
        double[][] sampleImage = new double[params.getNumOfRows()][params.getNumOfColumns()];

        for(int i = 0; i < 10; i++){

            for(int x = 0; x < sampleImage.length; x++){
                for(int y = 0; y < sampleImage[0].length; y++){
                    Random rand = new Random();
                    sampleImage[x][y] = rand.nextInt(50) + 1;
                }
            }
            emage.setImage(sampleImage);
            emage.setStart(System.currentTimeMillis());
            emage.setEnd(emage.getStartTime().getTime() + 5000);
            System.out.println(i + ", " + emage.toJson().toString());
            producer.produce(inputTopic, emage);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }
}

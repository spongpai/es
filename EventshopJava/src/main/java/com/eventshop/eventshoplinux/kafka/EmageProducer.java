package com.eventshop.eventshoplinux.kafka;

/**
 * Created by siripen on 3/10/16.
 */
import java.math.BigDecimal;
import java.math.MathContext;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import com.eventshop.eventshoplinux.domain.common.FrameParameters;
import com.eventshop.eventshoplinux.domain.datasource.emage.Emage;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

public class EmageProducer {
    final static String TOPIC = "emagetest";

    Producer<String,String> producer;
    public EmageProducer(){
        Properties properties = new Properties();
        properties.put("metadata.broker.list","localhost:9092");
        properties.put("serializer.class","kafka.serializer.StringEncoder");
        ProducerConfig producerConfig = new ProducerConfig(properties);
        producer = new Producer<String, String>(producerConfig);

    }
    public void produce(String topic, Emage emage){


        KeyedMessage<String, String> message =new KeyedMessage<String, String>
                (topic, emage.toJson().toString());
        //(TOPIC,"java:" + sdf.format(new Date()));
        producer.send(message);
    }

    public void close(){
        producer.close();
    }

    public static void main(String[] argv){
        FrameParameters params = new FrameParameters();
        params.setDefaultValues();

        Emage emage = new Emage(params, "testpy");
        double[][] sampleImage = new double[params.getNumOfRows()][params.getNumOfColumns()];
        MathContext context = new MathContext(5);
        EmageProducer producer = new EmageProducer();
        for(int i = 0; i < 10; i++) {
            Random rand = new Random();
            double latitude = rand.nextInt(10) + 10;
            double longitude = rand.nextInt(20) + 20;
            int row = params.getNumOfRows()
                    - 1
                    - (int) Math.floor(Math.abs((BigDecimal
                    .valueOf(latitude))
                    .subtract(BigDecimal.valueOf(params.swLat),
                            context)
                    .divide(BigDecimal.valueOf(params.latUnit),
                            context).doubleValue()));
            int col = (int) Math.floor(Math.abs((BigDecimal
                    .valueOf(longitude))
                    .subtract(BigDecimal.valueOf(params.swLong),
                            context)
                    .divide(BigDecimal.valueOf(params.longUnit),
                            context).doubleValue()));
            System.out.println(params.swLat + "lat,row:" + latitude + "," + row);
            System.out.println(params.swLong + "lng,col:" + longitude + "," + col);
        }
        for(int i = 0; i < 1; i++) {
            for (int x = 0; x < sampleImage.length; x++) {
                for (int y = 0; y < sampleImage[0].length; y++) {
                    Random rand = new Random();
                    sampleImage[x][y] = rand.nextInt(50) + 1;
                }
            }
            emage.setImage(sampleImage);
            emage.setStart(System.currentTimeMillis());
            emage.setEnd(emage.getStartTime().getTime() + 5000);
            producer.produce(TOPIC, emage);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

}

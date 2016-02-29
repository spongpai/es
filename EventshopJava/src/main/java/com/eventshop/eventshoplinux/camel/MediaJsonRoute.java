package com.eventshop.eventshoplinux.camel;

import com.eventshop.eventshoplinux.domain.datasource.DataSource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nandhiniv on 6/29/15.
 */
public class MediaJsonRoute extends RouteBuilder {

    private final static Logger LOGGER = LoggerFactory.getLogger(MediaJsonRoute.class);

    @Override
    public void configure() throws Exception {

        /**
         * Redirect based on isList=true
         */
        from("direct:toMediaJsonPath")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        System.out.println("media JSON: inside media json process" );
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());

                        String body = exchange.getIn().getBody(String.class);
                        //LOGGER.info("tomediajsonpath: setbody: " + body.toString());
                        exchange.getOut().setBody(body);

                        DataSource ds = exchange.getIn().getHeader("datasource", DataSource.class);
                        exchange.getOut().setHeader("dataSource", ds);
                        JsonParser parser = new JsonParser();
                        JsonObject jObj = parser.parse(ds.getWrapper().getWrprKeyValue()).getAsJsonObject();

                        //Check if it is a list and split the list before loading to kafka
                        JsonElement isList1 = jObj.get("isList");
                        Boolean isList = false;
                        if (isList1 != null) {
                            LOGGER.info("isList is true");
                            isList = isList1.getAsBoolean();
                        }
                        Object sqsList = exchange.getIn().getHeader("sqsList"); // sqsList is overwirte isList
                        if(sqsList != null){
                            if((boolean)sqsList){
                                LOGGER.info("sqsList is true");
                                isList = true;
                            } else{
                                isList = false;
                            }
                        }

                        String tokenizeElement = "";
                        String rootElement = "";
                        if (isList) {
                            if (jObj.has("rootElement") && !jObj.get("rootElement").isJsonNull()) {
                                rootElement = jObj.get("rootElement").getAsString();
                            }
                            if (jObj.has("tokenizeElement") && !jObj.get("tokenizeElement").isJsonNull()) {
                                tokenizeElement = jObj.get("tokenizeElement").getAsString();
                            }
                            exchange.getOut().setHeader("isList", true);
                            exchange.getOut().setHeader("tokenizeElement", tokenizeElement);
                            exchange.getOut().setHeader("rootElement", rootElement);
                        } else{
                            exchange.getOut().setHeader("isList", false);
                        }
                    }
                })
                .choice()
                .when(header("isList").isEqualTo(true))
                .to("direct:mediaJsonSplitList")
                .otherwise()
                .to("direct:populateKafka")
        ;
        /**
         Route to split the list and tokenize it before populating kafka
         */
        from("direct:mediaJsonSplitList")
//                .split(body().tokenizeXML("item", "cities/list/")).streaming()
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        System.out.println("inside mediaJsonSplitList");
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        String body = exchange.getIn().getBody(String.class);

                        JsonArray jsonElements = null;
                        JsonParser parser = new JsonParser();
                        final JsonElement parse = parser.parse(body);
                        jsonElements = parse.getAsJsonArray();

                        LOGGER.info("Successfully split media json list. List has {} elements", jsonElements.size());
                        exchange.getOut().setBody(jsonElements);

                    }
                })
                .split().body().streaming()
                .to("direct:populateKafka")
        ;


    }
}

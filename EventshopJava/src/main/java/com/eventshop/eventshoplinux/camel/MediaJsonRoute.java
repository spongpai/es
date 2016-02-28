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
                        LOGGER.info("tomediajsonpath: setbody: " + body.toString());
                        exchange.getOut().setBody(body);

                        /*
                        // convert to json array before parsing

                        JsonArray jsonElements = new JsonArray();
                        List<String> element = new ArrayList();
                        final JsonElement parse = parser.parse(body);
                        jsonElements.add(parse);

                        LOGGER.info("tomediajsonpath: setbody: " + jsonElements.toString());
                        exchange.getOut().setBody(jsonElements);
*/
                        DataSource ds = exchange.getIn().getHeader("datasource", DataSource.class);
                        exchange.getOut().setHeader("dataSource", ds);
                        JsonParser parser = new JsonParser();

                        JsonObject jObj = parser.parse(ds.getWrapper().getWrprKeyValue()).getAsJsonObject();
                        String dsType = jObj.get("datasource_type").getAsString();
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
                        String tokenizeElement = exchange.getIn().getHeader("tokenizeElement", String.class);
                        String rootElement = exchange.getIn().getHeader("rootElement", String.class);

                        JsonParser parser = new JsonParser();
                        final JsonElement parse = parser.parse(body);
                        JsonArray jsonElements = null;
                        if (parse.isJsonObject()) {

                            JsonObject jsonObj = parse.getAsJsonObject();


                            String root = "";
                            if (!rootElement.isEmpty()) {
//                            root = String.valueOf(JsonPath.read(body, ("$." + rootElement)));
                                root = jsonObj.get(rootElement).getAsString();

                            } else {
                                root = body;
                            }

                            List<String> element = new ArrayList();

                            if (!tokenizeElement.isEmpty()) {
//                            element = JsonPath.read(root, ("$." + tokenizeElement));
                                jsonElements = jsonObj.get(tokenizeElement).getAsJsonArray();

                            }
                        } else {
                            jsonElements = parse.getAsJsonArray();

                        }

                        LOGGER.info("Successfully split the list. List has {} elements", jsonElements.size());
                        exchange.getOut().setBody(jsonElements);

                    }
                })
                .split().body().streaming()
                .to("direct:populateKafka")
        ;


    }
}

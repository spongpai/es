package com.eventshop.eventshoplinux.camel;

import com.eventshop.eventshoplinux.DAO.datasource.DataSourceManagementDAO;
import com.eventshop.eventshoplinux.DAO.rule.RuleDao;
import com.eventshop.eventshoplinux.akka.query.message.MongoQueryMessage;
import com.eventshop.eventshoplinux.domain.datasource.DataSource;
import com.eventshop.eventshoplinux.model.ELocation;
import com.eventshop.eventshoplinux.model.MongoResponse;
import com.eventshop.eventshoplinux.ruleEngine.ApplyRule;
import com.eventshop.eventshoplinux.ruleEngine.Rule;
import com.eventshop.eventshoplinux.ruleEngine.Rules;
import com.eventshop.eventshoplinux.util.commonUtil.CommonUtil;
import com.eventshop.eventshoplinux.util.commonUtil.Config;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.QueryBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by aravindh on 5/8/15.
 */
public class MongoRoute extends RouteBuilder {

    private final static Logger LOGGER = LoggerFactory.getLogger(MongoRoute.class);

    @Override
    public void configure() throws Exception {


        from("direct:commonQueryMongo")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        DataSource dataSource = exchange.getIn().getHeader("datasource", DataSource.class);
                        LOGGER.info("commonQueryMongo\n" + dataSource.toString());


                        String timeType = dataSource.getInitParam().getTimeType();

                        double nelat = dataSource.getInitParam().getNeLat();
                        double nelong = dataSource.getInitParam().getNeLong();
                        double swlat = dataSource.getInitParam().getSwLat();
                        double swlong = dataSource.getInitParam().getSwLong();
                        double latUnit = dataSource.getInitParam().getLatUnit();
                        double longUnit = dataSource.getInitParam().getLongUnit();


                        long timeWindow = dataSource.getInitParam().getTimeWindow();
                        long syncAt = dataSource.getInitParam().getSyncAtMilSec();
                        long endTimeToCheck = 0;
                        long timeToCheck = 0;

                        if (timeType.equalsIgnoreCase("0")) {
                            endTimeToCheck = System.currentTimeMillis();
                            timeToCheck = endTimeToCheck - timeWindow;
                        } else if (timeType.equalsIgnoreCase("1")) {
                            timeToCheck = System.currentTimeMillis();
                            endTimeToCheck = timeToCheck + timeWindow;
                        }

                        JsonParser parser = new JsonParser();
                        JsonObject jObj = parser.parse(dataSource.getWrapper().getWrprKeyValue()).getAsJsonObject();
                        String sptlWrpr = jObj.get("spatial_wrapper").getAsString();

                        exchange.getOut().setHeader("endTimeToCheck", endTimeToCheck);
                        exchange.getOut().setHeader("dataSource", dataSource);
                        exchange.getOut().setHeader("datasource", dataSource);
                        exchange.getOut().setHeader("timeToCheck", timeToCheck);
                        exchange.getOut().setHeader("nelat", nelat);
                        exchange.getOut().setHeader("nelong", nelong);
                        exchange.getOut().setHeader("swlat", swlat);
                        exchange.getOut().setHeader("swlong", swlong);
                        exchange.getOut().setHeader("latUnit", latUnit);
                        exchange.getOut().setHeader("longUnit", longUnit);
                        exchange.getOut().setHeader("spatial_wrapper", sptlWrpr);
                        exchange.getOut().setHeader("timeWindow", timeWindow);
                        exchange.getOut().setHeader("syncAt", syncAt);
                    }
                })
                .to("direct:commonQuery")
        ;


        from("direct:commonQuery")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        exchange.getOut().setHeader("c_id", exchange.getProperty(Exchange.CORRELATION_ID));

                        DataSource ds = exchange.getIn().getHeader("datasource", DataSource.class);
                        long endTimeToCheck = exchange.getIn().getHeader("endTimeToCheck", Long.class);
                        long startTimeToCheck = exchange.getIn().getHeader("timeToCheck", Long.class);
                        double nelat = exchange.getIn().getHeader("nelat", Double.class);
                        double nelong = exchange.getIn().getHeader("nelong", Double.class);
                        double swlat = exchange.getIn().getHeader("swlat", Double.class);
                        double swlong = exchange.getIn().getHeader("swlong", Double.class);

                        LOGGER.info("Time intervals : {} , {} ", startTimeToCheck, endTimeToCheck);
//                        JsonParser jsonParser = new JsonParser();
//                        JsonObject dsQuery = (JsonObject) jsonParser.parse(ds.getInitParam().getDsQuery());
//                        String query = dsQuery.get("query").getAsString();
//                        JsonObject queryParams = dsQuery.get("queryParams").getAsJsonObject();

//                        String query = ds.getInitParam().getDsQuery();
//
//                        query = query.replace("$swlat", "" + ds.getInitParam().getSwLat());
//                        query = query.replace("$swlong", "" + ds.getInitParam().getSwLong());
//                        query = query.replace("$nelat", "" + ds.getInitParam().getNeLat());
//                        query = query.replace("$nelong", "" + ds.getInitParam().getNeLong());
//                        query = query.replace("$swlat", "" + startTimeToCheck);
//                        query = query.replace("$swlat", "" + endTimeToCheck);

                        //String query = "{ $and: [ {loc: { $geoWithin: { $box:  [ [ " + swlong + ", " +
                        //        +swlat + "], [ " + nelong + ", "
                        //        + nelat + " ] ]}}}, {timestamp : { $gt : " + (startTimeToCheck) + ", $lt : " + (endTimeToCheck) + " }} ] }";


                        String query = "{ $and: [ {stt_where: { $geoWithin: { $box:  [ [ " + swlong + ", " +
                                +swlat + "], [ " + nelong + ", "
                                + nelat + " ] ]}}}, {timestamp : { $gt : " + (startTimeToCheck) + ", $lt : " + (endTimeToCheck) + " }} ] }";


//                        Map<String, Object> attributes = new HashMap<String, Object>();
//                        Set<Map.Entry<String, JsonElement>> entrySet = queryParams.entrySet();
//                        for(Map.Entry<String,JsonElement> entry : entrySet){
//                            System.out.println(entry.getKey());
//                            System.out.println(entry.getValue());
//                            query=query.replace(entry.getKey(),entry.getValue().toString());
//
//                        }
//
//
//                        query=query.replace("startTimeToCheck",""+startTimeToCheck);
//                        query=query.replace("endTimeToCheck",""+endTimeToCheck);


                        LOGGER.info("Query : {}", query);

                        String dsId = ds.getSrcID();
                        String mongoPath = "mongodb:mongoBean?database=" + Config.getProperty("DSDB") + "&collection=ds" + dsId + "&operation=findAll";
                        exchange.getOut().setHeader("mPath", mongoPath);

                        exchange.getOut().setBody(query);
                        LOGGER.info(mongoPath);
                        LOGGER.info("commonQuery Query Start TIme:" + System.currentTimeMillis());

                    }
                })
                .convertBodyTo(String.class)
                .recipientList(header("mPath"))
                .to("direct:applySpatialWrapper");


        from("direct:applyRule")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        //Get the rule from DB

                        MongoQueryMessage mongoQueryMessage = exchange.getIn().getBody(MongoQueryMessage.class);

                        exchange.getOut().setHeader("endTimeToCheck", mongoQueryMessage.getEndTimeToFilter());
                        exchange.getOut().setHeader("timeToCheck", mongoQueryMessage.getTimeToFilter());
                        exchange.getOut().setHeader("nelat", mongoQueryMessage.getNelat());
                        exchange.getOut().setHeader("nelong", mongoQueryMessage.getNelong());
                        exchange.getOut().setHeader("swlat", mongoQueryMessage.getSwlat());
                        exchange.getOut().setHeader("swlong", mongoQueryMessage.getSwlong());
                        exchange.getOut().setHeader("latUnit", mongoQueryMessage.getLatUnit());
                        exchange.getOut().setHeader("longUnit", mongoQueryMessage.getLonUnit());
                        exchange.getOut().setHeader("spatial_wrapper", mongoQueryMessage.getSpatial_wrapper());

                        RuleDao ruleDao = new RuleDao();
                        Rules rules = ruleDao.getRules(mongoQueryMessage.getDataSourceID());
                        //System.out.println("RuleId: "+ rules.getRuleID());
                        //System.out.println("Rules:-1 : " + rules.toString());


                        DataSourceManagementDAO dataSourceManagementDAO = new DataSourceManagementDAO();
                        String source = rules.getSource();
                        source = source.replace("ds", "");

                        exchange.getOut().setHeader("datasource", dataSourceManagementDAO.getDataSource(Integer.parseInt(source)));

                        StringTokenizer st;

                        //Process the Rule and create the Emage
                        Mongo mongoConn = new Mongo(Config.getProperty("mongoHost"), Integer.parseInt(Config.getProperty("mongoPort")));
                        DB mongoDb = mongoConn.getDB("events");
                        DBCollection collection = mongoDb.getCollection(rules.getSource());
                        // Building the query parameters from Rules
                        QueryBuilder query = new QueryBuilder();
                        List<Rule> rulesList = rules.getRules();//TODO
                        //System.out.println("Rule List: "+rulesList.toString());
                        if (rulesList == null || rulesList.isEmpty()) {
                            rulesList = new ArrayList<Rule>();
                        }

                        // Adding the bounding box and timestamp Rule by default as Eventshop is always considered Geo-Spatial
                        Rule geoLocationRule = new Rule("loc", "coordinates",
                                String.valueOf(mongoQueryMessage.getSwlong()) + "," +
                                        String.valueOf(mongoQueryMessage.getSwlat()) + "," +
                                        String.valueOf(mongoQueryMessage.getNelong()) + "," +
                                        String.valueOf(mongoQueryMessage.getNelat()));

                        Rule startTimeStampRule = new Rule("timestamp", ">", String.valueOf(mongoQueryMessage.getTimeToFilter()));
                        Rule endTimeStampRule = new Rule("timestamp", "<", String.valueOf(mongoQueryMessage.getEndTimeToFilter()));

//                        rulesList.add(geoLocationRule);
//                        rulesList.add(startTimeStampRule);
//                        rulesList.add(endTimeStampRule);

                        ApplyRule ar = new ApplyRule();
                        StringBuffer result = ar.getAppliedRules(rules);

//                        Added to test writing the result to a file
                        File file = new File("result.json");

                        // if file doesnt exists, then create it
                        if (!file.exists()) {
                            file.createNewFile();
                        }

                        FileWriter fw = new FileWriter(file.getAbsoluteFile());
                        BufferedWriter bw = new BufferedWriter(fw);
                        bw.write(String.valueOf(String.valueOf(result)));
                        bw.close();


                        //System.out.println("rule0:"+result);
                        exchange.getOut().setBody(String.valueOf(result));
                        exchange.getOut().setHeader("createEmageFile", false);
                    }
                })
//                .to("direct:applyandExecuteRule")
                .to("direct:applySpatialWrapper");

        from("direct:applySpatialWrapper")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setHeaders(exchange.getIn().getHeaders());
                        LOGGER.info("Query End Time:" + System.currentTimeMillis());
                        LOGGER.info("{}", new Date());
                        final String body = exchange.getIn().getBody(String.class);
                        //System.out.println("STT:"+body);
                        JSONObject obj = new JSONObject("{ \"list\" : " + body + "}");
//                        LOGGER.info("{ \"list \" :" + exchange.getIn().getBody(String.class) + "}");
                        List<MongoResponse> list = new ArrayList<MongoResponse>();
                        JSONArray array = obj.getJSONArray("list");
                        JSONObject aObj = null;
                        try {
                            for (int i = 0; i < array.length(); i++) {
                                MongoResponse mongoResponse = new MongoResponse();
                                aObj = array.getJSONObject(i);
                                Double themeValue = 1.0;
                                if(aObj.has("stt_what")){
                                    Iterator<String> keys = aObj.getJSONObject("stt_what").keys();
                                    if(keys.hasNext()){
                                        String key = keys.next();
                                        JSONObject what = aObj.getJSONObject("stt_what").getJSONObject(key);
                                        if(what.has("value") && (what.get("value") instanceof Double))
                                            themeValue = what.getDouble("value");
                                    }
                                }
                                else if(aObj.has("value") && aObj.get("value") instanceof Double) {
                                    themeValue = aObj.getDouble("value");
                                }

                                mongoResponse.setValue(themeValue);

                                ELocation loc = new ELocation();
                                if(aObj.has("stt_where")){
                                    if(aObj.getJSONObject("stt_where").has("point")) {                   // stt_where: {point: [lat, lon]}
                                        JSONArray point = aObj.getJSONObject("stt_where").getJSONArray("point");
                                        loc.setLat(point.getDouble(0));
                                        loc.setLon(point.getDouble(1));
                                    }
                                    else if(aObj.getJSONObject("stt_where").has("rectangle")){
                                        // stt_where: {rectangle: [{point:[lat, lon]}, {point:[lat, lon]}]}
                                        JSONArray rec = aObj.getJSONObject("stt_where").getJSONArray("rectangle");
                                        JSONArray minPoint = rec.getJSONObject(0).getJSONArray("point");
                                        JSONArray maxPoint = rec.getJSONObject(1).getJSONArray("point");
                                        double minLat = minPoint.getDouble(0);
                                        double minLon = minPoint.getDouble(1);
                                        double maxLat = maxPoint.getDouble(0);
                                        double maxLon = maxPoint.getDouble(1);
                                        loc.setLat((minLat + maxLat)/2.0);      // get center point
                                        loc.setLon((minLon + maxLon)/2.0);      // get center point
                                    } else{
                                        loc.setLat(aObj.getJSONObject("stt_where").getDouble("lat"));   // stt_where: {lat: double, lon: double}
                                        loc.setLon(aObj.getJSONObject("stt_where").getDouble("lon"));
                                    }
                                } else if(aObj.has("where")){
                                    loc.setLat(aObj.getJSONObject("where").getJSONObject("geo_location").getDouble("latitude"));
                                    loc.setLon(aObj.getJSONObject("where").getJSONObject("geo_location").getDouble("longitude"));
                                } else if(aObj.has("loc")){
                                    loc.setLon(array.getJSONObject(i).getJSONObject("loc").getDouble("lon"));
                                    loc.setLat(array.getJSONObject(i).getJSONObject("loc").getDouble("lat"));
                                }

                                mongoResponse.setLoc(loc);
                                list.add(mongoResponse);
                            }
                        } catch (Exception e) {
                            LOGGER.error("cannot parse json object: " + aObj.toString());
                            e.printStackTrace();
                        }

                        DataSource dataSource = exchange.getIn().getHeader("datasource", DataSource.class);
                        double latUnit = exchange.getIn().getHeader("latUnit", Double.class);
                        double longUnit = exchange.getIn().getHeader("longUnit", Double.class);
                        double nelat = exchange.getIn().getHeader("nelat", Double.class);
                        double nelong = exchange.getIn().getHeader("nelong", Double.class);
                        double swlat = exchange.getIn().getHeader("swlat", Double.class);
                        double swlong = exchange.getIn().getHeader("swlong", Double.class);


                        long rows = (long) ((Math.ceil((nelat - swlat) / latUnit)));
                        long cols = (long) ((Math.ceil((nelong - swlong) / longUnit) * longUnit) / longUnit);
                        LOGGER.info("Row:" + rows);
                        LOGGER.info("Col:" + cols);
                        LOGGER.info("nelat:" + nelat);
                        LOGGER.info("nelong:" + nelong);
                        LOGGER.info("swlat:" + swlat);
                        LOGGER.info("swlong:" + swlong);
                        ArrayList<ArrayList<Double>> grid = new ArrayList<ArrayList<Double>>();

                        double lng;
                        double lat;
                        LOGGER.info("Size of data: " + list.size());
                        for (int i = 0; i < (rows * cols); i++) {
                            grid.add(new ArrayList<Double>());
                        }

                        for (int i = 0; i < list.size(); i++) {

                            lng = list.get(i).getLoc().getLon();
                            lat = list.get(i).getLoc().getLat();

                            //                            Please do not remove the below commented lines until the results are proved.
//
//                            LOGGER.debug("Lat:Long ["+lat+":"+lng +" ]");;
//                            LOGGER.debug("Calculated Lat : ["+((Math.ceil(lat/latUnit))*latUnit) +"]");
//                            LOGGER.debug("Calculated Long : ["+ ((Math.floor(lng/longUnit))*longUnit)+"]");
//                            LOGGER.debug("RowNumber Identified: "+(((nelat-((Math.ceil(lat/latUnit))*latUnit))/latUnit)-1));
//                            LOGGER.debug("ColNumber Identified: "+ (((Math.floor(lng/longUnit))*longUnit)-swlong)/longUnit);
//                            LOGGER.debug("Array Element to insert : "+((int)((((nelat-((Math.ceil(lat/latUnit))*latUnit))/latUnit)-1)*cols)+(int)((((Math.floor(lng/longUnit))*longUnit)-nelong)/longUnit)));
//                            LOGGER.debug("Value Inserted:" + list.get(i).getValue());
//
//                            LOGGER.debug("1. "+Math.ceil(lat/latUnit));
//                            LOGGER.debug("2. "+((Math.ceil(lat/latUnit))*latUnit));
//                            LOGGER.debug("3. "+(nelat-((Math.ceil(lat/latUnit))*latUnit)));
//                            LOGGER.debug("4. "+((nelat-((Math.ceil(lat/latUnit))*latUnit))/latUnit));
//                            LOGGER.debug("5. "+(((nelat-((Math.ceil(lat/latUnit))*latUnit))/latUnit)-1));
//                            LOGGER.debug("6. "+((((nelat-((Math.ceil(lat/latUnit))*latUnit))/latUnit)-1)*cols));
//                            LOGGER.debug("7. "+(int)((((nelat-((Math.ceil(lat/latUnit))*latUnit))/latUnit)-1)*cols));
//                            LOGGER.debug("8. "+ (Math.floor(lng/longUnit)) );
//                            LOGGER.debug("9. "+ ((Math.floor(lng/longUnit))*longUnit));
//                            LOGGER.debug("10. "+ (((Math.floor(lng/longUnit))*longUnit)-swlong));
//                            LOGGER.debug("11. "+((((Math.floor(lng/longUnit))*longUnit)-swlong)/longUnit));
//                            LOGGER.debug("12. "+(int)((((Math.floor(lng/longUnit))*longUnit)-swlong)/longUnit) );
//                            LOGGER.debug("13. "+((int)((((nelat-(((Math.ceil(lat/latUnit))*latUnit)+(nelat%latUnit)))/latUnit)-1)*cols)+(int)(((((Math.floor(lng/longUnit))*longUnit)-swlong)-(swlong%longUnit))/longUnit)));
//                            ((((nelat-((Math.ceil(lat/latUnit))*latUnit))/latUnit)-1)*cols);


//                            //RowNumber * Cols + colNumber
                            int point = ((int) ((((nelat - (((Math.ceil(lat / latUnit)) * latUnit) + (nelat % latUnit))) / latUnit)) * cols) + (int) (((((Math.floor(lng / longUnit)) * longUnit) - swlong) - (swlong % longUnit)) / longUnit));
                            if (point > 0 && point < (rows * cols)) {
                                grid.get(point).add(list.get(i).getValue());
                            }


                        }
                        String operation = exchange.getIn().getHeader("spatial_wrapper", String.class);

                        LOGGER.info("Operator: " + operation);
                        List<Double> outputList = new ArrayList<Double>();

                        if (list.size() > 0) {
                            if (operation.equalsIgnoreCase("sum")) {
                                for (int i = 0; i < (rows * cols); i++) {
                                    double val = 0;
                                    for (int j = 0; j < grid.get(i).size(); j++) {
                                        val += (grid.get(i).get(j));
                                    }
                                    outputList.add(val);
                                }
                            } else if (operation.equalsIgnoreCase("min")) {


                                for (int i = 0; i < (rows * cols); i++) {
                                    double val = 0;
                                    double min = Double.MAX_VALUE;
                                    for (int j = 0; j < grid.get(i).size(); j++) {
                                        if (grid.get(i).get(j) < min) {
                                            val = grid.get(i).get(j);
                                            min = val;
                                        }
                                    }
                                    outputList.add(val);
                                }

                            } else if (operation.equalsIgnoreCase("max")) {


                                for (int i = 0; i < (rows * cols); i++) {
                                    double val = 0;
                                    double max = (-Double.MAX_VALUE) + 1;
                                    for (int j = 0; j < grid.get(i).size(); j++) {
                                        if (grid.get(i).get(j) > max) {
                                            val = grid.get(i).get(j);
                                            max = val;
                                        }
                                    }
                                    outputList.add(val);
                                }

                            } else if (operation.equalsIgnoreCase("avg")) {
                                for (int i = 0; i < (rows * cols); i++) {
                                    double val = 0;
                                    double avg = 0;
                                    for (int j = 0; j < grid.get(i).size(); j++) {
                                        val += (grid.get(i).get(j));
                                    }
                                    if(grid.get(i).size() >0)
                                        avg = val / grid.get(i).size();
                                    outputList.add(avg);
                                }
                            } else if (operation.equalsIgnoreCase("count")) {
                                for (int i = 0; i < (rows * cols); i++) {
                                    double count = grid.get(i).size();
                                    outputList.add(count);
                                }
                            } else if (operation.equalsIgnoreCase("majority")) {
                                for (int i = 0; i < (rows * cols); i++) {
                                    double maj = CommonUtil.getMajority(grid.get(i));
                                    outputList.add(maj);
                                }
                            } else if (operation.equalsIgnoreCase("most_freq")) {
                                for (int i = 0; i < (rows * cols); i++) {
                                    double mostFreq = CommonUtil.getMostFreq(grid.get(i));
                                    outputList.add(mostFreq);
                                }
                            }
                        } else {
                            LOGGER.info("List size empty...");
                            for (int i = 0; i < (rows * cols); i++) {
                                outputList.add(0.0);
                            }
                        }
                        exchange.getOut().setHeader("numOfRows", rows);
                        exchange.getOut().setHeader("numOfCols", cols);
                        long endTimeToCheck = exchange.getIn().getHeader("endTimeToCheck", Long.class);
                        long startTimeToCheck = exchange.getIn(

                        ).getHeader("timeToCheck", Long.class);
                        exchange.getOut().setHeader("timeWindow", endTimeToCheck - startTimeToCheck);
                        System.out.println("timeWindowwwww: " + (endTimeToCheck - startTimeToCheck));

                        //long timeWindow = exchange.getIn().getHeader("timeWindow", Long.class);
                        //System.out.println("timeWindowwww: " + timeWindow);

                        exchange.getOut().setBody(outputList, List.class);
                        LOGGER.info("End Time:");
                        LOGGER.info("{}", System.currentTimeMillis());
                        LOGGER.info("{}", new Date());
                    }
                })
                .to("direct:emageBuilder");
    }

}
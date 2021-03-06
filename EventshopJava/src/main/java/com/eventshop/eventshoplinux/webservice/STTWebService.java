package com.eventshop.eventshoplinux.webservice;

import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.eventshop.eventshoplinux.DAO.datasource.DataSourceDao;
import com.eventshop.eventshoplinux.DAO.datasource.DataSourceManagementDAO;
import com.eventshop.eventshoplinux.DAO.rule.RuleDao;
import com.eventshop.eventshoplinux.akka.dataSource.DataSourceSchedular;
import com.eventshop.eventshoplinux.akka.query.MainQueryActor;
import com.eventshop.eventshoplinux.domain.common.FrameParameters;
import com.eventshop.eventshoplinux.domain.datasource.DataSource;
import com.eventshop.eventshoplinux.ruleEngine.ApplyRule;
import com.eventshop.eventshoplinux.ruleEngine.Rule;
import com.eventshop.eventshoplinux.ruleEngine.Rules;
import com.eventshop.eventshoplinux.util.commonUtil.CommonUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.collections.ArrayStack;
import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import java.math.BigDecimal;

/**
 * Created by siripen on 2/28/16.
 */
@Path("/sttwebservice")
public class STTWebService {
    private final static Logger LOGGER = LoggerFactory.getLogger(STTWebService.class);
    RuleDao ruleDAO = new RuleDao();

    @Context
    private ServletContext context;

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/test")
    public String runScript() {
        ActorSystem actorSystem = (ActorSystem) context.getAttribute("AkkaActorSystem");
        ActorSelection mainQueryActor = actorSystem.actorSelection("akka://eventshop-actorSystem/user/mainQueryActor");
        //mainQueryActor.tell(new MainQueryActor.EnableAndRunQuery((query.getQuery_id())), null);
        //return "Enabled Query with ID " + query.getQuery_id();
        LOGGER.info("run script ");
        //queryScriptActor.tell(new DataSourceSchedular.StartDataSource((dataSource.getID())), null);
        return "script";
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search/{ruleid}/box/{minlatlon}/{maxlatlon}/{stime}/{etime}")
    public String searchSTT(
        @PathParam(value="ruleid") final int ruleid,
        @PathParam(value="minlatlon") final String min,			// 24.0,-125.0
        @PathParam(value="maxlatlon") final String max,			// 50.0,-66.0
        @PathParam(value="stime") final String start,			// 2010-01-01T00:00:00Z
        @PathParam(value="etime") final String end)				// 2011-01-01T00:00:00Z
    {
        RuleDao ruleDao = new RuleDao();
        Rules rules = ruleDao.getRules(ruleid);
        if(!min.equalsIgnoreCase("null") && !min.isEmpty() && !max.equalsIgnoreCase("null") && !max.isEmpty()){
            Rule ruleWhere = new Rule();
            ruleWhere.setDataField("stt_where.point");
            ruleWhere.setRuleOperator("coordinates");
            ruleWhere.setRuleParameters(min + "," + max);
            rules.addRule(ruleWhere);
        }

        if(!start.equalsIgnoreCase("null") && !start.isEmpty() && !end.equalsIgnoreCase("null") && !end.isEmpty()){
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("between");
            ruleWhen.setRuleParameters(start + "," + end);
            rules.addRule(ruleWhen);
        } else if(!start.equalsIgnoreCase("null") && !start.isEmpty()){
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("after");
            ruleWhen.setRuleParameters(start);
            rules.addRule(ruleWhen);
        } else if(!end.equalsIgnoreCase("null") && !end.isEmpty()){
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("before");
            ruleWhen.setRuleParameters(end);
            rules.addRule(ruleWhen);
        }

        ApplyRule applyRule = new ApplyRule();


        String result =  applyRule.getAppliedRules(rules).toString();
        LOGGER.info("result: " + result);
        return result;

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search/{ruleid}/circle/{centerlatlonradius}/{stime}/{etime}")
    public String searchSTTwithin(
            @PathParam(value="ruleid") final int ruleid,
            @PathParam(value="centerlatlonradius") final String params,			// 24.0,-125.0, 10
            @PathParam(value="stime") final String start,			// 2010-01-01T00:00:00Z
            @PathParam(value="etime") final String end)				// 2011-01-01T00:00:00Z
    {
        RuleDao ruleDao = new RuleDao();
        Rules rules = ruleDao.getRules(ruleid);
        if (!params.equalsIgnoreCase("null") && !params.isEmpty()) {
            Rule ruleWhere = new Rule();
            ruleWhere.setDataField("stt_where.point");
            ruleWhere.setRuleOperator("radius");
            ruleWhere.setRuleParameters(params);
            rules.addRule(ruleWhere);
        }

        if (!start.equalsIgnoreCase("null") && !start.isEmpty() && !end.equalsIgnoreCase("null") && !end.isEmpty()) {
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("between");
            ruleWhen.setRuleParameters(start + "," + end);
            rules.addRule(ruleWhen);
        } else if (!start.equalsIgnoreCase("null") && !start.isEmpty()) {
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("after");
            ruleWhen.setRuleParameters(start);
            rules.addRule(ruleWhen);
        } else if (!end.equalsIgnoreCase("null") && !end.isEmpty()) {
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("before");
            ruleWhen.setRuleParameters(end);
            rules.addRule(ruleWhen);
        }

        ApplyRule applyRule = new ApplyRule();


        String result = applyRule.getAppliedRules(rules).toString();
        LOGGER.info("result: " + result);
        return result;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search/{ruleid}/box/{minlatlon}/{maxlatlon}/{stime}/{etime}/{resolution}/{agg}/{themename}/emage")
    public String searchMJsonSTTEmage(
            @PathParam(value="ruleid") final int ruleid,
            @PathParam(value="minlatlon") final String min,			// 24.0,-125.0
            @PathParam(value="maxlatlon") final String max,			// 50.0,-66.0
            @PathParam(value="stime") final String start,			// 2010-01-01T00:00:00Z
            @PathParam(value="etime") final String end,			    // 2011-01-01T00:00:00Z
            @PathParam(value="resolution") final String resolution,  // 0.1,0.1
            @PathParam(value="agg") final String aggOp,             // min, max, avg, count
            @PathParam(value="themename") final String themeName)   // e.g,. windspeed
    {
        return searchSTTEmage(ruleid, "point", min, max, start, end, resolution, aggOp, themeName);
    }



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search/{ruleid}/{sttwhere}/{minlatlon}/{maxlatlon}/{stime}/{etime}/{resolution}/{agg}/{themename}/emage")
    public String searchSTTEmage(
            @PathParam(value="ruleid") final int ruleid,
            @PathParam(value="sttwhere") final String sttwhere,     // point or latlon
            @PathParam(value="minlatlon") final String min,			// 24.0,-125.0
            @PathParam(value="maxlatlon") final String max,			// 50.0,-66.0
            @PathParam(value="stime") final String start,			// 2010-01-01T00:00:00Z
            @PathParam(value="etime") final String end,			    // 2011-01-01T00:00:00Z
            @PathParam(value="resolution") final String resolution,  // 0.1,0.1
            @PathParam(value="agg") final String aggOp,             // min, max, avg, count
            @PathParam(value="themename") final String themeName)   // e.g,. windspeed
    {
        RuleDao ruleDao = new RuleDao();
        Rules rules = ruleDao.getRules(ruleid);

        double[] box = {-90.0, -180.0, 90, 180.0};

        //long sTime = 0, eTime = 0;
        if(!min.equalsIgnoreCase("null") && !min.isEmpty() && !max.equalsIgnoreCase("null") && !max.isEmpty()){
            String[] minStr = min.split(",");
            box[0] = Double.parseDouble(minStr[0]); // min lat
            box[1] = Double.parseDouble(minStr[1]); // min lon
            String[] maxStr = max.split(",");
            box[2] = Double.parseDouble(maxStr[0]); // max lat
            box[3] = Double.parseDouble(maxStr[1]); // max lon

            if(sttwhere.equalsIgnoreCase("array")) {
                Rule ruleWhere = new Rule();
                ruleWhere.setDataField("stt_where.point");
                ruleWhere.setRuleOperator("coordinates");
                ruleWhere.setRuleParameters(min + "," + max);
                rules.addRule(ruleWhere);
            } else if(sttwhere.equalsIgnoreCase("latlon")){
                Rule ruleWhere1 = new Rule("stt_where.lat", ">", String.valueOf(box[0]));
                Rule ruleWhere2 = new Rule("stt_where.lat", "<", String.valueOf(box[2]));
                Rule ruleWhere3 = new Rule("stt_where.lon", ">", String.valueOf(box[1]));
                Rule ruleWhere4 = new Rule("stt_where.lon", "<", String.valueOf(box[3]));
                rules.addRule(ruleWhere1);
                rules.addRule(ruleWhere2);
                rules.addRule(ruleWhere3);
                rules.addRule(ruleWhere4);
            }

        }


        if(!start.equalsIgnoreCase("null") && !start.isEmpty() && !end.equalsIgnoreCase("null") && !end.isEmpty()){
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("between");
            ruleWhen.setRuleParameters(start + "," + end);
            rules.addRule(ruleWhen);
        } else if(!start.equalsIgnoreCase("null") && !start.isEmpty()){
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("after");
            ruleWhen.setRuleParameters(start);
            rules.addRule(ruleWhen);
        } else if(!end.equalsIgnoreCase("null") && !end.isEmpty()){
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("before");
            ruleWhen.setRuleParameters(end);
            rules.addRule(ruleWhen);
        }

        double latUnit = 0.1, lonUnit = 0.1;
        if(!resolution.equalsIgnoreCase("null") && !resolution.isEmpty()){
            String[] unit = resolution.split(",");
            latUnit = Double.parseDouble(unit[0]);
            lonUnit = Double.parseDouble(unit[1]);
        }

        ApplyRule applyRule = new ApplyRule();
        String result =  applyRule.getAppliedRules(rules).toString();

        long timeWindow = 0;
        long syncAtMilSec = 0;
        box = this.getStandardBox(box[0], box[1], box[2],box[3], latUnit, lonUnit);
        LOGGER.info("stdbox" + box[0] + "," + box[1] + "," + box[2] + "," + box[3]);
        FrameParameters fp = new FrameParameters(timeWindow, syncAtMilSec, latUnit, lonUnit, box[0], box[1], box[2],box[3]);
        LOGGER.info("fp: " + fp.toString());
        fp.setSpatial_wrapper(aggOp);

        JsonArray emage = this.getTempEmage(result, fp, themeName);
        //LOGGER.info("result: " + emage.toString());
        return emage.toString();
    }

    private double[] getStandardBox(double minLat,double minLon, double maxLat, double maxLon, double maxLatUnit, double maxLongUnit){	// round up the box
        double preferMinLat = Math.floor(minLat/maxLatUnit) * maxLatUnit;
        double preferMinLong = Math.floor(minLon/maxLongUnit) * maxLongUnit;
        double preferMaxLat = Math.ceil(maxLat/maxLatUnit) * maxLatUnit;
        double preferMaxLong = Math.ceil(maxLon/maxLongUnit) * maxLongUnit;


        double[] stdBox = {
                BigDecimal.valueOf(preferMinLat).setScale(5, BigDecimal.ROUND_FLOOR).doubleValue(),
                BigDecimal.valueOf(preferMinLong).setScale(5,BigDecimal.ROUND_FLOOR).doubleValue(),
                BigDecimal.valueOf(preferMaxLat).setScale(5, BigDecimal.ROUND_CEILING).doubleValue(),
                BigDecimal.valueOf(preferMaxLong).setScale(5, BigDecimal.ROUND_CEILING).doubleValue()
                };
        return stdBox;
    }


    // the higher latitude, the smaller row number
    public int latitude2row(double latitude, double[] box, double latUnit){
        int numRows = (int) Math.ceil(Math.abs(box[2] - box[0]) / latUnit);
        LOGGER.info("numRows: " + numRows + ", row_index: " + (int) Math.ceil((box[2]- latitude)/latUnit) + ", lat" + latitude + ", minLat " + box[0]);
        if(latitude >= box[0] && latitude < box[2])
            return (int) Math.ceil((box[2]- latitude)/latUnit) - 1;
        else
            return -1;
    }

    public int longitude2col(double longitude, double[] box, double longUnit){
        if(longitude >= box[1] && longitude < box[3])
            return (int) Math.floor((longitude - box[1])/longUnit);
        else
            return -1;
    }

    // return lower left corner of the cell
    public double[] index2latlon(int index, FrameParameters fp){
        int row = index/fp.numOfColumns;
        int col = index%fp.numOfColumns;
        double lat = fp.getNeLat() - (row + 1)*fp.latUnit;
        double lon = fp.getSwLong() + (col*fp.longUnit);
        return new double[]{lat, lon};
    }


    public JsonObject index2rectangle(int index, FrameParameters fp){
        int row = index/fp.numOfColumns;
        int col = index%fp.numOfColumns;

        double lat = fp.getNeLat() - ((row+1)*fp.latUnit);
        double lon = fp.getSwLong() + (col*fp.longUnit);
        String rectStr = "{\"rectangle\":[{\"point\":[" + lat + "," + lon + "]}, {\"point\":[" + (lat + fp.latUnit) + "," + (lon + fp.longUnit) +"]}]}";
        LOGGER.info("row,col:" + row + "," + col + "," + rectStr);
        JsonParser parser = new JsonParser();
        return parser.parse(rectStr).getAsJsonObject();
    }



    private JsonArray getTempEmage(String STTList, FrameParameters fp, String themeName){

        String operation = fp.getSpatial_wrapper();
        LOGGER.info("Operator: " + operation);
        JsonParser parser = new JsonParser();
        JsonArray sttJsonArray = parser.parse(STTList).getAsJsonArray();
        ArrayList<ArrayList<Double>> grid = new ArrayList<ArrayList<Double>>();
        ArrayList<JsonArray> gridSTT = new ArrayList<JsonArray>();
        int rows = fp.getNumOfRows();
        int cols = fp.getNumOfColumns();
        for (int i = 0; i < (rows * cols); i++) {
            grid.add(new ArrayList<Double>());
            gridSTT.add(new JsonArray());
        }

        // add values to each cell
        for(JsonElement js: sttJsonArray){
            JsonObject sttObj = js.getAsJsonObject();
            JsonObject sttWhere = sttObj.getAsJsonObject("stt_where");
            double lat = 999, lon = 999, value = 1.0;
            if(sttWhere.has("point")){
                JsonArray coordinate = sttWhere.getAsJsonArray("point");
                lat = coordinate.get(0).getAsDouble();
                lon = coordinate.get(1).getAsDouble();
            } else if(sttWhere.has("lat") && sttWhere.has("lon")){
                lat = sttWhere.get("lat").getAsDouble();
                lon = sttWhere.get("lon").getAsDouble();
            }
            if(!themeName.equalsIgnoreCase("null") && !themeName.isEmpty()){
                if(sttObj.getAsJsonObject("stt_what").has(themeName)){
                    String themeValue = sttObj.getAsJsonObject("stt_what").getAsJsonObject(themeName).get("value").getAsString();
                    try {
                        value = Double.parseDouble(themeValue);
                    } catch(Exception ex) {
                        LOGGER.error("theme value is not a double, so the default value (1.0) is assigned");
                    }
                }
            }
            //RowNumber * Cols + colNumber
            //int point = ((int) ((((fp.neLat - (((Math.ceil(lat / fp.latUnit)) * fp.latUnit) + (fp.neLat % fp.latUnit))) / fp.latUnit)) * fp.getNumOfColumns())
            //        + (int) (((((Math.floor(lon / fp.longUnit)) * fp.longUnit) - fp.swLong) - (fp.swLat % fp.longUnit)) / fp.longUnit));
            double[] box = new double[]{fp.swLat, fp.swLong, fp.neLat, fp.neLong};
            int row = this.latitude2row(lat, box, fp.latUnit);
            int col = this.longitude2col(lon, box, fp.longUnit);
            int point = row*fp.numOfColumns + col;
            LOGGER.info("lat,lon,row,col,point: " + lat + "," + lon + "," + row + "," + col + "," + point + " ["+fp.latUnit+", " + fp.longUnit+"]");
            if (point > 0 && point < (rows * cols)) {
                grid.get(point).add(value);
                gridSTT.get(point).add(sttObj);
            }
        }
        JsonArray emage = new JsonArray();
        List<Double> cellList = CommonUtil.getCellValue(operation, rows, cols, grid);
        for(int i = 0; i < cellList.size(); i++){
            if(cellList.get(i) != 0.0){
                JsonObject sttList = new JsonObject();
                sttList.add("orderedlist", gridSTT.get(i).getAsJsonArray());

                JsonObject stel = new JsonObject();
                stel.addProperty("index", i);
                stel.add("cell", index2rectangle(i, fp));
                stel.addProperty(operation, cellList.get(i));
                stel.add("values", sttList);
                emage.add(stel);
            }
        }
        return emage;
        /*
        RuleDao ruleDao = new RuleDao();
        Rules rules = ruleDao.getRules(ruleid);


        String source = rules.getSource();
        if(source.contains("ds")){
            DataSourceManagementDAO sourceDao = new DataSourceManagementDAO();
            DataSource ds = sourceDao.getDataSource(Integer.parseInt(source.replace("ds","")));

        }
        */



    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search/{ruleid}/{sttwhere}/{minlatlon}/{maxlatlon}/{stime}/{etime}/{resolution}/{agg}/{themename}/query")
    public String getQ(
            @PathParam(value="ruleid") final String ruleid,
            @PathParam(value="sttwhere") final String sttwhere,     // point or latlon
            @PathParam(value="minlatlon") final String min,			// 24.0,-125.0
            @PathParam(value="maxlatlon") final String max,			// 50.0,-66.0
            @PathParam(value="stime") final String start,			// 2010-01-01T00:00:00Z
            @PathParam(value="etime") final String end,			    // 2011-01-01T00:00:00Z
            @PathParam(value="resolution") final String resolution,  // 0.1,0.1
            @PathParam(value="agg") final String aggOp,             // min, max, avg, count
            @PathParam(value="themename") final String themeName)   // e.g,. windspeed
    {
        //RuleDao ruleDao = new RuleDao();
        //Rules rules = ruleDao.getRules(ruleid);

        double[] box = {-90.0, -180.0, 90, 180.0};

        //long sTime = 0, eTime = 0;
        if(!min.equalsIgnoreCase("null") && !min.isEmpty() && !max.equalsIgnoreCase("null") && !max.isEmpty()) {
            String[] minStr = min.split(",");
            box[0] = Double.parseDouble(minStr[0]); // min lat
            box[1] = Double.parseDouble(minStr[1]); // min lon
            String[] maxStr = max.split(",");
            box[2] = Double.parseDouble(maxStr[0]); // max lat
            box[3] = Double.parseDouble(maxStr[1]); // max lon

        }
        double latUnit = 0.1, lonUnit = 0.1;
        if(!resolution.equalsIgnoreCase("null") && !resolution.isEmpty()){
            String[] unit = resolution.split(",");
            latUnit = Double.parseDouble(unit[0]);
            lonUnit = Double.parseDouble(unit[1]);
        }

        long timeWindow = 0;
        long syncAtMilSec = 0;
        box = this.getStandardBox(box[0], box[1], box[2],box[3], latUnit, lonUnit);
        LOGGER.info("stdbox" + box[0] + "," + box[1] + "," + box[2] + "," + box[3]);
        FrameParameters fp = new FrameParameters(timeWindow, syncAtMilSec, latUnit, lonUnit, box[0], box[1], box[2],box[3]);

        String basePath = "http://eventshop.ics.uci.edu:8085/eventshoplinux/";
        String url1 = "rest/sttwebservice/search/6/latlon/"+min+"/"+max+"/"+start+"/"+end+"/"+latUnit + "%20," + lonUnit +"/avg/fill/emage";
        String url2 = "rest/sttwebservice/search/7/latlon/"+min+"/"+max+"/"+start+"/"+end+"/"+latUnit + "%20," + lonUnit+"/avg/fill/emage";
        String url3 = "rest/sttwebservice/search/10/latlon/"+min+"/"+max+"/"+start+"/"+end+"/"+latUnit + "%20," + lonUnit+"/avg/fill/emage";
        String[] urls = {url1, url2, url3};
        StringBuilder strb = new StringBuilder();
        JsonArray emage = new JsonArray();



        try {
            JsonArray sensors = null, microreport = null, events = null;
            JsonArray[] grid = new JsonArray[urls.length];
            for(int i = 0; i < urls.length; i++) {
                URL path = new URL(basePath + urls[i]);
                BufferedReader br = new BufferedReader(new InputStreamReader(path.openStream()));
                //log.info("CODEC: " + codec.name());
                StringBuilder str = new StringBuilder();
                for (String line; (line = br.readLine()) != null; ) {
                    str.append(line);
                }
                JsonParser parser = new JsonParser();
                if(i == 0)
                    sensors = parser.parse(str.toString()).getAsJsonArray();
                else if(i == 1)
                    microreport = parser.parse(str.toString()).getAsJsonArray();
                else if(i == 2)
                    events = parser.parse(str.toString()).getAsJsonArray();
                //grid[i] = parser.parse(str.toString()).getAsJsonArray();
            }
            grid[0] = sensors;
            grid[1] = microreport;
            grid[2] = events;
            ArrayList<List<Double>> gridArr = new ArrayList<List<Double>>();
            Map<Integer, List<Double>> gridMap = new HashMap<Integer, List<Double>>();
            for(int i = 0; i < grid.length; i++){
                for(int j = 0; j < grid[i].size(); j++){
                    JsonObject cell = grid[i].get(j).getAsJsonObject();
                    int index = cell.get("index").getAsInt();
                    double value = cell.get("avg").getAsDouble();
                    if(gridMap.containsKey(index)){
                        gridMap.get(index).add(value);
                    } else{
                        List<Double> temp = new ArrayList<Double>();
                        temp.add(value);
                        gridMap.put(index, temp);
                    }
                }
            }
            Iterator<Integer> it =  gridMap.keySet().iterator();
            ArrayList<Double> cellList = new ArrayList<Double>();
            int length = fp.getNumOfRows()*fp.getNumOfColumns();
            for(int i = 0; i < length; i++){
                cellList.add(0.0);
            }
            while(it.hasNext()){
                int index = it.next();
                List<Double> list = gridMap.get(index);
                double total = 0.0, count = 0.0, avg = 0.0;
                for(int i = 0; i < list.size(); i++){
                    total = total + list.get(i);
                    count = count + 1;
                }
                if(count > 0)
                    avg = total/count;
                cellList.add(index, avg);
                strb.append(index + "," + gridMap.get(index)+ ","+ avg);
                System.out.println(index + "," + gridMap.get(index)+ ","+ avg);
            }

            for(int i = 0; i < cellList.size(); i++){
                if(cellList.get(i) != 0.0){
                    JsonObject sttList = new JsonObject();

                    JsonObject stel = new JsonObject();
                    stel.addProperty("index", i);
                    stel.add("cell", index2rectangle(i, fp));
                    stel.addProperty("avg", cellList.get(i));
                    stel.add("values", sttList);
                    emage.add(stel);
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return emage.toString();
    }
}

package com.eventshop.eventshoplinux.webservice;

import com.eventshop.eventshoplinux.DAO.rule.RuleDao;
import com.eventshop.eventshoplinux.ruleEngine.ApplyRule;
import com.eventshop.eventshoplinux.ruleEngine.Rule;
import com.eventshop.eventshoplinux.ruleEngine.Rules;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

/**
 * Created by siripen on 2/28/16.
 */
@Path("/sttwebservice")
public class STTWebService {
    private final static Logger LOGGER = LoggerFactory.getLogger(STTWebService.class);
    RuleDao ruleDAO = new RuleDao();



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search/{ruleid}/{minlatlon}/{maxlatlon}/{stime}/{etime}")
    public String searchSTT(
        @PathParam(value="ruleid") final int ruleid,
        @PathParam(value="minlatlon") final String min,			// 24.0,-125.0
        @PathParam(value="maxlatlon") final String max,			// 50.0,-66.0
        @PathParam(value="stime") final String start,			// 2010-01-01T00:00:00Z
        @PathParam(value="etime") final String end)				// 2011-01-01T00:00:00Z
    {
        RuleDao ruleDao = new RuleDao();
        Rules rules = ruleDao.getRules(ruleid);
        if(min != "null" && min != "" && max != "" && max != "null"){
            Rule ruleWhere = new Rule();
            ruleWhere.setDataField("stt_where.point");
            ruleWhere.setRuleOperator("coordinates");
            ruleWhere.setRuleParameters(min + "," + max);
            rules.addRule(ruleWhere);
        }

        if(start != "" && start != "null" && end != "" && end != "null"){
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("between");
            ruleWhen.setRuleParameters(start + "," + end);
            rules.addRule(ruleWhen);
        } else if(start != "" && start != "null"){
            Rule ruleWhen = new Rule();
            ruleWhen.setDataField("stt_when.datetime");
            ruleWhen.setRuleOperator("after");
            ruleWhen.setRuleParameters(start);
            rules.addRule(ruleWhen);
        } else if(end != "" && end != "null"){
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

/*
            System.out.println("in geting stt details");
            String minPoint = "24.0,-125.0";
            String maxPoint = "50.0,-66.0";

            String stime = "2010-01-01T00:00:00Z";
            String etime = "2011-01-01T00:00:00Z";

            if(!min.contains("null")) minPoint = min;
            if(!max.contains("null")) maxPoint = max;
            if(!start.contains("null")) stime = start;
            if(!end.contains("null")) etime = end;

            String dbName = "dbflickr";
            String tableName = tablename;
            String sttWhere = "", sttWhereLat = "", sttWhereLon = "", sttWhen = "";
            String sttProjWhat = "", sttSelectWhat = "", sttSelectOp = "", sttSelectValue ="";

            if(tableName.contains("flickr")){
                dbName = "dbflickr";
                sttWhere = "coord";
                sttWhereLat = "latitude";
                sttWhereLon = "longitude";
                sttWhen = "date_taken";
                //String detail = "image_url,image_class,user_tags,title";
                //sttProjWhat = detail.split(",");
                sttProjWhat = "image_id as stt_id, image_url as image_url, image_class as theme,user_tags as tags, title as title";
                sttSelectWhat = "image_class";
                sttSelectOp = "=";
                sttSelectValue = themeValue;
            } else if(tableName.contains("uk")){
                dbName = "dbflickr";
                sttWhere = "coord";
                sttWhereLat = "latitude";
                sttWhereLon = "longitude";
                sttWhen = "date_taken";
                //String detail = "image_url,image_class,user_tags,title";
                //sttProjWhat = detail.split(",");
                sttProjWhat = "image_id as stt_id, download_url as image_url, concepts as theme,user_tags as tags, title as title";
                sttSelectWhat = "concepts";
                sttSelectOp = "LIKE";
                sttSelectValue = "'%" + themeValue + "%'";
            } else if(tableName.contains("stt")){
                dbName = "dbflickr";
                sttWhere = "coord";
                sttWhereLat = "latitude";
                sttWhereLon = "longitude";
                sttWhen = "date_taken";
                //String detail = "image_url,image_class,user_tags,title";
                //sttProjWhat = detail.split(",");
                sttProjWhat = "photo_id as stt_id, download_url as image_url, concept as theme, user_tags as tags, title as title";
                sttSelectWhat = "concept";
                sttSelectOp = "=";
                sttSelectValue = "'" + themeValue + "'";
            }

            String themeCondition = "";
            StringBuilder themeCon = new StringBuilder();
            if(!themeName.contains("null") && !themeValue.contains("null") && !themeValue.contains("undefined")){
                String[] orTheme = themeValue.split(",");
                String or = "";
                for(int j = 0; j < orTheme.length; j++){
                    themeCon.append(or + sttSelectWhat + " " + sttSelectOp + " '" + orTheme[j] + "'");
                    or = "or ";
                }
                themeCondition = " and (" + themeCon.toString() + ")";
                log.info("themeCon: " + themeCondition);
            }

            BaseDAO dao = new BaseDAO();
            Connection con = dao.connection(Config.getProperty("slnDBURL"), Config.getProperty("slnUser"), Config.getProperty("slnPwd"));
            PreparedStatement stmt = null;
            ResultSet rs = null;
            String[] minLatLon = minPoint.split(",");
            String[] maxLatLon = maxPoint.split(",");
            String lineString = "LineString(" + minLatLon[1] + " " + minLatLon[0] + ", " + maxLatLon[1] + " " + maxLatLon[0] + ")";

            log.info(lineString);

            String timeCond = " and (" + sttWhen  + " >= '" + stime +"' and "
                    + sttWhen + "< '" + etime  + "') ";


            JSONArray valueList = new JSONArray();
            try {
                String cellStr = "set @cell = \"" + lineString + "\";";
                System.out.println(cellStr);
                stmt = con.prepareStatement(cellStr);
                stmt.execute(cellStr);
                String sqlPhoto = "select " + sttProjWhat + ", " + sttWhereLat + " as lat, " + sttWhereLon +" as lon, "
                        + " unix_timestamp(" + sttWhen + ") as dt "
                        + " from " + dbName + "." + tableName  + " "
                        + " where ST_CONTAINS(st_envelope(st_geomfromtext(@cell)), " + sttWhere + ")"
                        + timeCond + themeCondition
                        + " order by dt;";
                System.out.println(sttProjWhat);
                System.out.println(sqlPhoto);
                rs =  stmt.executeQuery(sqlPhoto);
                if(sttProjWhat.contains("image_url")){	// it is photo emages
                    while(rs.next()){
                        JSONObject photo = new JSONObject("{\"stt_id\":\"" +  rs.getString("stt_id") + "\","
                                + "\"stt_where\":{\"point\":[" + rs.getDouble("lat")+ "," + rs.getDouble("lon") + "]},"
                                + "\"stt_when\":{\"datetime\":" + rs.getLong("dt")+ "},"
                                + "\"stt_what\":{"
                                + "		\"image_url\":\"" + rs.getString("image_url") + "\","
                                + "		\"theme\":\"" + rs.getString("theme") + "\", "
                                + "		\"tags\":\"" + rs.getString("tags") + "\", "
                                + "		\"title\":\"" + rs.getString("title") + "\" "
                                + "}}");
                        valueList.put(photo);
                    }
                }
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                try {
                    if(rs != null) rs.close();
                    if(stmt != null) stmt.close();
                    if(dao.con != null) dao.con.close();
                } catch (SQLException e) {
                    log.error(e.getMessage());
                }
            }
            //System.out.println(emageList.toString());
            return valueList;
            */


    }

}

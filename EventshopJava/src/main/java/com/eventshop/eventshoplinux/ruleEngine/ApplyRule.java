package com.eventshop.eventshoplinux.ruleEngine;

import com.eventshop.eventshoplinux.util.commonUtil.Config;
import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Created by aravindh on 9/28/15.
 */
public class ApplyRule {
    private final static Logger LOGGER = LoggerFactory.getLogger(ApplyRule.class);

    public StringBuffer getAppliedRules(Rules rules) {

        System.out.println("rules<><><>:"+ rules.toString());

        List<Rule> rulesList = rules.getRules();//TODO

        Mongo mongoConn = null;
        try {
            mongoConn = new Mongo(Config.getProperty("mongoHost"), Integer.parseInt(Config.getProperty("mongoPort")));
            DB mongoDb = mongoConn.getDB("events");
            LOGGER.info("in apply rule: get source id ["+rules.getRuleID()+"]: " + rules.getSource());
            DBCollection collection = mongoDb.getCollection(rules.getSource());


            if (rulesList == null || rulesList.isEmpty()) {
                rulesList = new ArrayList<Rule>();
            }
            System.out.println("Rule List in Apply rule: "+ rulesList.toString());
            QueryBuilder query = new QueryBuilder();

            StringTokenizer st;
            for (Rule rule : rulesList) {
                switch (rule.getRuleOperator()) {
                    case "<"://Number
                        query.put(rule.getDataField()).lessThan(Double.valueOf(rule.getRuleParameters()));
                        break;
                    case ">"://Number
                        query.put(rule.getDataField()).greaterThan(Double.valueOf(rule.getRuleParameters()));
                        break;
                    case "="://Number
                        query.put(rule.getDataField()).is(Double.valueOf(rule.getRuleParameters()));
                        break;
                    case "!="://Number
                        query.put(rule.getDataField()).notEquals(Double.valueOf(rule.getRuleParameters()));
                        break;
                    case "regex"://String
                        query.put(rule.getDataField()).regex(Pattern.compile(rule.getRuleParameters()));
                        break;
                    case "equals"://String
                        query.put(rule.getDataField()).is(rule.getRuleParameters());
                        break;
                    case "before":  //long
                        query.put(rule.getDataField()).lessThanEquals(Long.valueOf(rule.getRuleParameters()));
                        break;
                    case "after":  //long
                        query.put(rule.getDataField()).greaterThanEquals(Long.valueOf(rule.getRuleParameters()));
                        break;
                    case "between"://Number
                        String[] params = rule.getRuleParameters().split(",");
                        if(params[0].contains("."))
                            query.put(rule.getDataField()).greaterThanEquals(Double.valueOf(params[0]));
                        else
                            query.put(rule.getDataField()).greaterThanEquals(Long.valueOf(params[0]));

                        if(params[1].contains("."))
                            query.put(rule.getDataField()).lessThanEquals(Double.valueOf(params[1]));
                        else
                            query.put(rule.getDataField()).lessThanEquals(Long.valueOf(params[1]));
                        break;
                    case "coordinates"://Location
//                        st = new StringTokenizer(rule.getRuleParameters(), ",");
//                        query.put(rule.getDataField()).withinBox(
//                                Double.valueOf(String.valueOf(st.nextElement()))
//                                , Double.valueOf(String.valueOf(st.nextElement()))
//                                , Double.valueOf(String.valueOf(st.nextElement()))
//                                , Double.valueOf(String.valueOf(st.nextElement()))
//                        );
                        System.out.println("Coordinates selected...  ");
                        String coord = rule.getRuleParameters();
                        String[] coordarr =coord.split(",");
                        System.out.println("Rule DataField:"+rule.getDataField());
                        System.out.println("coord "+ coord);
                        query.put(rule.getDataField()).withinBox(Double.valueOf(String.valueOf(coordarr[0])), Double.valueOf(String.valueOf(coordarr[1])), Double.valueOf(String.valueOf(coordarr[2])), Double.valueOf(String.valueOf(coordarr[3])));
//                        final BasicDBObject query
//                                = new BasicDBObject("loc", new BasicDBObject("$within", new BasicDBObject("$box", box)));
                        break;
                    case "address"://Location
                        query.put(rule.getDataField()).equals(rule.getRuleParameters());
                        break;
                    case "radius"://Location
                        st = new StringTokenizer(rule.getRuleParameters(), ",");
                        query.put(rule.getDataField()).withinCenter(
                                Double.valueOf(String.valueOf(st.nextElement()))
                                , Double.valueOf(String.valueOf(st.nextElement()))
                                , Double.valueOf(String.valueOf(st.nextElement()))
                        );
                        break;
                    default:
                        LOGGER.info("Invalid Query Operator");
                }
            }

            String[] limitFields = rules.getExtractFields().split(",");
            BasicDBObjectBuilder queryFields = BasicDBObjectBuilder.start()
                    .add("stt_id", 1).add("stt_when", 1).add("stt_where", 1).add("stt_value",1).add("_id", 0);   // default fields
            for(String field: limitFields){
                if(field != "")
                    queryFields.add(field,1);
            }
            //DBCursor dbCursor = collection.find(query.get());
            LOGGER.info("queries: " + query.get().toString() + ", fields: " + queryFields.get());

            DBCursor dbCursor = collection.find(query.get(), queryFields.get());
            StringBuffer result = new StringBuffer();
//            result.append("[");
//            System.out.println("Mongo Route: Extract field");
            result.append("[");
            while (dbCursor.hasNext()) {
                DBObject next = dbCursor.next();
                st = new StringTokenizer(rules.getExtractFields(), ",");
                System.out.println("DB LINE: "+next.toString());

//                while (st.hasMoreElements()) {
//                    String field = String.valueOf(st.nextElement());
//                    result.append("\"" + field + "\"" + " : " + next.get(field));
//                    System.out.println(field + ":" + next.get(field));
//                    if (st.hasMoreElements()) {
//                        result.append(", ");
//                    }
//                }
//                result.append(next);
//                if (dbCursor.hasNext()) {
//                    result.append(",");
//                }
//
                //result.append(next.toString());
                LOGGER.debug("DB LINE: " + next.toString());
                result.append(next);
                if (dbCursor.hasNext()) {
                    result.append(",");
                }
            }
            result.append("]");
            System.out.println("Applied Rules: "+ result );
            return result;


        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        } finally{
            mongoConn.close();
        }

    }
}

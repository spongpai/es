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
import java.util.StringTokenizer;

/**
 * Created by siripen on 2/28/16.
 */
@Path("/sttwebservice")
public class STTWebService {
    private final static Logger LOGGER = LoggerFactory.getLogger(STTWebService.class);
    RuleDao ruleDAO = new RuleDao();



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
    public String searchSTTwithing(
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
}

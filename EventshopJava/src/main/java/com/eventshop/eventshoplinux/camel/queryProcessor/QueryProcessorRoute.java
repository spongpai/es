package com.eventshop.eventshoplinux.camel.queryProcessor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by nandhiniv on 5/19/15.
 */
public class QueryProcessorRoute extends RouteBuilder {


    protected Log log = LogFactory.getLog(this.getClass().getName());

    @Override
    public void configure() throws Exception {

//
        from("direct:queryInit")
                .to("direct:masterActor")

        ;
        from("direct:queryScript")
                .to("exec:wc?args=--words /var/www/html/temp/ds9070.txt")
                .process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        // By default, the body is ExecResult instance
                        //assertIsInstanceOf(ExecResult.class, exchange.getIn().getBody());
                        // Use the Camel Exec String type converter to convert the ExecResult to String
                        // In this case, the stdout is considered as output
                        String wordCountOutput = exchange.getIn().getBody(String.class);
                        // do something with the word count
                        System.out.println("WORD COUNT: " + wordCountOutput);
                    }
                });
    }

}

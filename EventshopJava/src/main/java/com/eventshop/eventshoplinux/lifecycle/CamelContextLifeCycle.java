package com.eventshop.eventshoplinux.lifecycle;

import com.eventshop.eventshoplinux.util.commonUtil.Config;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.apache.camel.component.servletlistener.CamelContextLifecycle;
import org.apache.camel.component.servletlistener.ServletCamelContext;
import org.apache.camel.impl.JndiRegistry;

/**
 * Created by nandhiniv on 5/6/15.
 */
public class CamelContextLifeCycle implements CamelContextLifecycle<JndiRegistry> {
    MongoClient mongoClient = null;
    @Override
    public void beforeStart(ServletCamelContext servletCamelContext, JndiRegistry jndiRegistry) throws Exception {
        if(mongoClient == null)
            mongoClient = new MongoClient(Config.getProperty("mongoHost"));

        jndiRegistry.bind("mongoBean", mongoClient);

    }

    @Override
    public void afterStart(ServletCamelContext servletCamelContext, JndiRegistry jndiRegistry) throws Exception {

    }

    @Override
    public void beforeStop(ServletCamelContext servletCamelContext, JndiRegistry jndiRegistry) throws Exception {

    }

    @Override
    public void afterStop(ServletCamelContext servletCamelContext, JndiRegistry jndiRegistry) throws Exception {
        mongoClient.close();
    }

    @Override
    public void beforeAddRoutes(ServletCamelContext servletCamelContext, JndiRegistry jndiRegistry) throws Exception {

    }

    @Override
    public void afterAddRoutes(ServletCamelContext servletCamelContext, JndiRegistry jndiRegistry) throws Exception {

    }
}

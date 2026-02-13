package com.venia.core.models.commerce;

import com.shopify.graphql.support.AbstractResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple OSGi component that runs code on bundle startup and shutdown.
 */
@Component(immediate = true)
public class MyStartupService {

    private static final Logger LOG = LoggerFactory.getLogger(MyStartupService.class);

    @Activate
    protected void activate() {
        // Set system property to enable custom simple fields when bundle starts
        System.setProperty(AbstractResponse.UNLOCK_CUSTOM_UNSAFE_FIELDS_PROPERTY, "true");
        LOG.info("MyStartupService bundle started");
    }

    @Deactivate
    protected void deactivate() {
        // Set system property to disable custom simple fields when bundle stops
        System.setProperty(AbstractResponse.UNLOCK_CUSTOM_UNSAFE_FIELDS_PROPERTY, "false");
        LOG.info("MyStartupService bundle stopped");
    }
}
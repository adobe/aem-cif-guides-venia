package com.venia.core.models.commerce;

import com.shopify.graphql.support.AbstractResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * A simple OSGi component that runs code on bundle startup and shutdown.
 */
@Component(immediate = true)
public class MyStartupService {

    @Activate
    protected void activate() {
        // Set system property to enable custom simple fields when bundle starts
        System.setProperty(AbstractResponse.UNLOCK_CUSTOM_UNSAFE_FIELDS_PROPERTY, "true");
        System.out.println("Bundle started!");
    }

    @Deactivate
    protected void deactivate() {
        // Set system property to disable custom simple fields when bundle stops
        System.setProperty(AbstractResponse.UNLOCK_CUSTOM_UNSAFE_FIELDS_PROPERTY, "false");
        System.out.println("Bundle stopped!");
    }
}
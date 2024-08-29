package com.venia.core.models.commerce;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;

import com.adobe.granite.ui.components.ValueMapResourceWrapper;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = MyProductReference.class,
        resourceType = {
                "cif/cfm/admin/components/productreference"
        })
public class MyProductReferenceImpl implements MyProductReference {
    static final String PN_SELECTION_ID = "selectionId";
    static final String SELECTION_ID_VALUE = "combinedSku";
    static final String PN_ENABLE_PREVIEW_DATE_FILTER = "enablePreviewDateFilter";
    static final String PN_TRACKING = "tracking";
    static final String PN_TRACKING_VALUE = "ON";
    static final String PN_TRACKING_ELEMENT = "trackingElement";
    static final String PN_TRACKING_ELEMENT_VALUE = "product reference";
    static final String PN_TRACKING_FEATURE = "trackingFeature";
    static final String PN_TRACKING_FEATURE_VALUE = "aem:cif:contentfragment";
    static final String PN_SHOW_LINK = "showLink";

    @Inject
    protected Resource resource;

    private Resource renderResource;

    @PostConstruct
    protected void initModel() {
        renderResource = new ValueMapResourceWrapper(resource, resource.getResourceType());
        ValueMap properties = renderResource.getValueMap();
        properties.putAll(resource.getValueMap());
        properties.put(PN_SELECTION_ID, SELECTION_ID_VALUE);
        properties.put(PN_ENABLE_PREVIEW_DATE_FILTER, Boolean.TRUE);
        properties.put(PN_SHOW_LINK, Boolean.TRUE);

        // Enable Omega tracking
        properties.put(PN_TRACKING, PN_TRACKING_VALUE);
        properties.put(PN_TRACKING_ELEMENT, PN_TRACKING_ELEMENT_VALUE);
        properties.put(PN_TRACKING_FEATURE, PN_TRACKING_FEATURE_VALUE);
    }

    @Override
    public Resource getRenderResource() {
        return renderResource;
    }
}

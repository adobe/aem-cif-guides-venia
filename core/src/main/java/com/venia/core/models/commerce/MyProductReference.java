package com.venia.core.models.commerce;

import org.apache.sling.api.resource.Resource;

public interface MyProductReference {
    /**
     * @return a resource preconfigured for productfield rendering
     */
    Resource getRenderResource();
}

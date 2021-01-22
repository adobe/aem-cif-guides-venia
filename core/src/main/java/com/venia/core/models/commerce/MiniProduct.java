package com.venia.core.models.commerce;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface MiniProduct {

    String getName();

    String getImage();

    String getUrlKey();

}

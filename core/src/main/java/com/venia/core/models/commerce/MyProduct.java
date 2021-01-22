package com.venia.core.models.commerce;

import com.adobe.cq.commerce.core.components.models.product.Product;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface MyProduct extends Product {

    String getCountryOfManufacture();

}

package com.venia.core.models.commerce;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.MediaGalleryInterface;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = MiniProduct.class,
    resourceType = MiniProductImpl.RESOURCE_TYPE
)
public class MiniProductImpl implements MiniProduct {

    protected static final String RESOURCE_TYPE = "venia/components/commerce/miniproduct";

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Page currentPage;

    @Inject
    private Resource resource;

    private ProductInterface product;
    private MagentoGraphqlClient magentoGraphqlClient;

    @PostConstruct
    private void initModel() {
        magentoGraphqlClient = MagentoGraphqlClient.create(resource, currentPage, request);

        // You can fetch this from a JCR property
        String identifier = "VT12";

        fetchProduct(identifier);
    }

    private void fetchProduct(String identifier) {
        // Define GraphQL query
        String query = Operations.query(q -> q
            .products(
                s -> s
                    .filter(
                        new ProductAttributeFilterInput().setSku(
                            new FilterEqualTypeInput().setEq(identifier))),
                i -> i
                    .items(p -> p
                        .name()
                        .urlKey()
                        .mediaGallery(m -> m
                            .url())))).toString();

        // Use GraphQL client to send query to Magento
        GraphqlResponse<Query, Error> response = magentoGraphqlClient.execute(query);

        // Get Query model from response data
        Query rootQuery = response.getData();

        // Get list of products
        List<ProductInterface> products = rootQuery.getProducts().getItems();

        // Select the first product in the list
        if (products.size() > 0) {
            product = products.get(0);
        }
    }

    @Override public String getName() {
        return product != null ? product.getName() : "";
    }

    @Override public String getImage() {
        if (product == null) {
            return null;
        }

        List<MediaGalleryInterface> images = product.getMediaGallery();

        if (images.size() == 0) {
            return null;
        }

        return images.get(0).getUrl();
    }

    @Override public String getUrlKey() {
        return product != null ? product.getUrlKey() : "";
    }
}

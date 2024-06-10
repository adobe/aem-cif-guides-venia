package com.venia.core.models.commerce.services;


import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class CommerceComponentModelFinderTest {

    @Mock
    ModelFactory modelFactory;

    @Mock
    private Resource resource;

    private final AemContext context = new AemContext();

    @InjectMocks
    private CommerceComponentModelFinder finder;

    @BeforeEach
    void setUp() {
        //finder.modelFactory = modelFactory;
    }

    @Test
    void findProductComponentModelReturnsNullWhenNoProductComponent() {
        Product product = finder.findProductComponentModel(context.request());
        assertNull(product);
    }

    @Test
    void findProductComponentModelReturnsProductWhenProductComponentExists() {
        Product mockProduct = mock(Product.class);
        Resource mockResource = mock(Resource.class);
        Resource mockChildResource = mock(Resource.class);

        // Create a list of mock resource types
        List<String> mockResourceTypes = Arrays.asList("mockResourceType1", "mockResourceType2");

        when(mockResource.getChildren()).thenReturn(Collections.singletonList(mockChildResource));
        when(mockChildResource.isResourceType(anyString())).thenReturn(true);
        when(modelFactory.getModelFromWrappedRequest(any(), any(), eq(Product.class))).thenReturn(mockProduct);
        Product product = finder.findComponentModel(context.request(), mockResource, mockResourceTypes, Product.class);
        assertNotNull(product);
        assertEquals(mockProduct, product);
    }

    @Test
    void findProductListComponentModelReturnsNullWhenNoProductListComponent() {
        ProductList productList = finder.findProductListComponentModel(context.request());
        assertNull(productList);
    }

    @Test
    void findProductListComponentModelReturnsProductListWhenProductListComponentExists() {
        ProductList mockProductList = mock(ProductList.class);
        Resource mockResource = mock(Resource.class);
        Resource mockChildResource = mock(Resource.class);

        // Create a list of mock resource types
        List<String> mockResourceTypes = Arrays.asList("mockResourceType1", "mockResourceType2");
        when(mockResource.getChildren()).thenReturn(Collections.singletonList(mockChildResource));
        when(mockChildResource.isResourceType(anyString())).thenReturn(true);
        when(modelFactory.getModelFromWrappedRequest(any(), any(), eq(ProductList.class))).thenReturn(mockProductList);

        ProductList productList = finder.findComponentModel(context.request(), mockResource, mockResourceTypes, ProductList.class);
        assertNotNull(productList);
        assertEquals(mockProductList, productList);
    }
}

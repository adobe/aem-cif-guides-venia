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
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
class CommerceComponentModelFinderTest {

    @Mock
    ModelFactory modelFactory;

    @Mock
    private Resource resource;

    @Mock
    private MockSlingHttpServletRequest request;

    @Mock
    private Product mockProduct;


    private final AemContext context = new AemContext();

    @InjectMocks
    private CommerceComponentModelFinder finder;

    @BeforeEach
    void setUp() {;
    }

    @Test
    void findProductComponentModelReturnsNullWhenNoProductComponent() {
        Product product = finder.findProductComponentModel(context.request());
        assertNull(product);
    }

    @Test
    void findProductComponentModelReturnsProductWhenProductComponentExists() {

        Resource mockChildResource = mock(Resource.class);

        when(request.getResource()).thenReturn(resource);
        when(resource.getChildren()).thenReturn(Collections.singletonList(mockChildResource));
        when(mockChildResource.isResourceType(anyString())).thenReturn(true);
        when(modelFactory.getModelFromWrappedRequest(any(), any(), eq(Product.class))).thenReturn(mockProduct);
        Product product = finder.findProductComponentModel(request);
        assertNotNull(product);
        assertEquals(mockProduct, product);
    }

    @Test
    void findProductComponentModelWithResourceReturnsNullWhenNoProductComponent() {
        Product product = finder.findProductComponentModel(request, resource);
        assertNull(product);
    }

    @Test
    void findProductComponentModelWithResourceReturnsProductWhenProductComponentExists() {
        Resource mockChildResource = mock(Resource.class);
        when(resource.getChildren()).thenReturn(Collections.singletonList(mockChildResource));
        when(mockChildResource.isResourceType(anyString())).thenReturn(true);
        when(modelFactory.getModelFromWrappedRequest(any(), any(), eq(Product.class))).thenReturn(mockProduct);
        Product product = finder.findProductComponentModel(request, resource);
        assertNotNull(product);
        assertEquals(mockProduct, product);
    }

    @Test
    void findProductListComponentModelReturnsNullWhenNoProductListComponent() {
        ProductList productList = finder.findProductListComponentModel(request);
        assertNull(productList);
    }

    @Test
    void findProductListComponentModelReturnsProductListWhenProductListComponentExists() {
        ProductList mockProductList = mock(ProductList.class);
        Resource mockChildResource = mock(Resource.class);

        // Create a list of mock resource types
        //List<String> mockResourceTypes = Arrays.asList("mockResourceType1", "mockResourceType2");
        when(request.getResource()).thenReturn(resource);
        when(resource.getChildren()).thenReturn(Collections.singletonList(mockChildResource));
        when(mockChildResource.isResourceType(anyString())).thenReturn(true);
        when(modelFactory.getModelFromWrappedRequest(any(), any(), eq(ProductList.class))).thenReturn(mockProductList);

        ProductList productList = finder.findProductListComponentModel(request);
        assertNotNull(productList);
        assertEquals(mockProductList, productList);
    }

    @Test
    void findProductListComponentModelWithResourceReturnsNullWhenNoProductListComponent() {
        ProductList productList = finder.findProductListComponentModel(request, resource);
        assertNull(productList);
    }

    @Test
    void findProductListComponentModelWithResourceReturnsProductListWhenProductListComponentExists() {
        ProductList mockProductList = mock(ProductList.class);
        Resource mockChildResource = mock(Resource.class);

        // Create a list of mock resource types
        //List<String> mockResourceTypes = Arrays.asList("mockResourceType1", "mockResourceType2");
        when(request.getResource()).thenReturn(resource);
        when(resource.getChildren()).thenReturn(Collections.singletonList(mockChildResource));
        when(mockChildResource.isResourceType(anyString())).thenReturn(true);
        when(modelFactory.getModelFromWrappedRequest(any(), any(), eq(ProductList.class))).thenReturn(mockProductList);

        ProductList productList = finder.findProductListComponentModel(request);
        assertNotNull(productList);
        assertEquals(mockProductList, productList);
    }
}

/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
package com.venia.core.models.commerce;

import com.adobe.cq.commerce.core.components.datalayer.ProductData;
import com.adobe.cq.commerce.core.components.models.common.CommerceIdentifier;
import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.productteaser.ProductTeaser;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.wcm.core.components.models.Component;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.scripting.WCMBindingsConstants;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.FieldReader;
import org.mockito.internal.util.reflection.FieldSetter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(AemContextExtension.class)
class MyProductTeaserImplTest {

    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String PAGE = "/content/page";
    private static final String PRODUCTTEASER_NO_BADGE = "productteaser-no-badge";
    private static final String PRODUCTTEASER_BADGE_FALSE = "productteaser-badge-false";
    private static final String PRODUCTTEASER_BADGE_TRUE_NO_AGE = "productteaser-badge-true-no-age";
    private static final String PRODUCTTEASER_BADGE_TRUE_WITH_AGE = "productteaser-badge-true-with-age";

    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private MyProductTeaser underTest;

    private ProductTeaser productTeaser;

    private ConfigurationBuilder mockConfigBuilder;

    @Mock
    private AbstractProductRetriever productRetriever;

    @Mock
    private ProductInterface product;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(productRetriever.fetchProduct()).thenReturn(product);
        Mockito.when(product.getCreatedAt()).thenReturn("2020-01-01 00:00:00");

        Page page = context.create().page(PAGE);
        createResource(page, PRODUCTTEASER_NO_BADGE, null, null);
        createResource(page, PRODUCTTEASER_BADGE_FALSE, false, null);
        createResource(page, PRODUCTTEASER_BADGE_TRUE_NO_AGE, true, null);
        createResource(page, PRODUCTTEASER_BADGE_TRUE_WITH_AGE, true, 3);

        context.addModelsForClasses(MyProductTeaserImpl.class);
    }

    void createResource(Page page, String name, Object badge, Object age) {
        Map<String, Object> props = new HashMap<>();
        props.put("sling:resourceType", "venia/components/commerce/productteaser");
        props.put("sling:resourceSuperType", "core/cif/components/commerce/productteaser/v1/productteaser");
        if (badge != null) {
            props.put("badge", badge);
            if (age != null) {
                props.put("age", age);
            }
        }
        context.create().resource(page, name, props);
    }

    void setup(String resourceName) throws Exception {
        Page page = context.currentPage(PAGE);
        context.currentResource(PAGE + "/jcr:content/" +resourceName);
        Resource teaserResource = context.resourceResolver().getResource(PAGE + "/jcr:content/" +resourceName);

        // This sets the page attribute injected in the models with @Inject or @ScriptVariable
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.setResource(teaserResource);
        slingBindings.put(WCMBindingsConstants.NAME_CURRENT_PAGE, page);
        slingBindings.put(WCMBindingsConstants.NAME_PROPERTIES, teaserResource.getValueMap());

        context.registerService(UrlProvider.class, Mockito.mock(UrlProvider.class));
        context.registerAdapter(SlingHttpServletRequest.class, Component.class, new Component() {});

        underTest = context.request().adaptTo(MyProductTeaser.class);
        Assertions.assertNotNull(underTest);

        Class<? extends MyProductTeaser> clazz = underTest.getClass();
        productTeaser = Mockito.spy((ProductTeaser)(new FieldReader(underTest, clazz.getDeclaredField("productTeaser")).read()));
        FieldSetter.setField(underTest, clazz.getDeclaredField("productTeaser"), productTeaser);
        FieldSetter.setField(underTest, clazz.getDeclaredField("productRetriever"), productRetriever);
    }

    @ParameterizedTest
    @ValueSource(strings = {PRODUCTTEASER_NO_BADGE, PRODUCTTEASER_BADGE_FALSE, PRODUCTTEASER_BADGE_TRUE_NO_AGE})
    void testShowBadge_false(String resourcePath) throws Exception {
        setup(resourcePath);
        Assertions.assertNotNull(underTest);
        Assertions.assertFalse(underTest.isShowBadge());
    }

    @Test
    void testShowBadge_true() throws Exception {
        setup(PRODUCTTEASER_BADGE_TRUE_WITH_AGE);
        Mockito.when(product.getCreatedAt()).thenReturn(LocalDateTime.now().minusDays(1).format(formatter));

        Assertions.assertNotNull(underTest);
        Assertions.assertTrue(underTest.isShowBadge());
    }

    @Test
    void testGetName() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn("TestName").when(productTeaser).getName();
        Assertions.assertEquals("TestName", underTest.getName());
    }

    @Test
    void testGetPriceRange() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Price priceRange = Mockito.mock(Price.class);
        Mockito.doReturn("USD 15.2").when(priceRange).getFormattedFinalPrice();
        Mockito.doReturn(priceRange).when(productTeaser).getPriceRange();
        Assertions.assertEquals(priceRange, underTest.getPriceRange());
        Assertions.assertEquals("USD 15.2", underTest.getPriceRange().getFormattedFinalPrice());
    }

    @Test
    void testGetImage() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn("TestImage").when(productTeaser).getImage();
        Assertions.assertEquals("TestImage", underTest.getImage());
    }

    @Test
    void testGetImageAlt() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn("TestImageAltText").when(productTeaser).getImageAlt();
        Assertions.assertEquals("TestImageAltText", underTest.getImageAlt());
    }

    @Test
    void testGetUrl() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn("TestUrl").when(productTeaser).getUrl();
        Assertions.assertEquals("TestUrl", underTest.getUrl());
    }

    @Test
    void testGetSku() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn("TestSKU").when(productTeaser).getSku();
        Assertions.assertEquals("TestSKU", underTest.getSku());
    }

    @Test
    public void testGetCallToAction() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn("TestCTA").when(productTeaser).getCallToAction();
        Assertions.assertEquals("TestCTA", underTest.getCallToAction());
    }

    @Test
    public void testGetCallToActionText() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn("TestCTAText").when(productTeaser).getCallToActionText();
        Assertions.assertEquals("TestCTAText", underTest.getCallToActionText());
    }

    @Test
    public void testGetCallToActionCommand() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn("TestCTACommand").when(productTeaser).getCallToActionCommand();
        Assertions.assertEquals("TestCTACommand", underTest.getCallToActionCommand());
    }

    @Test
    void testIsVirtualProduct() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn(true).when(productTeaser).isVirtualProduct();
        Assertions.assertTrue(underTest.isVirtualProduct());

        Mockito.doReturn(false).when(productTeaser).isVirtualProduct();
        Assertions.assertFalse(underTest.isVirtualProduct());
    }


    @Test
    void testGetCommerceIdentifier() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn(new CommerceIdentifier() {
            @Override public String getValue() {
                return "test-sku";
            }

            @Override public IdentifierType getType() {
                return IdentifierType.SKU;
            }

            @Override public EntityType getEntityType() {
                return EntityType.PRODUCT;
            }
        }).when(productTeaser).getCommerceIdentifier();

        Assertions.assertNotNull(underTest);
        CommerceIdentifier commerceIdentifier = underTest.getCommerceIdentifier();
        Assertions.assertEquals("test-sku", commerceIdentifier.getValue());
        Assertions.assertEquals(CommerceIdentifier.EntityType.PRODUCT, commerceIdentifier.getEntityType());
        Assertions.assertEquals(CommerceIdentifier.IdentifierType.SKU, commerceIdentifier.getType());
    }

    @Test
    void testGetId() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        Mockito.doReturn("test-id").when(productTeaser).getId();
        Assertions.assertEquals("test-id", underTest.getId());
    }

    @Test
    public void testDataLayerFeature() throws Exception {
        setup(PRODUCTTEASER_NO_BADGE);
        String dataLayerJson = "{ "
            + " \"test-id\": {"
            + " \"xdm:SKU\": \"test-sku\","
            + " \"xdm:listPrice\": 10.2,"
            + " \"xdm:currencyCode\": \"USD\","
            + " \"dc:title\": \"Test Product Teaser\","
            + " \"repo:modifyDate\": \"2020-10-12T10:20:34Z\","
            + " \"@type\": \"venia/components/commerce/productteaser\""
            + " }"
            + " }";
        ProductData mockProductData = Mockito.mock(ProductData.class);
        Mockito.doReturn(dataLayerJson).when(mockProductData).getJson();
        Mockito.doReturn(mockProductData).when(productTeaser).getData();

        Assertions.assertEquals(dataLayerJson, underTest.getData().getJson());
    }
}

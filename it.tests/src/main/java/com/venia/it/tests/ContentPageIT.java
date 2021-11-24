package com.venia.it.tests;

import java.util.Collections;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.junit.Test;

public class ContentPageIT extends CommerceTestBase {

    @Test
    public void testPageNotFound() throws ClientException {
        String pagePath = VENIA_CONTENT_US_EN + "/unknown-page.html";
        List<NameValuePair> params = Collections.singletonList(new BasicNameValuePair("wcmmode","disabled"));

        adminAuthor.doGet(pagePath, params, 404);
    }
}

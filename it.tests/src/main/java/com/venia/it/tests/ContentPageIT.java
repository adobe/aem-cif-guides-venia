package com.venia.it.tests;

import java.util.Collections;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContentPageIT extends CommerceTestBase {

    @Test
    public void testPageNotFound() throws ClientException {
        String pagePath = VENIA_CONTENT_US_EN + "/unknown-page.html";
        List<NameValuePair> params = Collections.singletonList(new BasicNameValuePair("wcmmode","disabled"));
        SlingHttpResponse response = adminAuthor.doGet(pagePath, params, 404);
        Document doc = Jsoup.parse(response.getContent());

        Elements elements = doc.select(H1_SELECTOR);
        assertEquals("Ruh-Roh! Page Not Found",elements.first().text());
    }
}

package com.venia.core.models.commerce;

import com.adobe.cq.commerce.core.components.services.urls.UrlProvider;
import com.adobe.cq.commerce.core.components.utils.SiteNavigation;
import com.adobe.cq.sightly.SightlyWCMMode;
import com.day.cq.commons.Externalizer;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = CommerceDownload.class,
    resourceType = CommerceDownloadImpl.RESOURCE_TYPE
)
public class CommerceDownloadImpl implements CommerceDownload {

    protected static final String RESOURCE_TYPE = "venia/components/commerce/download";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommerceDownloadImpl.class);

    private static final String DAM_ROOT = DamConstants.MOUNTPOINT_ASSETS;

    @Self
    private SlingHttpServletRequest request;

    @ScriptVariable
    private Page currentPage;

    @ScriptVariable(name = "wcmmode")
    private SightlyWCMMode wcmMode;

    @SlingObject
    private ResourceResolver resourceResolver;

    @OSGiService
    private UrlProvider urlProvider;

    @OSGiService
    private Externalizer externalizer;

    private List<CommerceDownloadItem> downloadItems = Collections.emptyList();

    @PostConstruct
    private void initModel() {
        // Only initialize when on a product page
        if (!SiteNavigation.isProductPage(currentPage)) {
            return;
        }

        // Get product SKU from the identifier in the URL
        String productSku = urlProvider.getProductIdentifier(request);
        if (StringUtils.isBlank(productSku)) {
            LOGGER.warn("Cannot find product sku for current page.");
            return;
        }

        // Only consider assets tagged with download
        final String tag = "download";

        // Search for assets
        downloadItems = findAssets(productSku, tag);
    }

    private String getQuery() {
        return "SELECT * FROM [dam:Asset]"
                + "WHERE isdescendantnode('" + DAM_ROOT + "')"
                + "AND contains(*, $identifier)"
                + "AND ([jcr:content/metadata/cq:products] = $identifier OR [jcr:content/metadata/cq:products] LIKE $identifierPrefix)"
                + "AND ([jcr:content/metadata/cq:tags] = $tag)"
                + "ORDER BY [jcr:created] ASC";
    }

    private List<CommerceDownloadItem> findAssets(String sku, String tag) {
        List<CommerceDownloadItem> assets = new ArrayList<>();

        try {
            Session session = resourceResolver.adaptTo(Session.class);
            Workspace workspace = session.getWorkspace();
            QueryManager queryManager = workspace.getQueryManager();
            ValueFactory valueFactory = session.getValueFactory();
            Query query = queryManager.createQuery(getQuery(), "JCR-SQL2");

            // Bind variables to query
            query.bindValue("identifier", valueFactory.createValue(sku));
            query.bindValue("identifierPrefix", valueFactory.createValue(sku + "#%"));
            query.bindValue("tag", valueFactory.createValue(tag));

            // Execute query
            QueryResult result = query.execute();
            NodeIterator nodes = result.getNodes();
            while(nodes.hasNext()) {
                Node node = nodes.nextNode();

                // Get resource for node or skip
                Resource resource = resourceResolver.getResource(node.getPath());
                if (resource == null) {
                    continue;
                }

                // Get asset for resource or skip
                Asset asset = resource.adaptTo(Asset.class);
                if (asset == null) {
                    continue;
                }

                // Create CommerceDownloadItem and add to list
                Long size = (Long) asset.getMetadata("dam:size");
                String sizeString = size / 1024 + " KB";
                String downloadLink;

                boolean isAuthorInstance = wcmMode != null && !wcmMode.isDisabled();
                if (isAuthorInstance) {
                    downloadLink = externalizer.authorLink(resourceResolver, asset.getPath());
                } else {
                    downloadLink = externalizer.publishLink(resourceResolver, asset.getPath());
                }

                assets.add(new CommerceDownloadItemImpl(asset.getName(), sizeString, downloadLink));
            }
        } catch(Exception e) {
            LOGGER.warn("Error while querying assets", e);
        }

        return assets;
    }

    @Override public List<CommerceDownloadItem> getDownloads() {
        return downloadItems;
    }

    private static class CommerceDownloadItemImpl implements CommerceDownloadItem {

        public CommerceDownloadItemImpl(String name, String size, String downloadLink) {
            this.name = name;
            this.size = size;
            this.downloadLink = downloadLink;
        }

        private String name;
        private String size;
        private String downloadLink;

        @Override public String getName() {
            return name;
        }

        @Override public String getSize() {
            return size;
        }

        @Override public String getDownloadLink() {
            return downloadLink;
        }
    }
}

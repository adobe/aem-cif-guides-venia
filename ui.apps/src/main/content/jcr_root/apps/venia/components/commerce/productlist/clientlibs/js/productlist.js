/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2023 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
"use strict";
// TODO: Document steps categories="[core.cif.productlist.v1]" on clientlibs/.content.xml
// then need to add this to the clientlib-cif/.content.xml embed="[core.cif.productlist.v1]"

const qaPLP =
  "https://plp-widgets-ui-qa.magento-datasolutions.com/v1/search.js";
const prodPLP = "https://plp-widgets-ui.magento-ds.com/v1/search.js";

class ProductList {
  constructor() {
    const stateObject = {
      categoryName: null,
      currentCategoryUrlPath: null,
    };
    this._state = stateObject;
    this._init();
  }
  _init() {
    this._initWidgetPLP();
  }

  _injectStoreScript(src) {
    const script = document.createElement("script");
    script.type = "text/javascript";
    script.src = src;

    document.head.appendChild(script);
  }

  async _getStoreData() {
    // get from session storage
    const sessionData = sessionStorage.getItem(
      "WIDGET_STOREFRONT_INSTANCE_CONTEXT"
    );
    // WIDGET_STOREFRONT_INSTANCE_CONTEXT is set from searchbar/clientlibs/js/searchbar.js
    // if not, we will need to retrieve from graphql separately here.

    if (sessionData) {
      this._state.dataServicesStorefrontInstanceContext =
        JSON.parse(sessionData);
      return;
    }
  }

  getStoreConfigMetadata() {
    const storeConfig = JSON.parse(
      document
        .querySelector("meta[name='store-config']")
        .getAttribute("content")
    );

    const { storeRootUrl } = storeConfig;
    const redirectUrl = storeRootUrl.split(".html")[0];
    return { storeConfig, redirectUrl };
  }

  async _initWidgetPLP() {
    if (!window.LiveSearchPLP) {
      this._injectStoreScript(qaPLP);
      // wait until script is loaded
      await new Promise((resolve) => {
        const interval = setInterval(() => {
          if (window.LiveSearchPLP && window.LiveSearchAutocomplete) {
            // Widget expects LiveSearchAutocomplete already loaded to rely on data-service-graphql
            clearInterval(interval);
            resolve();
          }
        }, 200);
      });
    }
    this._getStoreData();
    const { dataServicesStorefrontInstanceContext } = this._state;
    if (!dataServicesStorefrontInstanceContext) {
      console.log("no dataServicesStorefrontInstanceContext");
      return;
    }

    const root = document.getElementById("search-plp-root");
    if (!root) {
      console.log("plp root not found.");
      return;
    }
    // get dataset from root
    const categoryUrlPath = root.getAttribute("data-plp-urlPath") || "";
    const categoryName = root.getAttribute("data-plp-title") || "";
    const storeDetails = {
      environmentId: dataServicesStorefrontInstanceContext.environment_id,
      environmentType: dataServicesStorefrontInstanceContext.environment,
      apiKey: dataServicesStorefrontInstanceContext.api_key,
      websiteCode: dataServicesStorefrontInstanceContext.website_code,
      storeCode: dataServicesStorefrontInstanceContext.store_code,
      storeViewCode: dataServicesStorefrontInstanceContext.store_view_code,
      config: {
        pageSize: "8",
        perPageConfig: {
          pageSizeOptions: "12,24,36,48",
          defaultPageSizeOption: "12",
        },
        minQueryLength: "2",
        currencySymbol: "LBP",
        currencyRate: "1",
        displayOutOfStock: "1",
        allowAllProducts: "1",
        locale: "en_US",

        currentCategoryUrlPath: categoryUrlPath,
        categoryName,
        displayMode: "", //   "" for plp || "PAGE" for category/catalog
      },
      context: {
        customerGroup: dataServicesStorefrontInstanceContext.customer_group,
      },
      route: ({ sku }) => {
        return `${
          this.getStoreConfigMetadata().redirectUrl
        }.cifproductredirect.html/${sku}`;
      },
    };

    window.LiveSearchPLP({ storeDetails, root });
  }
}

(function () {
  function onDocumentReady() {
    new ProductList({});
  }

  if (document.readyState !== "loading") {
    onDocumentReady();
  } else {
    document.addEventListener("DOMContentLoaded", onDocumentReady);
  }
})();

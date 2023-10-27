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
console.log("searchbar.js");
const dataServicesStorefrontInstanceContextQuery = `
   query DataServicesStorefrontInstanceContext {
     dataServicesStorefrontInstanceContext {
       customer_group
       environment_id
       environment
       store_id
       store_view_id
       store_code
       store_view_code
       website_id
       website_name
       website_code
       store_url
       api_key
       store_name
       store_view_name
       base_currency_code
       store_view_currency_code
       catalog_extension_version
     }
     storeConfig {
       base_currency_code
       store_code
     }
   }
 `;
const dataServicesMagentoExtensionContextQuery = `
    query DataServicesStorefrontInstanceContext {
      dataServicesMagentoExtensionContext {
        magento_extension_version
      }
    }
  `;

async function getGraphQLQuery(query, variables = {}) {
  const graphqlEndpoint = `/api/graphql`;
  const response = await fetch(graphqlEndpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      query,
      variables,
    }),
  }).then((res) => res.json());

  return response.data;
}

class SearchBar {
  constructor() {
    const stateObject = {
      dataServicesStorefrontInstanceContext: null,
      magentoExtensionVersion: null,
      storeConfig: null,
    };
    this._state = stateObject;
    this._init();
  }
  _init() {
    this._initLiveSearch();
  }

  _injectStoreScript(src) {
    const script = document.createElement("script");
    script.type = "text/javascript";
    script.src = src;

    document.head.appendChild(script);
  }

  async _getStoreData() {
    const { dataServicesStorefrontInstanceContext, storeConfig } =
      (await getGraphQLQuery(dataServicesStorefrontInstanceContextQuery)) || {};
    this._state.dataServicesStorefrontInstanceContext =
      dataServicesStorefrontInstanceContext;
    this._state.storeConfig = storeConfig;

    if (!dataServicesStorefrontInstanceContext) {
      console.log("no dataServicesStorefrontInstanceContext");
      return;
    }
    // set session storage to expose for widget
    sessionStorage.setItem(
      "WIDGET_STOREFRONT_INSTANCE_CONTEXT",
      JSON.stringify(dataServicesStorefrontInstanceContext)
    );
  }

  async _getMagentoExtensionVersion() {
    const { dataServicesMagentoExtensionContext } =
      (await getGraphQLQuery(dataServicesMagentoExtensionContextQuery)) || {};
    this._state.magentoExtensionVersion =
      dataServicesMagentoExtensionContext?.magento_extension_version;
    if (!dataServicesMagentoExtensionContext) {
      console.log("no magentoExtensionVersion");
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

  async _initLiveSearch() {
    await Promise.all([
      this._getStoreData(),
      this._getMagentoExtensionVersion(),
    ]);
    if (!window.LiveSearchAutocomplete) {
      const liveSearchQaSrc =
        "https://searchautocompleteqa.magento-datasolutions.com/v0/LiveSearchAutocomplete.js";
      const liveSearchProdSrc =
        "https://livesearch-autocomplete.magento-ds.com/v0/LiveSearchAutocomplete.js";

      this._injectStoreScript(liveSearchQaSrc);
      // wait until script is loaded
      await new Promise((resolve) => {
        const interval = setInterval(() => {
          if (window.LiveSearchAutocomplete) {
            clearInterval(interval);
            resolve();
          }
        }, 200);
      });
    }

    const { dataServicesStorefrontInstanceContext } = this._state;
    if (!dataServicesStorefrontInstanceContext) {
      console.log("no dataServicesStorefrontInstanceContext");
      return;
    }

    // initialize live-search
    new window.LiveSearchAutocomplete({
      environmentId: dataServicesStorefrontInstanceContext.environment_id,
      websiteCode: dataServicesStorefrontInstanceContext.website_code,
      storeCode: dataServicesStorefrontInstanceContext.store_code,
      storeViewCode: dataServicesStorefrontInstanceContext.store_view_code,
      config: {
        pageSize: 8,
        minQueryLength: "2",
        currencySymbol: "$",
        currencyRate: "1",
        displayOutOfStock: true,
        allowAllProducts: false,
      },
      context: {
        customerGroup: dataServicesStorefrontInstanceContext.customer_group,
      },
      route: ({ sku }) => {
        return `${
          this.getStoreConfigMetadata().redirectUrl
        }.cifproductredirect.html/${sku}`;
      },
      searchRoute: {
        route: `${this.getStoreConfigMetadata().redirectUrl}/search.html`,
        query: "search_query",
      },
    });

    const formEle = document.getElementById("search_mini_form");

    formEle.setAttribute(
      "action",
      `${dataServicesStorefrontInstanceContext.store_url}catalogsearch/result`
    );
    // initialize store event after live-search
    this._initMetrics();
  }

  async _initMetrics() {
    //  Magento Store event

    // wait until script is magentoStorefrontEvents is found
    await new Promise((resolve) => {
      const interval = setInterval(() => {
        if (window.magentoStorefrontEvents) {
          clearInterval(interval);
          resolve();
        }
      }, 200);
    });

    const mse = window.magentoStorefrontEvents;

    const { dataServicesStorefrontInstanceContext, storeConfig } = this._state;

    const {
      base_currency_code,
      catalog_extension_version,
      environment,
      environment_id,
      store_code,
      store_id,
      store_name,
      store_url,
      store_view_code,
      store_view_id,
      store_view_name,
      store_view_currency_code,
      website_code,
      website_id,
      website_name,
    } = dataServicesStorefrontInstanceContext;

    console.log("initializing magento extension");
    mse.context.setMagentoExtension({
      magentoExtensionVersion: this._state.magentoExtensionVersion,
    });
    // mse.context.setShopper({ shopperId: "logged-in" }); // TODO:
    mse.context.setPage({
      pageType: "pdp",
      maxXOffset: 0,
      maxYOffset: 0,
      minXOffset: 0,
      minYOffset: 0,
      ping_interval: 5,
      pings: 1,
    });

    mse.context.setStorefrontInstance({
      environmentId: environment_id,
      //  instanceId, // TODO:
      environment: environment,
      storeUrl: store_url,
      websiteId: website_id,
      websiteCode: website_code,
      storeId: store_id,
      storeCode: store_code,
      storeViewId: store_view_id,
      storeViewCode: store_view_code,
      websiteName: website_name,
      storeName: store_name,
      storeViewName: store_view_name,
      baseCurrencyCode: base_currency_code,
      storeViewCurrencyCode: store_view_currency_code,
      catalogExtensionVersion: catalog_extension_version,
    });
  }
}

(function () {
  function onDocumentReady() {
    new SearchBar({});
  }

  if (document.readyState !== "loading") {
    onDocumentReady();
  } else {
    document.addEventListener("DOMContentLoaded", onDocumentReady);
  }
})();

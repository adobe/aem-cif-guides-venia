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
async function getStoreDataGraphQLQuery() {
  const graphqlEndpoint = `/api/graphql`;
  const response = await fetch(graphqlEndpoint, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      query: `
            query DataServicesStorefrontInstanceContext {
                dataServicesStorefrontInstanceContext {
                    catalog_extension_version
                    environment
                    environment_id
                    store_code
                    store_id
                    store_name
                    store_url
                    store_view_code
                    store_view_id
                    store_view_name
                    store_view_currency_code
                    website_code
                    website_id
                    website_name
                  }
                  storeConfig {
                    base_currency_code
                    store_code
                  }
                }
                `,
      variables: {},
    }),
  }).then((res) => res.json());

  return response.data;
}

class SearchBar {
  constructor() {
    const stateObject = {
      dataServicesStorefrontInstanceContext: null,
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
      (await getStoreDataGraphQLQuery()) || {};
    this._state.dataServicesStorefrontInstanceContext =
      dataServicesStorefrontInstanceContext;
    this._state.storeConfig = storeConfig;
    if (!dataServicesStorefrontInstanceContext) {
      console.log("no dataServicesStorefrontInstanceContext");
      return;
    }
  }

  async _initLiveSearch() {
    await this._getStoreData();
    if (!window.LiveSearchAutocomplete) {
      this._injectStoreScript(
        "https://searchautocompleteqa.magento-datasolutions.com/v0/LiveSearchAutocomplete.js"
      );
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

    // initialize live-search
    new window.LiveSearchAutocomplete({
      environmentId: dataServicesStorefrontInstanceContext.environment_id, // TODO: Test using
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
        customerGroup: "", // TODO:
      },
    });

    const formEle = document.getElementById("search_mini_form");

    formEle.setAttribute(
      "action",
      `${dataServicesStorefrontInstanceContext.store_url}catalogsearch/result`
    );
    // initialize store event after live-search
    this._initStoreEvent();
  }

  async _initStoreEvent() {
    //  Magento Store event
    if (!window.magentoStorefrontEvents) {
      this._injectStoreScript(
        "https://unpkg.com/@adobe/magento-storefront-events-sdk@qa/dist/index.js"
      );
      this._injectStoreScript(
        "https://unpkg.com/@adobe/magento-storefront-event-collector@qa/dist/index.js"
      );
    }
    // wait until script is loaded
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
      catalog_extension_version,
      environment,
      environment_id,
      store_code, // TODO: storeCode is also in storeConfig w/ diff value
      store_id,
      store_name,
      store_url,
      store_view_code,
      store_view_id,
      store_view_name,
      website_code,
      website_id,
      website_name,
      store_view_currency_code,
    } = dataServicesStorefrontInstanceContext;
    const { baseCurrencyCode /* , storeCode */ } = storeConfig;
    // mse.context.setMagentoExtension({
    //   magentoExtensionVersion: "1.0.0",
    // });
    // mse.context.setShopper({ shopperId: "logged-in" });
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
      // instanceId, // TODO:
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
      baseCurrencyCode,
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

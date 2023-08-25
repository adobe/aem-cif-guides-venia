"use strict";
async function getStoreDataGraphQLQuery() {
  let response;
  const graphqlEndpoint = `/api/graphql`;

  response = await fetch(graphqlEndpoint, {
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

class Searchbar {
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

  async _initLiveSearch() {
    const { dataServicesStorefrontInstanceContext, storeConfig } =
      (await getStoreDataGraphQLQuery()) || {};
    this._state.dataServicesStorefrontInstanceContext =
      dataServicesStorefrontInstanceContext;
    this._state.storeConfig = storeConfig;
    if (!dataServicesStorefrontInstanceContext) {
      console.log("no dataServicesStorefrontInstanceContext");
      return;
    }

    new window.LiveSearchAutocomplete({
      environmentId: dataServicesStorefrontInstanceContext.environment_id,
      websiteCode: dataServicesStorefrontInstanceContext.website_code,
      storeCode: dataServicesStorefrontInstanceContext.store_code,
      storeViewCode: dataServicesStorefrontInstanceContext.store_view_code,
    });
    const formEle = document.getElementById("search_mini_form");

    formEle.setAttribute(
      "action",
      `catalogsearch/result` // Verify this is correct for venia
      // `${dataServicesStorefrontInstanceContext.store_url}catalogsearch/result`
    );
    this._initStoreEvent();
  }

  _injectStoreEventScript(src) {
    const script = document.createElement("script");
    script.type = "text/javascript";
    script.src = src;
    document.head.appendChild(script);
  }

  async _initStoreEvent() {
    //  Magento Store event
    console.log(
      window.magentoStorefrontEvents,
      "window.magentoStorefrontEvents"
    );
    if (!window.magentoStorefrontEvents) {
      console.log("injecting event");
      this._injectStoreEventScript(
        "https://unpkg.com/@adobe/magento-storefront-events-sdk@qa/dist/index.js"
      );
      this._injectStoreEventScript(
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
    console.log(mse, "<-- mse");
    const { dataServicesStorefrontInstanceContext, storeConfig } = this._state;

    const {
      catalog_extension_version,
      environment,
      environment_id,
      store_code, // should we use this? or from storeConfig?
      store_id,
      store_name,
      store_url,
      store_view_code,
      store_view_id,
      store_view_name,
      website_code,
      website_id,
      // website_name,
      store_view_currency_code,
    } = dataServicesStorefrontInstanceContext;
    const { baseCurrencyCode, storeCode } = storeConfig;
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
      // instanceId, // TODO: Missing
      environment: environment,
      storeUrl: store_url,
      websiteId: website_id,
      websiteCode: website_code,
      storeId: store_id,
      storeCode: store_code,
      storeViewId: store_view_id,
      storeViewCode: store_view_code,
      websiteName,
      storeName: store_name,
      storeViewName: store_view_name,
      baseCurrencyCode,
      storeViewCurrencyCode: store_view_currency_code,
      catalogExtensionVersion: catalog_extension_version,
    });
  }
}
// FIXME: Fix onClick product search url
(function () {
  function onDocumentReady() {
    const searchBar = new Searchbar({});
  }

  if (document.readyState !== "loading") {
    onDocumentReady();
  } else {
    document.addEventListener("DOMContentLoaded", onDocumentReady);
  }
})();

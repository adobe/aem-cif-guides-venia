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
      `/catalogsearch/result` // Verify this is correct for venia
      // `${dataServicesStorefrontInstanceContext.store_url}catalogsearch/result`
    );
  }

  toggle() {
    if (this._state.visible) {
      this._hide();
    } else {
      this._show();
    }

    this._state.visible = !this._state.visible;
  }
}
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

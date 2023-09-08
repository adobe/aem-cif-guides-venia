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
const storeQuery = `
  query DataServicesStorefrontInstanceContext {
    dataServicesStorefrontInstanceContext {
      catalog_extension_version
      customer_group
      website_id
      website_name
    }
    storeConfig {
      base_currency_code
      store_code
    }
  }
`;

// async function getGraphQLQuery(query, variables = {}) {
// }

const qaPLP =
  "https://plp-widgets-ui-qa.magento-datasolutions.com/v1/search.js";
const prodPLP = "https://plp-widgets-ui.magento-ds.com/v1/search.js";

class ProductList {
  constructor() {
    const stateObject = {};
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

  async _getStoreData() {}
  async _getMagentoExtensionVersion() {}

  async _initWidgetPLP() {
    console.log("plp.js loading");
    this._getStoreData();
    if (!window.LiveSearchPLP) {
      this._injectStoreScript(qaPLP);
      // wait until script is loaded
      await new Promise((resolve) => {
        const interval = setInterval(() => {
          if (window.LiveSearchPLP) {
            clearInterval(interval);
            resolve();
          }
        }, 200);
      });
    }
    // const { dataServicesStorefrontInstanceContext } = this._state;
    // if (!dataServicesStorefrontInstanceContext) {
    //   console.log("no dataServicesStorefrontInstanceContext");
    //   return;
    // }

    const root = document.getElementById("search-plp-root");
    const storeDetails = {
      environmentId: "22500baf\u002D135e\u002D4b8f\u002D8f18\u002D14276de7d356",
      environmentType: "Testing",
      apiKey: "5280da1b5a174b4b89b70b497ad5b437",
      websiteCode: "base",
      storeCode: "main_website_store",
      storeViewCode: "default",
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
        currentCategoryUrlPath: "women\u002Ftops\u002Dwomen",
        // currentCategoryUrlPath: "men\u002Fbottoms\u002Dmen",
        categoryName: "Bottoms",
        displayMode: "",
        locale: "en_US",
      },
      context: {
        customerGroup: "b6589fc6ab0dc82cf12099d1c2d40ab994e8410c",
      },
    };
    console.log("initializing PLP widget");
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

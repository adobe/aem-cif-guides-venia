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
            this._state.dataServicesSessionContext = JSON.parse(sessionData);
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
            const liveSearchPlpSrc =
                "https://plp-widgets-ui.magento-ds.com/v1/search.js";
            this._injectStoreScript(liveSearchPlpSrc);
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
        await this._getStoreData();
        const { dataServicesSessionContext } = this._state;
        if (!dataServicesSessionContext) {
            console.log("no dataServicesSessionContext");
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
            environmentId: dataServicesSessionContext.environment_id,
            environmentType: dataServicesSessionContext.environment,
            apiKey: dataServicesSessionContext.api_key,
            websiteCode: dataServicesSessionContext.website_code,
            storeCode: dataServicesSessionContext.store_code,
            storeViewCode: dataServicesSessionContext.store_view_code,
            config: {
                pageSize: dataServicesSessionContext.page_size,
                perPageConfig: {
                    pageSizeOptions: dataServicesSessionContext.page_size_options,
                    defaultPageSizeOption:
                    dataServicesSessionContext.default_page_size_option,
                },
                minQueryLength: dataServicesSessionContext.min_query_length,
                currencySymbol: dataServicesSessionContext.currency_symbol,
                currencyRate: dataServicesSessionContext.currency_rate,
                displayOutOfStock: dataServicesSessionContext.display_out_of_stock,
                allowAllProducts: dataServicesSessionContext.allow_all_products,
                locale: dataServicesSessionContext.locale,
                currentCategoryUrlPath: categoryUrlPath,
                categoryName,
                displayMode: "", // "" for plp || "PAGE" for category/catalog
            },
            context: {
                customerGroup: dataServicesSessionContext.customer_group,
            },
            route: ({ sku }) => {
                return `${
                    this.getStoreConfigMetadata().redirectUrl
                }.cifproductredirect.html/${sku}`;
            },
            searchQuery: "search_query",
        };
        setTimeout(() => {
            console.log("init PLP");
            window.LiveSearchPLP({ storeDetails, root });
        }, 0);
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

/*******************************************************************************
 *
 *    ADOBE CONFIDENTIAL
 *     ___________________
 *
 *     Copyright 2021 Adobe
 *     All Rights Reserved.
 *
 *     NOTICE: All information contained herein is, and remains
 *     the property of Adobe and its suppliers, if any. The intellectual
 *     and technical concepts contained herein are proprietary to Adobe
 *     and its suppliers and are protected by all applicable intellectual
 *     property laws, including trade secret and copyright laws.
 *     Dissemination of this information or reproduction of this material
 *     is strictly forbidden unless prior written permission is obtained
 *     from Adobe.
 *
 ******************************************************************************/

(function($, ns, channel, window, undefined) {
    const controllersToWrap = [
        // handled by the AssetViewHandler
        'Images',
        'Videos',
        'Documents',
        'Content Fragments',
        '3D',
        'Manuscript',
        'Design Packages',
        // handled by PageViewHandler
        'Pages',
        // handled by ExperienceFragmentsViewHandler
        'Experience Fragments'
    ];

    const PRODUCT_SERVLET = '/bin/wcm/contentfinder/cifproduct/view.json';
    const PRODUCT_ITEM_RT = 'cif/assetfinder/product';

    /**
     * Wraps the original `loadAssets` function of the asset controller with a function that invokes the Product View Handler first and then crafts a new query string that also includes the SKUs (if any) returned from Magento
     * @param callback
     * @returns {function(...[*]=)}
     */
    function wrapLoadAssets(callback) {
        return function loadAssetsWrapped(query, lowerLimit, upperLimit) {
            if (query.length === 0) {
                // we have no query string so there's no need to look in the Commerce backend
                return callback(query, lowerLimit, upperLimit);
            }

            // the GQL parser will parse a query like
            //
            // <sku1> OR <sku2> OR <term1> <term2> => (<sku1> OR <sku2> OR <term1>) AND <term2>
            //
            // this is will not return the expected result set, however it seems there is no way to achieve a query like
            //
            // <sku1> OR <sku2> OR <term1> <term2> => <sku1> OR <sku2> OR (<term1> AND <term2>)
            //
            // alternatively, we can wrap the fulltext constraint in a phrase which will be parsed to
            //
            // <sku1> OR <sku2> OR "<term1> <term2>" => (<sku1> OR <sku2> OR <phrase>) AND ...
            //
            // to achieve this we use the fact that all constraints but the fulltext constraint relate to a property and so will
            // contain a colon. If not the whole query is the fulltext constraint;

            let colPos = query.indexOf(':');
            let fulltextConstraint = '';
            let otherConstraints;

            if (colPos > 0) {
                let spacePos = query.substr(0, colPos).lastIndexOf(' ');
                if (spacePos >= 0) {
                    fulltextConstraint = query.substr(0, spacePos);
                    otherConstraints = query.substr(spacePos + 1);
                } else {
                    // no fulltext constraint
                    otherConstraints = query;
                }
            } else {
                // no other constraints
                fulltextConstraint = query;
            }

            fulltextConstraint = fulltextConstraint.trim();

            if (fulltextConstraint.length === 0) {
                // without a fulltext constraint we don't need to query for products as we would search only for a subset of random
                // skus
                return callback(query, lowerLimit, upperLimit);
            }

            const param = {
                _dc: new Date().getTime(),
                query: fulltextConstraint,
                referer: ns.getPageInfoLocation(),
                // the excludedPath is used by the ExperienceFragmentsViewHandler
                excludedPath: ns.getPageInfoLocation(),
                itemResourceType: PRODUCT_ITEM_RT,
                limit: lowerLimit + '..' + upperLimit,
                _charset_: 'uft-8'
            };


           return callback(query, lowerLimit, upperLimit);
        };
    }

    // wrap loadAssets of newly registered controllers
    const originalRegisterController = ns.ui.assetFinder.register;
    ns.ui.assetFinder.register = (name, controller) => {
        if (controllersToWrap.indexOf(name) !== -1) {
            const originalLoadAssets = controller.loadAssets;
            controller.loadAssets = wrapLoadAssets(originalLoadAssets);
        }
        originalRegisterController(name, controller);
    };

    // wrap loadAssets of all already registered controllers
    Object.entries(ns.ui.assetFinder.registry)
        .filter(([name]) => controllersToWrap.indexOf(name) !== -1)
        .forEach(([name, originalAssetController]) => {
            const originalLoadAssets = originalAssetController.loadAssets;
            originalAssetController.loadAssets = wrapLoadAssets(originalLoadAssets);
        });
})(jQuery, Granite.author, jQuery(document), this);

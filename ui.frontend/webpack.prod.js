/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
const merge                   = require('webpack-merge');
const OptimizeCSSAssetsPlugin = require('optimize-css-assets-webpack-plugin');
const TerserPlugin            = require('terser-webpack-plugin');
const common                  = require('./webpack.common.js');

module.exports = merge(common('production'), {
    optimization: {
        minimize: true,
        usedExports: true,
        minimizer: [
            new TerserPlugin({
                terserOptions: {
                    mangle: true
                },
            }),
            new OptimizeCSSAssetsPlugin({
                cssProcessorPluginOptions: {
                    cssProcessor: require('cssnano'),
                    preset: [
                        'default',
                        {
                            calc: true,
                            convertValues: true,
                            discardComments: {
                                removeAll: true,
                            },
                            discardDuplicates: true,
                            discardEmpty: true,
                            mergeRules: true,
                            normalizeCharset: true,
                            reduceInitial: true, // This is since IE11 does not support the value Initial
                            svgo: true,
                        },
                    ],
                },
                canPrint: false,
            }),
        ]
    },
    devtool: 'none',
    performance: { hints: false }
});

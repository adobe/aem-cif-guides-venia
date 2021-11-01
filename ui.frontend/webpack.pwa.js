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
'use strict';

const path = require('path');
const pkg = require('./package.json');
const webpack = require('webpack');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const { DefinePlugin } = webpack;
const OptimizeCSSAssetsPlugin = require('optimize-css-assets-webpack-plugin');
const TerserPlugin = require('terser-webpack-plugin');

const { configureWebpack } = require('@magento/pwa-buildpack');

async function configureExtensions(options) {
    // Generate full webpack configuration as used by PWA Studio
    const config = await configureWebpack(options);

    // Extract the buildbus rule
    const rule = config.module.rules[1];

    // Extract the buildbus plugin
    const plugin = config.plugins[0];

    // Return only parts that are relevant for the extension mechanism
    return [rule, plugin];
};

module.exports = async env => {
    const { mode } = env;

    const SOURCE_ROOT = __dirname + '/src/main';
    const alias = Object.keys(pkg.dependencies)
        .reduce((obj, key) => ({ ...obj, [key]: path.resolve('node_modules', key) }), {});

    const [rule, plugin] = await configureExtensions({
        context: __dirname,
        vendor: [
            '@apollo/client',
            'apollo-cache-persist',
            'informed',
            'react',
            'react-dom',
            'react-feather',
            'react-redux',
            'react-router-dom',
            'redux',
            'redux-actions',
            'redux-thunk'
        ],
        special: {
            'react-feather': {
                esModules: true
            }
        },
        ...env
    });

    const config = {
        mode,
        entry: {
            site: SOURCE_ROOT + '/site/main.js'
        },
        output: {
            filename: 'clientlib-site/[name].js',
            chunkFilename: 'clientlib-site/[name].js',
            path: path.resolve(__dirname, 'dist')
        },
        module: {
            rules: [
                rule,
                {
                    test: /\.css$/,
                    include: /node_modules\/@magento/,
                    use: [
                        MiniCssExtractPlugin.loader,
                        {
                            loader: 'css-loader',
                            options: {
                                modules: {
                                    localIdentName: 'cmp-Venia[folder]__[name]__[local]'
                                }
                            }
                        }
                    ]
                },
                {
                    test: /\.css$/,
                    exclude: /node_modules\/@magento/,
                    use: [
                        MiniCssExtractPlugin.loader,
                        {
                            loader: 'css-loader',
                            options: {
                                modules: true
                            }
                        }
                    ]
                },
                {
                    test: /\.scss$/,
                    use: [
                        MiniCssExtractPlugin.loader,
                        {
                            loader: 'css-loader',
                            options: {
                                url: false,
                                import: true
                            }
                        },
                        {
                            loader: 'postcss-loader',
                            options: {
                                plugins() {
                                    return [require('autoprefixer')];
                                }
                            }
                        },
                        {
                            loader: 'sass-loader',
                            options: {
                                url: false
                            }
                        },
                        {
                            loader: 'webpack-import-glob-loader',
                            options: {
                                url: false
                            }
                        }
                    ]
                }
            ],
            strictExportPresence: true
        },
        resolve: {
            alias: {
                ...alias
            },
            extensions: ['.ee.js', '.js', '.json', '.wasm']
        },
        plugins: [
            plugin,
            new CleanWebpackPlugin(),
            new webpack.NoEmitOnErrorsPlugin(),
            new MiniCssExtractPlugin({
                filename: 'clientlib-site/[name].css'
            }),
            new CopyWebpackPlugin([
                {
                    from: path.resolve(__dirname, SOURCE_ROOT + '/resources'),
                    to: './clientlib-site/'
                }
            ]),
            new DefinePlugin({
                DEFAULT_COUNTRY_CODE: JSON.stringify('US')
            })
        ],
        optimization: {
            splitChunks: {
                cacheGroups: {
                    main: {
                        chunks: 'all',
                        name: 'site',
                        test: 'main',
                        enforce: true
                    },
                    // Merge all the CSS into one file
                    styles: {
                        name: 'styles',
                        test: /\.s?css$/,
                        chunks: 'all',
                        enforce: true,
                    },
                }
            },
            minimize: false,
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
        stats: {
            assetsSort: 'chunks',
            builtAt: true,
            children: false,
            chunkGroups: true,
            chunkOrigins: true,
            colors: false,
            errors: true,
            errorDetails: true,
            env: true,
            modules: false,
            performance: true,
            providedExports: false,
            source: false,
            warnings: true
        },
        devtool: 'inline-source-map',
        performance: { hints: false }
    };

    console.log('FINAL CONFIG', JSON.stringify(config, null, 4));

    return config;
};
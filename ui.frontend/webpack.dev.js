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
const merge             = require('webpack-merge');
const common            = require('./webpack.common.js');
const path              = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const SOURCE_ROOT = __dirname + '/src/main';

module.exports = merge(common, {
    mode: 'development',
    devtool: 'inline-source-map',
    performance: { hints: 'warning' },
    plugins: [
        new HtmlWebpackPlugin({
            template: path.resolve(__dirname, SOURCE_ROOT + '/static/index.html')
        })
    ],
    resolve: {
        // during development we may have dependencies which are linked in node_modules using either `npm link`
        // or `npm install <file dir>`. Those dependencies will bring *all* their dependencies along, because
        // in that case npm ignores the "devDependencies" setting.
        // In that case, we need to make sure that this project using its own version of React libraries.

        alias: {
            react: path.resolve('./node_modules/react'),
            'react-dom': path.resolve('./node_modules/react-dom'),
            'react-i18next': path.resolve('./node_modules/react-i18next')
        }
    },
    devServer: {
        inline: true,
        proxy: [
            {
                context: ['/content', '/etc.clientlibs'],
                target: 'http://localhost:4502'
            }
        ]
    }
});

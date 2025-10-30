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

module.exports = merge(common('development'), {
    devtool: 'eval-source-map',
    performance: { hints: 'warning' },
    module: {
        strictExportPresence: true 
    },
    plugins: [
        new HtmlWebpackPlugin({
            template: path.resolve(__dirname, SOURCE_ROOT + '/static/index.html')
        })
    ],
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

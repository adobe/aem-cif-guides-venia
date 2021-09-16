const peregrine = require('@magento/babel-preset-peregrine');

module.exports = (api, opts = {}) => {
    const config = {
        ...peregrine(api, opts),
        // important as some in combiation with babel-runtime and umd/esm mixed 
        // module the default value will cause the imports from the umd 
        // libraries (@adobe/) to be not recognized anymore
        sourceType: 'unambiguous'
    }

    // Remove react-refresh/babel as this causes issues with the modules 
    // extracted using the mini-css-extract-plugin. sources suggest to exclude
    // node_modules from babel which we cannot do because of peregrine. 
    // We should fix this in peregrine (make it configureable)
    config.plugins = config.plugins.filter(plugin => plugin !== 'react-refresh/babel');

    return config; 
}
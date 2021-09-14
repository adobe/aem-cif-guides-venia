module.exports = {
    presets: [
        [ '@magento/babel-preset-peregrine' ]
    ],
    // important as some in combiation with babel-runtime and umd/esm mixed 
    // module the default value will cause the imports from the umd libraries
    // (@adobe/) to be not recognized anymore
    sourceType: 'unambiguous'
}
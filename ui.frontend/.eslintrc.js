const headerBlock = [
    '******************************************************************************',
    ' *',
    {
        pattern: ' *    Copyright \\d{4} Adobe. All rights reserved.',
        template: ` *    Copyright ${new Date().getFullYear()} Adobe. All rights reserved.`,
    },
    ' *    This file is licensed to you under the Apache License, Version 2.0 (the "License");',
    ' *    you may not use this file except in compliance with the License. You may obtain a copy',
    ' *    of the License at http://www.apache.org/licenses/LICENSE-2.0',
    ' *',
    ' *    Unless required by applicable law or agreed to in writing, software distributed under',
    ' *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS',
    ' *    OF ANY KIND, either express or implied. See the License for the specific language',
    ' *    governing permissions and limitations under the License.',
    ' *',
    ' *****************************************************************************',
];

module.exports = {
    parser: 'babel-eslint', // Specifies the ESLint parser
    extends: [
        'plugin:@typescript-eslint/recommended', // Uses the recommended rules from the @typescript-eslint/eslint-plugin
        'plugin:react/recommended',
    ],
    parserOptions: {
        ecmaVersion: 2018, // Allows for the parsing of modern ECMAScript features
        sourceType: 'module', // Allows for the use of imports
    },
    settings: {
        react: {
            version: 'detect',
        },
    },
    plugins: ['react', 'header'],
    rules: {
        curly: 1,
        '@typescript-eslint/explicit-function-return-type': [0],
        '@typescript-eslint/no-explicit-any': [0],
        '@typescript-eslint/no-use-before-define': [0],
        '@typescript-eslint/camelcase': [0],
        'ordered-imports': [0],
        'object-literal-sort-keys': [0],
        'max-len': [1, 120],
        'new-parens': 1,
        'no-bitwise': 1,
        'no-cond-assign': 1,
        'no-trailing-spaces': 0,
        'eol-last': 1,
        'func-style': ['error', 'declaration', { allowArrowFunctions: true }],
        semi: 1,
        // override the default which is more restrictive
        'react/prop-types': ['warn', { ignore: ['children'] }],
        'no-var': 0,
        'header/header': [2, 'block', headerBlock],
    },
};

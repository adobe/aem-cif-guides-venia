console.log('CALLING INTERCEPT');

const { Targetables } = require('@magento/pwa-buildpack')


module.exports = targets => {

    const targetables = Targetables.using(targets);

    const Button = targetables.reactComponent('@magento/venia-ui/lib/components/Button/button.js');

    Button.appendJSX('<button />', '<span>Hello World!</span>');
};
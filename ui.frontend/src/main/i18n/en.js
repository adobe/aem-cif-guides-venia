/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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

import enMessagesCoreComponents from '@adobe/aem-core-cif-react-components/i18n/en.json';
import enMessagesProductRecs from '@adobe/aem-core-cif-product-recs-extension/i18n/en.json';
import enMessagesVenia from '@magento/venia-ui/i18n/en_US.json';
import enProject from '../../../i18n/en.json';

export const messages = { ...enMessagesVenia, ...enMessagesCoreComponents, ...enMessagesProductRecs, ...enProject };
export const locale = 'en';

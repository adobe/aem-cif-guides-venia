/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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
package com.venia.core.models.commerce;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = SlingHttpServletRequest.class, adapters = ProductList.class, resourceType = ProductListImpl.RESOURCE_TYPE)
public class ProductListImpl implements ProductList {

  protected static final String RESOURCE_TYPE = "venia/components/commerce/productlist";
  private static final String CATEGORY_PROPERTY = "category";

  // @Self
  // protected SlingHttpServletRequest request;
  @Self
  @Via("resource")
  protected ValueMap properties;

  // @ScriptVariable
  // private ValueMap properties;
  // private boolean catalogPage;

  @Override
  public Boolean isPLP() {
    if (properties == null) {
      // properties = request.getResource().getValueMap();
    }
      final boolean isCategory = properties.get("category", false);

      return isCategory;
  }

  @Override
  public String getTitle() {
      return "getTitle()";
  }
}

# Low Stock Badge — Custom Product Teaser Extension

A real-world AEM CIF customisation demonstrating how to extend the out-of-the-box
`ProductTeaser` component to surface live Magento inventory data in the AEM authoring
experience and on the published storefront.

## What it does

When a product's **only_x_left_in_stock** quantity is at or below a configurable
threshold and the product is still `IN_STOCK`, a red pill badge is rendered
bottom-left on the product teaser:

```
┌────────────────────────┐
│                        │
│   [product image]      │
│                        │
│ ┌──────────────┐       │
│ │ ONLY 3 LEFT! │       │
│ └──────────────┘       │
└────────────────────────┘
```

The badge is fully author-configurable per component instance.

## Architecture

This follows the standard AEM CIF extension pattern:

```
MyProductTeaser (interface)
    └── extends ProductTeaser (CIF Core)
            └── MyProductTeaserImpl (Sling Model)
                    └── delegates to ProductTeaser via @Via(ResourceSuperType)
                    └── extends GraphQL query with stockStatus + onlyXLeftInStock
```

### Files changed

| File | Change |
|------|--------|
| `core/.../MyProductTeaser.java` | Added `isLowStock()` and `getLowStockMessage()` to interface |
| `core/.../MyProductTeaserImpl.java` | Implemented both methods; extended GraphQL query |
| `core/.../MyProductTeaserImplTest.java` | 8 new unit tests covering all branches |
| `ui.apps/.../productteaser.html` | Added conditional low stock badge `<div>` |
| `ui.apps/.../_cq_dialog/.content.xml` | Added "Low Stock Badge" author dialog tab |
| `ui.frontend/.../\_productteaser.scss` | Added `.item__badge--low-stock` pill style |

## Author dialog

The component dialog gains a new **Low Stock Badge** tab with three fields:

| Field | Property | Default | Description |
|-------|----------|---------|-------------|
| Enable checkbox | `lowStockEnabled` | `false` | Opt-in per component instance |
| Low Stock Threshold | `lowStockThreshold` | `5` | Show badge when qty ≤ this value |
| Badge Text | `lowStockText` | *(empty)* | Custom text; leave empty for "Only N left!" |

## Magento requirements

- Magento 2.4+ with **Only X Left in Stock** enabled:
  `Stores > Configuration > Catalog > Inventory > Only X left Threshold`
- Products must have `only_x_left_in_stock` populated (Magento sets this
  automatically when stock drops below the configured threshold).

## GraphQL query extension

The implementation appends two fields to the CIF-generated product query:

```graphql
stock_status
only_x_left_in_stock
```

via `AbstractProductRetriever.extendProductQueryWith()`. The CIF framework chains
all registered consumers and executes a single optimised query.

## Building and deploying

```bash
# Run unit tests only
mvn test -pl core

# Full build + deploy to local AEM SDK (author on :4502)
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
mvn clean install -PautoInstallSinglePackage -Dclassic \
    -Daem.host=localhost -Daem.port=4502
```

## Testing the badge

1. In your Magento admin, go to **Catalog > Products** and edit a product.
2. Set **Quantity** to e.g. 3 and ensure **Manage Stock** is enabled.
3. In `Stores > Configuration > Catalog > Inventory`, set **Only X left Threshold** to `5`.
4. In AEM, add a **Product Teaser** component to a page and pick that product.
5. Open the component dialog > **Low Stock Badge** tab.
6. Check **Enable 'Low Stock' badge** and save.
7. Preview or publish the page — the badge should appear.

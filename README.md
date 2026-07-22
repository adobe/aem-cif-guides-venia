[![CircleCI](https://circleci.com/gh/adobe/aem-cif-guides-venia.svg?style=svg)](https://circleci.com/gh/aem-cif-guides-venia)
![GitHub](https://img.shields.io/github/license/adobe/aem-cif-guides-venia.svg)

# AEM Guides - CIF Venia Project

This project contains the AEM CIF Venia reference site. It demonstrates the usage of [CIF Core Components](https://github.com/adobe/aem-core-cif-components) for Adobe Experience Manager (AEM). It is intended as a best-practice set of examples as well as a potential starting point to develop your own functionality.

The project can be used in two variants:

* AEM as a Cloud Service deployments
* AEM on-prem or AEM hosted by Adobe Managed Services deployments

This project was generated using the [aem-project-archetype](https://github.com/adobe/aem-project-archetype).

## Variants

### AEM as a Cloud Service

The default variant of the project is built and deployed on AEM as a Cloud Service. It requires an AEM as a Cloud Service with entitlement for the Commerce Add-On. The Commerce Add-On provides the commerce authoring tooling like product & category pickers or product search for authors it also manages the backend connection to Magento (or alternative commerce system) via GraphQL. Once provisioned it is deployed on AEM as a Cloud Service environments automatically.

The deployment on AEM as a Cloud Service happens via [Cloud Manager](https://docs.adobe.com/content/help/en/experience-manager-cloud-service/implementing/deploying/overview.html) and this project can be transferred into the Cloud Manager git repository.

For local development an AEM as a Cloud Service SDK with the Commerce Add-On installed is required. Both can be downloaded via the Software Distribution channel. See [Set up local AEM Runtime](https://docs.adobe.com/content/help/en/experience-manager-learn/cloud-service/local-development-environment-set-up/aem-runtime.html) for instructions. For build and deployment steps see below.

### AEM 6.5

In this variant the project is built and deployed on AEM 6.5 hosted by Adobe Managed Services or self-hosted. The minimum requirements are AEM 6.5.7 with the Commerce Add-On installed. The AEM Commerce Add-On for AEM 6.5 is also available on the [Software Distribution portal](https://experience.adobe.com/#/downloads/content/software-distribution/en/aem.html).

The CIF Core Components and the Commerce Add-On connect to a Magento (or alternative) via GraphQL. This connection has to be configured in the `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl-default.config` config. A reference is included in the project template. Consult [documentation](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/home.html) for detailed configuation steps.

The project deployment can be done via Cloud Manager or AEM package install. For project build and deployment use the `classic` profile, see steps below.

## Branching Strategy
This project uses a `dev` branch for the development cycle between releases. On `dev` there can be dependencies to snapshot versions of the CIF Core components project. CircleCI will provide the snapshot dependencies including the `react-components` package on all branches except `main`.

After a release of the required dependencies, all dependencies have to be updated to release versions and the current state of the `dev` branch is merged to `main`. All releases of this project will be done from the `main` branch. This guarantees that the state on `main` can always be built and installed as-is.

## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as servlets or request filters.
* ui.apps: contains the /apps (and /etc) parts of the project, ie JS&CSS clientlibs, components, templates and runmode specific configs
* ui.content: contains sample content using the components from the ui.apps
* ui.tests: Java bundle containing JUnit tests that are executed server-side. This bundle is not to be deployed onto production.
* ui.launcher: contains glue code that deploys the ui.tests bundle (and dependent bundles) to the server and triggers the remote JUnit execution
* ui.frontend: an optional dedicated front-end build mechanism (Angular, React or general Webpack project)

## Best Practice

### Component Customization

The Venia reference project code demonstrates how CIF core component can be adopted, customized, and extended for a project. The project includes an extended product teaser component example called `MyProductTeaser`. It demonstrates the [CIF core components customization](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/storefront/developing/customize-cif-components.html) options by using an extended Sling `MyProductTeaser` model and a proxy component overlay.

### Individual product and category page URLs

This project includes sample configurations to demonstrate the usage of custom URLs for product and category pages. This allows each project to setup individual URL patterns for product and category pages according to their SEO needs. A combination of [UrlProvider](ui.apps/src/main/content/jcr_root/apps/venia/config.publish/com.adobe.cq.commerce.core.components.internal.services.UrlProviderImpl.config) config with [Sling Mappings](https://sling.apache.org/documentation/the-sling-engine/mappings-for-resource-resolution.html) is used.

This configuration must be adjusted with the external domain used by the project. The Sling Mappings are working based on the hostname and domain. Therefore this configuration is disabled by default and must be enabled before deployment. To do so rename the Sling Mapping `hostname.adobeaemcloud.com` folder in `ui.content/src/main/content/jcr_root/etc/map.publish/https` according to the used domain name and enable this config by adding `resource.resolver.map.location="/etc/map.publish"` to the [JcrResourceResolver](ui.apps/src/main/content/jcr_root/apps/venia/config.publish/org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl.xml) config.

For detailed configuration options see [Configuring and customizing product and category pages URLs](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/storefront/administering/advanced-url-configuration.html) in the AEM Content & Commerce and the [AEM Resource Mapping](https://docs.adobe.com/content/help/en/experience-manager-65/deploying/configuring/resource-mapping.html) documentation.

### Component Caching

[CIF Core Components](https://github.com/adobe/aem-core-cif-components) already have built-in support for [caching GraphQL responses for individual components](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/content-and-commerce/storefront/administering/caching.html). This feature can be used to reduce the number of GraphQL backend calls by a large factor. An effective caching can be achieved especially for repeating queries like retrieving the category tree for a navigation component or fetching all the available aggregations/facets values displayed on the product search and category pages.

## How to build

To build all the modules, run in the project root directory the following command with Maven 3:

    mvn clean install

This will build only the artefacts for an AEM as a Cloud Service target. To build the artefacts for AEM on-premise as target 
use the `-Pclassic` profile:

    mvn clean install -Pclassic

If you have a running AEM instance you can build and package the whole project and deploy into AEM with

    mvn clean install -PautoInstallSinglePackage

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallSinglePackagePublish

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

The `classic` profile can be combined with either of the examples mentioned above. 

## Client-side Components
The client-side CIF core components access the Magento GraphQL endpoint directly, so all calls have to either be served from the same endpoint as AEM or served via a proxy that adds CORS headers.

* For AEMaaCS installations this is pre-configured as part of the CIF onboarding.
* For AEM on-prem installations, please add a proxy to your dispatcher configuration. You can find an example of dispatcher configuration in the [CIF Core Components](https://github.com/adobe/aem-core-cif-components/tree/master/dispatcher) project.
* For local development, you can start a proxy using the following command. The GraphQL endpoint is then available at `http://localhost:3002/graphql`.
```
npx local-cors-proxy --proxyUrl https://my.magento.cloud --port 3002 --proxyPartial ''
```

## Testing

There are two levels of testing contained in the project:

### Unit tests

This show-cases classic unit testing of the code contained in the bundle. To
test, execute:

    mvn clean test

### Integration tests

This allows running integration tests that exercise the capabilities of AEM via
HTTP calls to its API. To run the integration tests, use one of the following two
commands depending on your [project variant](#variants).

For AEM as a Cloud project:

    mvn clean verify -Plocal,cloud

For AEM classic project:

    mvn clean verify -Plocal,classic

Test classes must be saved in the `src/main/java` directory (or any of its
subdirectories), and must be contained in files matching the pattern `*IT.java`.

The configuration provides sensible defaults for a typical local installation of
AEM. If you want to point the integration tests to different AEM author and
publish instances, you can use the following system properties via Maven's `-D`
flag.

| Property | Description | Default value |
| --- | --- | --- |
| `it.author.url` | URL of the author instance | `http://localhost:4502` |
| `it.author.user` | Admin user for the author instance | `admin` |
| `it.author.password` | Password of the admin user for the author instance | `admin` |
| `it.publish.url` | URL of the publish instance | `http://localhost:4503` |
| `it.publish.user` | Admin user for the publish instance | `admin` |
| `it.publish.password` | Password of the admin user for the publish instance | `admin` |

The integration tests in this archetype use the [AEM Testing
Clients](https://github.com/adobe/aem-testing-clients) and showcase some
recommended [best
practices](https://github.com/adobe/aem-testing-clients/wiki/Best-practices) to
be put in use when writing integration tests for AEM.

## ClientLibs

The frontend module is made available using an [AEM ClientLib](https://helpx.adobe.com/experience-manager/6-5/sites/developing/using/clientlibs.html). When executing the NPM build script, the app is built and the [`aem-clientlib-generator`](https://github.com/wcm-io-frontend/aem-clientlib-generator) package takes the resulting build output and transforms it into such a ClientLib.

A ClientLib will consist of the following files and directories:

- `css/`: CSS files which can be requested in the HTML
- `css.txt` (tells AEM the order and names of files in `css/` so they can be merged)
- `js/`: JavaScript files which can be requested in the HTML
- `js.txt` (tells AEM the order and names of files in `js/` so they can be merged
- `resources/`: Source maps, non-entrypoint code chunks (resulting from code splitting), static assets (e.g. icons), etc.

## Maven settings

The project comes with the auto-public repository configured. To set up the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html

## Releases

The Venia demo is only released on Github but not on Maven Central like other projects like the CIF components. To perform a release, we use a dedicated profile to make sure all modules versions are updated:

`mvn release:prepare release:clean -Pclassic`

Releases must be done on the `main` branch.

## Contributing

Contributions are welcomed! Read the [Contributing Guide](.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

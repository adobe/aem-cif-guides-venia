# AEM Guides - CIF Venia Project

This project contains the AEM CIF Venia reference site. It demonstrates the usage of [CIF Core Components](https://github.com/adobe/aem-core-cif-components) for Adobe Experience Manager (AEM). It is intended as a best-practice set of examples as well as a potential starting point to develop your own functionality.

The project can be used in two variants:

* AEM as a Cloud Service deployments
* AEM on-prem or AEM hosted by Adobe Managed Services deployments

This project was generated using the [aem-project-archetype](https://github.com/adobe/aem-project-archetype).

## Variants

### AEM as a Cloud Service

The default variant of the project is built and deployed on AEM as a Cloud Service. It requires an AEM as a Cloud Service with entitlement for the CIF Add-On. The CIF Add-On provides the commerce authoring tooling like product & category pickers or product search for authors it also manages the backend connection to Magento (or alternative commerce system) via GraphQL. Once provisioned it is deployed on AEM as a Cloud Service environments automatically.

The deployment on AEM as a Cloud Service happens via [Cloud Manager](https://docs.adobe.com/content/help/en/experience-manager-cloud-service/implementing/deploying/overview.html) and this project can be transferred into the Cloud Manager git repository.

For local development an AEM as a Cloud Service SDK with the CIF Add-On installed is required. Both can be downloaded via the Software Distribution channel. See [Set up local AEM Runtime](https://docs.adobe.com/content/help/en/experience-manager-learn/cloud-service/local-development-environment-set-up/aem-runtime.html) for instructions. For build and deployment steps see below.

### Classic AEM

In this variant the project is built and deploy on AEM 6.5 hosted by Adobe Managed Services or self-hosted. The minimum requirements are AEM 6.5 with the [CIF Connector](https://github.com/adobe/commerce-cif-connector) installed. The CIF Connector is not included in the generated project and must be installed separately. See [CIF Connector](https://github.com/adobe/commerce-cif-connector) project for instructions.

The CIF Core Components and the CIF Connector connect to a Magento (or alternative) via GraphQL. This connection has to be configured in the `com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl-default.config` config. A reference is included in the project template. Consult [documentation](https://github.com/adobe/aem-core-cif-components/wiki/configuration) for detailed configuation steps.

The project deployment can be done via Cloud Manager or AEM package install. For project build and deployment use the `classic` profile, see steps below.

## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as servlets or request filters.
* ui.apps: contains the /apps (and /etc) parts of the project, ie JS&CSS clientlibs, components, templates and runmode specific configs
* ui.content: contains sample content using the components from the ui.apps
* ui.tests: Java bundle containing JUnit tests that are executed server-side. This bundle is not to be deployed onto production.
* ui.launcher: contains glue code that deploys the ui.tests bundle (and dependent bundles) to the server and triggers the remote JUnit execution
* ui.frontend: an optional dedicated front-end build mechanism (Angular, React or general Webpack project)

## Customization

The Venia reference project code demonstrates how [CIF core components can be customized](https://github.com/adobe/aem-core-cif-components/wiki/Customizing-CIF-Core-Components) and extended is included in the `core` bundle module. The Sling modules package contains an example `MyProductTeaser` model.

## How to build

**Important**: The Venia project has two build profiles depending on the target platform where you deploy the project:
* `cloud` (default profile): this is the default profile and targets AEM as a Cloud Service (AEMaaCS). This is active by default if you don't specify a profile or if `-Pcloud` is defined.
* `classic`: this profile is for Abobe Managed Services (AMS) or on-premise deployments. This is defined with the `-Pclassic` profile.

_Note that while the 'cloud' profile is active by default, the default behavior of maven is to disable it when any other profile is specified on the command line. This means that, for example, one has to explicitly specify the 'cloud' profile when using one of the installation profiles shown below._

To build all the modules with the default `cloud` profile, run in the project root directory the following command with Maven 3:

    mvn clean install

If you have a running AEM instance you can build and package the whole project and deploy into AEM with

    mvn clean install -PautoInstallPackage,cloud

Or to deploy it to a publish instance, run

    mvn clean install -PautoInstallPackagePublish,cloud

Or alternatively

    mvn clean install -PautoInstallPackage,cloud -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle,cloud

If you are building the Venia demo for on-premise deployment, simply use `mvn clean install -Pclassic` for the top-level command that builds all the modules, and replace the `cloud` profile with `classic` in the other example commands like `mvn clean install -PautoInstallPackage,classic`.

## Testing

There are two levels of testing contained in the project:

### Unit tests

This show-cases classic unit testing of the code contained in the bundle. To
test, execute:

    mvn clean test

### Integration tests

This allows running integration tests that exercise the capabilities of AEM via
HTTP calls to its API. To run the integration tests, run:

    mvn clean verify -Plocal

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

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html

## Contributing

Contributions are welcomed! Read the [Contributing Guide](.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
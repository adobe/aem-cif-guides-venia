# Venia CIF Reference Storefront

AEM CIF reference storefront for AEM as a Cloud Service with React frontend.

## Build

JDK 11, Maven 3.3.9+. Node 12.14.1 / npm 6.14.6 auto-installed by frontend-maven-plugin.

- `mvn clean install` -- full build (all modules)
- `mvn clean install -pl core` -- build only the core OSGi bundle
- `mvn clean install -pl ui.frontend` -- build only the frontend (runs webpack prod)
- `mvn clean install -PautoInstallSinglePackage` -- build and deploy the `all` package to a local AEM instance
- `mvn clean install -pl core -PautoInstallBundle` -- build and deploy the core bundle only
- `mvn clean install -Pclassic` -- include classic (AEM 6.5) modules

## Testing

- **Java unit** (JUnit 5, Mockito, AEM Mocks): `mvn test -pl core` -- JaCoCo enforces 50% branch coverage
- **Frontend unit** (Jest, React Testing Library): `cd ui.frontend && npm test` -- includes lint + prettier check
- **Integration** (JUnit 4, AEM Cloud Testing Clients): `mvn verify -pl it.tests -Plocal` -- requires running AEM author + publish
- **UI E2E** (WebdriverIO): `mvn verify -pl ui.tests -Pui-tests-local-execution` -- requires running AEM

## Code Style

### Java
- Apache License 2.0 header required on all source files
- No enforced formatter -- follow existing code conventions

### Frontend (ui.frontend)
- ESLint: `npm run lint` / `npm run lint:fix` -- enforces Apache license header via `eslint-plugin-header`
- Prettier: `npm run prettier:check` / `npm run prettier:fix` -- 120 char width, 4-space indent, single quotes
- Both run automatically as part of `npm test`

## Module Map

| Module | Path | Description |
|--------|------|-------------|
| core | `core/` | OSGi bundle -- Sling Models, servlets, services |
| ui.frontend | `ui.frontend/` | React/Webpack frontend -- CIF storefront components, clientlibs |
| ui.apps | `ui.apps/` | AEM content package -- HTL components, clientlib nodes, component dialogs |
| ui.apps.structure | `ui.apps.structure/` | Repository structure package defining /apps roots |
| ui.config | `ui.config/` | OSGi configurations for AEM as a Cloud Service |
| ui.content | `ui.content/` | Mutable content -- page templates, policies, sample pages |
| all | `all/` | Container package embedding all modules + CIF Core Components |
| it.tests | `it.tests/` | Server-side integration tests (Cloud Manager compatible) |
| ui.tests | `ui.tests/` | Selenium/WebdriverIO UI end-to-end tests |
| dispatcher | `dispatcher/` | AEM Dispatcher configuration (Cloud Service) |
| classic/* | `classic/` | AEM 6.5 Classic variants (activated with `-Pclassic`) |

## Architecture

- **Backend**: Sling Models under `com.venia.core.models` extending CIF Core Component models. OSGi + BND for bundle metadata.
- **Frontend**: React 17 + Webpack 4. Extends `@adobe/aem-core-cif-react-components` with custom components/talons. Output packaged as AEM clientlibs via `aem-clientlib-generator`.
- **Commerce**: GraphQL to Adobe Commerce via CIF GraphQL client (`magento-graphql` bindings). Apollo Client on frontend.
- **AEM Components**: HTL templates in `ui.apps` delegate to Sling Models. Proxy pattern extending CIF Core and WCM Core components under `venia/components/`.
- **Deployment**: AEM as a Cloud Service (primary), AEM 6.5 (classic profile). `aemanalyser-maven-plugin` validates Cloud Service compatibility.

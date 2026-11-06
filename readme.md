# Dependency Graph Plugin

A Maven plugin that generates a D3-based dependency graph page for a Maven project and writes the dependency graph JSON used by the browser chart.

## What it does

This plugin:
- inspects the current Maven project dependencies
- builds a dependency graph model
- writes a JavaScript data file for D3 rendering
- generates an HTML page with a hierarchical edge-bundling visualization similar to the reference output used in the project notes

### Selected D3 rendering pattern

The plugin uses the hierarchical edge-bundling layout as the default visualization style because it scales best for large Java module ecosystems such as Spring and Hibernate.

This style is the best fit for projects with many modules and a moderate-to-large dependency surface because it keeps the graph readable while preserving the relationships between modules.

The generated output is placed under:

```text
target/dependency-graph/
```

with files like:
- `index.html`
- `dependency-graph-data.js`

## Plugin coordinates

```text
org.wildcraft.plugin:dependency-graph-plugin
```

## Build and run

```bash
mvn clean install
mvn dependency-graph:generate
```

Then open:

```text
target/dependency-graph/index.html
```

## Example output shape

```js
window.dependencyGraphData = [
  { "imports": ["org.apache.maven-maven-plugin-api"], "name": "org.wildcraft.plugin-dependency-graph-plugin" }
];
```

## Project status

The project is release-ready for GitHub and Maven Central publication, with metadata and release configuration added to the POM.

## References

- https://wildcraft.github.io/page3/
- https://www.baeldung.com/maven-plugin
- https://maven.apache.org/plugin-developers/index.html
- https://bl.ocks.org/mbostock/7607999
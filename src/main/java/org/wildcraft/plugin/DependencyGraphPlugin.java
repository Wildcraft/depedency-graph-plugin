package org.wildcraft.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mojo(name = "generate")
public class DependencyGraphPlugin extends AbstractMojo {
   @Parameter(defaultValue = "${project}", required = true, readonly = true)
   private MavenProject project;

   @Parameter(property = "dependency.graph.outputDirectory", defaultValue = "${project.build.directory}/dependency-graph")
   private File outputDirectory;

   @Override
   public void execute() throws MojoExecutionException {
       Map<String, Set<String>> dependencyGraph = buildDependencyGraph();
       writeGraphFiles(dependencyGraph);
   }

   private Map<String, Set<String>> buildDependencyGraph() {
       Map<String, Set<String>> graph = new LinkedHashMap<>();
       List projects = project.getCollectedProjects();
       if (projects == null || projects.isEmpty()) {
           projects = new ArrayList();
           projects.add(project);
       }

       for (Object rawProject : projects) {
           MavenProject mavenProject = (MavenProject) rawProject;
           String currentModule = moduleKey(mavenProject.getGroupId(), mavenProject.getArtifactId());
           graph.computeIfAbsent(currentModule, ignored -> new LinkedHashSet<>());

           List dependencies = mavenProject.getDependencies();
           if (dependencies != null) {
               for (Object rawDependency : dependencies) {
                   Dependency dependency = (Dependency) rawDependency;
                   String dependencyKey = moduleKey(dependency.getGroupId(), dependency.getArtifactId());
                   graph.computeIfAbsent(dependencyKey, ignored -> new LinkedHashSet<>());
                   graph.get(currentModule).add(dependencyKey);
               }
           }

           Set artifacts = mavenProject.getArtifacts();
           if (artifacts != null) {
               for (Object rawArtifact : artifacts) {
                   Artifact artifact = (Artifact) rawArtifact;
                   String artifactKey = moduleKey(artifact.getGroupId(), artifact.getArtifactId());
                   graph.computeIfAbsent(artifactKey, ignored -> new LinkedHashSet<>());
                   graph.get(currentModule).add(artifactKey);
               }
           }
       }

       return graph;
   }

   private String moduleKey(String groupId, String artifactId) {
       String normalizedGroup = groupId == null ? "project" : groupId.trim();
       String normalizedArtifact = artifactId == null ? "module" : artifactId.trim();
       String key = normalizedGroup + "-" + normalizedArtifact;
       return key.replace(':', '-').replaceAll("\\s+", "");
   }

   private void writeGraphFiles(Map<String, Set<String>> dependencyGraph) throws MojoExecutionException {
       if (outputDirectory == null) {
           outputDirectory = new File(project.getBuild().getDirectory(), "dependency-graph");
       }

       try {
           Files.createDirectories(outputDirectory.toPath());

           List<String> orderedNames = new ArrayList<>(dependencyGraph.keySet());
           orderedNames.sort(Comparator.naturalOrder());

           StringBuilder json = new StringBuilder();
           json.append("window.dependencyGraphData = [\n");
           for (int i = 0; i < orderedNames.size(); i++) {
               String name = orderedNames.get(i);
               List<String> imports = new ArrayList<>(dependencyGraph.getOrDefault(name, Collections.emptySet()));
               imports.sort(Comparator.naturalOrder());
               json.append("  {\"imports\":[");
               if (!imports.isEmpty()) {
                   for (int j = 0; j < imports.size(); j++) {
                       if (j > 0) {
                           json.append(",");
                       }
                       json.append("\"").append(imports.get(j)).append("\"");
                   }
               }
               json.append("],\"name\":\"").append(name).append("\"}");
               if (i < orderedNames.size() - 1) {
                   json.append(",");
               }
               json.append("\n");
           }
           json.append("];\n");

           Path dataFile = outputDirectory.toPath().resolve("dependency-graph-data.js");
           Files.writeString(dataFile, json.toString(), StandardCharsets.UTF_8);

           String html = buildHtmlPage();
           Path htmlFile = outputDirectory.toPath().resolve("index.html");
           Files.writeString(htmlFile, html, StandardCharsets.UTF_8);

           getLog().info("Dependency graph report written to: " + htmlFile.toAbsolutePath());
       } catch (IOException e) {
           throw new MojoExecutionException("Unable to write dependency graph report", e);
       }
   }

   private String buildHtmlPage() {
       return "<!DOCTYPE html>\n" +
               "<html lang=\"en\">\n" +
               "<head>\n" +
               "  <meta charset=\"UTF-8\">\n" +
               "  <title>Dependency Graph</title>\n" +
               "  <style>\n" +
               "    body { margin: 0; background: #f3f4f6; font-family: Helvetica, Arial, sans-serif; }\n" +
               "    #chart { width: 100%; overflow: auto; padding: 20px; box-sizing: border-box; }\n" +
               "    svg { display: block; margin: 0 auto; background: #f3f4f6; }\n" +
               "    .node { font: 300 11px \"Helvetica Neue\", Helvetica, Arial, sans-serif; fill: #bbb; }\n" +
               "    .node:hover { fill: #000; }\n" +
               "    .link { stroke: steelblue; stroke-opacity: .4; fill: none; pointer-events: none; }\n" +
               "    .node:hover, .node-transitive-target, .node-transitive-source, .node--source, .node--target { font-weight: 700; }\n" +
               "    .node--source { fill: #2ca02c; }\n" +
               "    .node-transitive-target { fill: #931919; }\n" +
               "    .node-transitive-source { fill: #1ee232; }\n" +
               "    .node--target { fill: #d62728; }\n" +
               "    .link-transitive-target, .link-transitive-source, .link--source, .link--target { stroke-opacity: 1; stroke-width: 2px; }\n" +
               "    .link--source { stroke: #d62728; }\n" +
               "    .link-transitive-target { stroke: #931919; }\n" +
               "    .link-transitive-source { stroke: #1ee232; }\n" +
               "    .link--target { stroke: #2ca02c; }\n" +
               "  </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "  <div id=\"chart\" align=\"left\"></div>\n" +
               "  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/d3/3.5.17/d3.min.js\"></script>\n" +
               "  <script src=\"./dependency-graph-data.js\"></script>\n" +
               "  <script>\n" +
               "    var myData = window.dependencyGraphData || [];\n" +
               "    var diameter = 850, radius = diameter / 2, innerRadius = radius - 100;\n" +
               "    var cluster = d3.layout.cluster().size([360, innerRadius]).sort(null).value(function(d) { return d.size; });\n" +
               "    var bundle = d3.layout.bundle();\n" +
               "    var line = d3.svg.line.radial().interpolate(\"bundle\").tension(.85).radius(function(d) { return d.y; }).angle(function(d) { return d.x / 180 * Math.PI; });\n" +
               "    var svg = d3.select(\"#chart\").append(\"svg\").attr(\"width\", diameter).attr(\"height\", diameter).append(\"g\").attr(\"transform\", \"translate(\" + radius + \",\" + radius + \")\");\n" +
               "    var link = svg.append(\"g\").selectAll(\".link\"), node = svg.append(\"g\").selectAll(\".node\");\n" +
               "    var nodes = cluster.nodes(packageHierarchy(myData)), links = packageImports(nodes);\n" +
               "    link = link.data(bundle(links)).enter().append(\"path\").each(function(d) { d.source = d[0]; d.target = d[d.length - 1]; var actualTarget = d.source.imports.filter(function(e) { return e.substring(e.lastIndexOf(\" \") + 1) == d.target.name; })[0]; var targetAttrib = actualTarget ? actualTarget.substring(0, actualTarget.lastIndexOf(\" \")).trim() : \"\"; d.targetAttrib = targetAttrib; }).attr(\"class\", \"link\").attr(\"d\", line);\n" +
               "    node = node.data(nodes.filter(function(n) { return !n.children; })).enter().append(\"text\").attr(\"class\", \"node\").attr(\"dy\", \".31em\").attr(\"transform\", function(d) { return \"rotate(\" + (d.x - 90) + \")translate(\" + (d.y + 8) + \",0)\" + (d.x < 180 ? \"\" : \"rotate(180)\"); }).style(\"text-anchor\", function(d) { return d.x < 180 ? \"start\" : \"end\"; }).text(function(d) { return d.key; }).on(\"mouseover\", mouseovered).on(\"mouseout\", mouseouted);\n" +
               "    function mouseovered(d) {\n" +
               "      node.each(function(n) { n.target = n.source = false; n.targetAttrib = \"\"; n.transExp = \"\"; });\n" +
               "      link.classed(\"link--target\", function(l) { if (l.target === d) { if (l.targetAttrib == 'transitive') { l.source.transExp = 'transitive'; l.transExp = 'transitive'; l.source.source = true; return false; } else { return l.source.source = true; } } }).classed(\"link--source\", function(l) { if (l.source === d) { if (l.targetAttrib == 'transitive') { l.target.targetAttrib = 'transitive'; l.target.target = true; return false; } else { return l.target.target = true; } } }).classed(\"link-transitive-target\", function(l) { if (l.source === d) { return l.targetAttrib == 'transitive'; } }).classed(\"link-transitive-source\", function(l) { if (l.target === d) { return l.transExp == 'transitive'; } }).filter(function(l) { return l.target === d || l.source === d; }).each(function() { this.parentNode.appendChild(this); });\n" +
               "      node.classed(\"node--target\", function(n) { return n.target && n.targetAttrib != 'transitive'; }).classed(\"node-transitive-target\", function(n) { return n.targetAttrib == 'transitive'; }).classed(\"node--source\", function(n) { return n.source && n.transExp != 'transitive'; }).classed(\"node-transitive-source\", function(n) { return n.transExp == 'transitive'; });\n" +
               "    }\n" +
               "    function mouseouted(d) {\n" +
               "      link.classed(\"link--target\", false).classed(\"link--source\", false).classed(\"link-transitive-target\", false).classed(\"link-transitive-source\", false);\n" +
               "      node.classed(\"node--target\", false).classed(\"node--source\", false).classed(\"node-transitive-target\", false).classed(\"node-transitive-source\", false);\n" +
               "    }\n" +
               "    d3.select(self.frameElement).style(\"height\", diameter + \"px\");\n" +
               "    function packageHierarchy(classes) {\n" +
               "      var map = {};\n" +
               "      function find(name, data) { var node = map[name], i; if (!node) { node = map[name] = data || { name: name, children: [] }; if (name.length) { node.parent = find(name.substring(0, i = name.lastIndexOf(\"-\"))); node.parent.children.push(node); node.key = name.substring(i + 1); } } return node; }\n" +
               "      classes.forEach(function(d) { find(d.name, d); });\n" +
               "      return map[''];\n" +
               "    }\n" +
               "    function packageImports(nodes) {\n" +
               "      var map = {}, imports = [];\n" +
               "      nodes.forEach(function(d) { map[d.name] = d; });\n" +
               "      nodes.forEach(function(d) { if (d.imports) { d.imports.forEach(function(i) { var reqImport = i.substring(i.lastIndexOf(\" \") + 1); imports.push({ source: map[d.name], target: map[reqImport] }); }); } });\n" +
               "      return imports;\n" +
               "    }\n" +
               "  </script>\n" +
               "</body>\n" +
               "</html>\n";
   }
}

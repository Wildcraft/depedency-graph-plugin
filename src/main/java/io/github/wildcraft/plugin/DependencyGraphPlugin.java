package io.github.wildcraft.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.function.Function;

// Aggregator: in a multi-module build the goal runs once, at the top, and reports every module together.
// Dependency collection (rather than resolution) reads the graph from POMs, so sibling modules need not be built first.
@Mojo(name = "generate", aggregator = true, requiresDependencyCollection = ResolutionScope.TEST)
public class DependencyGraphPlugin extends AbstractMojo {
   @Parameter(defaultValue = "${project}", required = true, readonly = true)
   private MavenProject project;

   @Parameter(defaultValue = "${session}", readonly = true)
   private MavenSession session;

   @Parameter(property = "dependency.graph.outputDirectory", defaultValue = "${project.build.directory}/dependency-graph")
   private File outputDirectory;

   @Parameter(property = "dependency.graph.depth", defaultValue = "3")
   private int depth;

   @Parameter(property = "dependency.graph.includeTransitive", defaultValue = "true")
   private boolean includeTransitive;

   @Component
   private RepositorySystem repositorySystem;

   @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
   private RepositorySystemSession repositorySession;

   private final Map<String, String> labels = new LinkedHashMap<>();
   private final Map<String, String> groups = new LinkedHashMap<>();
   private final Set<String> projectModules = new LinkedHashSet<>();
   // Shortest distance from a project module: 0 for the project, 1 for direct dependencies (L1), and so on
   private final Map<String, Integer> levels = new LinkedHashMap<>();

   @Override
   public void execute() throws MojoExecutionException {
       Map<String, Set<String>> dependencyGraph = buildDependencyGraph();
       int maxLevel = Math.max(1, levels.values().stream().max(Integer::compare).orElse(1));

       // The page can switch between L1..maxLevel; this only picks the level it opens at
       int initialLevel = Math.min(Math.max(1, depth), maxLevel);
       while (isSparseGraph(dependencyGraph, initialLevel) && initialLevel < maxLevel) {
           initialLevel++;
       }

       writeGraphFiles(dependencyGraph, initialLevel);
   }

   private boolean isSparseGraph(Map<String, Set<String>> graph, int level) {
       int totalNodes = 0;
       int totalEdges = 0;
       for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
           if (levelOf(entry.getKey()) > level) {
               continue;
           }
           totalNodes++;
           for (String imported : entry.getValue()) {
               if (levelOf(imported) <= level) {
                   totalEdges++;
               }
           }
       }
       return totalNodes <= 2 || totalEdges == 0;
   }

   private int levelOf(String key) {
       return levels.getOrDefault(key, Integer.MAX_VALUE);
   }

   private void recordLevel(String key, int level) {
       levels.merge(key, level, Math::min);
   }

   /** The modules of this build; a parent or aggregator POM is left out when the build has real modules. */
   private List<MavenProject> reactorModules() {
       List<MavenProject> all = session == null || session.getProjects() == null ? Collections.emptyList() : session.getProjects();
       List<MavenProject> modules = new ArrayList<>();
       for (MavenProject candidate : all) {
           if (!"pom".equals(candidate.getPackaging())) {
               modules.add(candidate);
           }
       }
       if (modules.isEmpty()) {
           modules.add(project);
       }
       return modules;
   }

   private Map<String, Set<String>> buildDependencyGraph() {
       Map<String, Set<String>> graph = new LinkedHashMap<>();
       List<MavenProject> projects = reactorModules();

       for (MavenProject mavenProject : projects) {
           String currentModule = moduleKey(mavenProject.getGroupId(), mavenProject.getArtifactId());
           graph.computeIfAbsent(currentModule, ignored -> new LinkedHashSet<>());
           projectModules.add(currentModule);
           recordLevel(currentModule, 0);

           List dependencies = mavenProject.getDependencies();
           if (dependencies != null) {
               for (Object rawDependency : dependencies) {
                   Dependency dependency = (Dependency) rawDependency;
                   String dependencyKey = moduleKey(dependency.getGroupId(), dependency.getArtifactId());
                   graph.computeIfAbsent(dependencyKey, ignored -> new LinkedHashSet<>());
                   graph.get(currentModule).add(dependencyKey);
                   recordLevel(dependencyKey, 1);
               }
           }

           if (includeTransitive) {
               Set artifacts = mavenProject.getArtifacts();
               if (artifacts != null) {
                   for (Object rawArtifact : artifacts) {
                       Artifact artifact = (Artifact) rawArtifact;
                       // The trail is "groupId:artifactId:type:version" strings from the project down to this artifact
                       List trail = artifact.getDependencyTrail();
                       if (trail == null || trail.size() < 2) {
                           continue;
                       }

                       String parentKey = currentModule;
                       for (int i = 1; i < trail.size(); i++) {
                           String[] coordinates = String.valueOf(trail.get(i)).split(":");
                           String childKey = moduleKey(coordinates[0], coordinates.length > 1 ? coordinates[1] : null);
                           graph.computeIfAbsent(childKey, ignored -> new LinkedHashSet<>());
                           recordLevel(childKey, i);
                           if (!parentKey.equals(childKey)) {
                               graph.get(parentKey).add(childKey);
                           }
                           parentKey = childKey;
                       }
                   }
                   addDeclaredDependencies(mavenProject, artifacts, graph);
               }
           }
       }

       return graph;
   }

   /**
    * The dependency trail only records the one path Maven used to resolve each artifact, so a library
    * used by several modules would appear to have a single user. Reading each artifact's own POM adds
    * every dependency it declares on another resolved module, which the coupling metrics rely on.
    */
   private void addDeclaredDependencies(MavenProject mavenProject, Set artifacts, Map<String, Set<String>> graph) {
       if (repositorySystem == null || repositorySession == null) {
           return;
       }
       for (Object rawArtifact : artifacts) {
           Artifact artifact = (Artifact) rawArtifact;
           String artifactKey = moduleKey(artifact.getGroupId(), artifact.getArtifactId());
           ArtifactDescriptorRequest request = new ArtifactDescriptorRequest(
                   new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                           artifact.getArtifactHandler().getExtension(), artifact.getVersion()),
                   mavenProject.getRemoteProjectRepositories(), null);
           try {
               for (org.eclipse.aether.graph.Dependency declared : repositorySystem.readArtifactDescriptor(repositorySession, request).getDependencies()) {
                   String scope = declared.getScope();
                   // Test and provided dependencies of a library are not part of the consumer's graph
                   if (declared.isOptional() || "test".equals(scope) || "provided".equals(scope) || "system".equals(scope)) {
                       continue;
                   }
                   String targetKey = moduleKey(declared.getArtifact().getGroupId(), declared.getArtifact().getArtifactId());
                   if (graph.containsKey(targetKey) && !targetKey.equals(artifactKey)) {
                       graph.get(artifactKey).add(targetKey);
                   }
               }
           } catch (ArtifactDescriptorException e) {
               getLog().debug("Unable to read dependencies of " + artifact + ": " + e.getMessage());
           }
       }
   }

   private String moduleKey(String groupId, String artifactId) {
       String normalizedGroup = groupId == null ? "project" : groupId.trim();
       String normalizedArtifact = artifactId == null ? "module" : artifactId.trim();
       String key = normalizedGroup + "-" + normalizedArtifact;
       key = key.replace(':', '-').replaceAll("\\s+", "");
       labels.put(key, normalizedArtifact);
       groups.put(key, normalizedGroup);
       return key;
   }

   private void writeGraphFiles(Map<String, Set<String>> dependencyGraph, int initialLevel) throws MojoExecutionException {
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
                       json.append(DependencyMetrics.quote(imports.get(j)));
                   }
               }
               json.append("],\"group\":").append(DependencyMetrics.quote(groups.getOrDefault(name, "other")))
                       .append(",\"label\":").append(DependencyMetrics.quote(labels.getOrDefault(name, name)))
                       .append(",\"level\":").append(levelOf(name))
                       .append(",\"name\":").append(DependencyMetrics.quote(name));
               if (projectModules.contains(name)) {
                   json.append(",\"project\":true");
               }
               json.append("}");
               if (i < orderedNames.size() - 1) {
                   json.append(",");
               }
               json.append("\n");
           }
           json.append("];\n");
           json.append("window.dependencyGraphDepth = ").append(initialLevel).append(";\n");

           Function<String, String> orEmpty = value -> DependencyMetrics.quote(value == null ? "" : value);
           String projectName = project.getName() == null || project.getName().isBlank() ? project.getArtifactId() : project.getName();
           json.append("window.dependencyGraphProject = {\"name\":").append(orEmpty.apply(projectName))
                   .append(",\"groupId\":").append(orEmpty.apply(project.getGroupId()))
                   .append(",\"artifactId\":").append(orEmpty.apply(project.getArtifactId()))
                   .append(",\"version\":").append(orEmpty.apply(project.getVersion()))
                   .append(",\"generated\":").append(orEmpty.apply(java.time.LocalDateTime.now().withNano(0).toString().replace('T', ' ')))
                   .append("};\n");

           String metrics = new DependencyMetrics(dependencyGraph, groups, levels, projectModules).toJson();
           json.append("window.dependencyGraphMetrics = ").append(metrics.trim()).append(";\n");

           Path dataFile = outputDirectory.toPath().resolve("dependency-graph-data.js");
           Files.writeString(dataFile, json.toString(), StandardCharsets.UTF_8);
           Files.writeString(outputDirectory.toPath().resolve("dependency-graph-metrics.json"), metrics, StandardCharsets.UTF_8);

           Path htmlFile = outputDirectory.toPath().resolve("index.html");
           try (InputStream page = getClass().getResourceAsStream("/dependency-graph/index.html")) {
               if (page == null) {
                   throw new MojoExecutionException("Report page template is missing from the plugin");
               }
               Files.write(htmlFile, page.readAllBytes());
           }

           getLog().info("Dependency graph report written to: " + htmlFile.toAbsolutePath());
           getLog().info("Coupling and cohesion metrics written to: " + outputDirectory.toPath().resolve("dependency-graph-metrics.json").toAbsolutePath());
       } catch (IOException e) {
           throw new MojoExecutionException("Unable to write dependency graph report", e);
       }
   }
}

package io.github.wildcraft.plugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Coupling and cohesion metrics for a module dependency graph.
 *
 * <p>Module coupling follows Robert C. Martin's package metrics: afferent coupling (Ca, incoming),
 * efferent coupling (Ce, outgoing) and instability I = Ce / (Ca + Ce). Propagation cost
 * (MacCormack et al.) is the average share of the graph each module reaches transitively.
 * Cohesion is measured per groupId as relational cohesion H = (R + 1) / N, where R is the number
 * of dependencies between modules of the same group and N the number of modules in it.
 */
final class DependencyMetrics {
   private final Map<String, Set<String>> graph;
   private final Map<String, Set<String>> reverse = new LinkedHashMap<>();
   private final Map<String, String> groups;
   private final Map<String, Integer> levels;
   private final Set<String> projectModules;

   DependencyMetrics(Map<String, Set<String>> graph, Map<String, String> groups, Map<String, Integer> levels, Set<String> projectModules) {
       this.graph = graph;
       this.groups = groups;
       this.levels = levels;
       this.projectModules = projectModules;
       for (String name : graph.keySet()) {
           reverse.computeIfAbsent(name, ignored -> new LinkedHashSet<>());
           for (String target : graph.get(name)) {
               reverse.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(name);
           }
       }
   }

   String toJson() {
       List<String> names = new ArrayList<>(graph.keySet());
       Collections.sort(names);
       int moduleCount = names.size();
       int edgeCount = 0;
       long reachableTotal = 0;
       int maxLevel = 0;

       StringBuilder modules = new StringBuilder();
       for (String name : names) {
           int ca = reverse.get(name).size();
           int ce = graph.get(name).size();
           int transitiveCa = reachable(name, reverse).size();
           int transitiveCe = reachable(name, graph).size();
           edgeCount += ce;
           reachableTotal += transitiveCe;
           maxLevel = Math.max(maxLevel, levels.getOrDefault(name, 0));
           if (modules.length() > 0) {
               modules.append(",\n");
           }
           modules.append("    {\"name\":").append(quote(name))
                   .append(",\"group\":").append(quote(groups.getOrDefault(name, "other")))
                   .append(",\"level\":").append(levels.getOrDefault(name, 0))
                   .append(",\"project\":").append(projectModules.contains(name))
                   .append(",\"ca\":").append(ca)
                   .append(",\"ce\":").append(ce)
                   .append(",\"instability\":").append(ratio(ce, ca + ce))
                   .append(",\"transitiveCa\":").append(transitiveCa)
                   .append(",\"transitiveCe\":").append(transitiveCe)
                   .append("}");
       }

       // Distinct external dependencies declared by any of the project's modules; links between modules don't count
       Set<String> direct = new HashSet<>();
       for (String projectModule : projectModules) {
           for (String target : graph.getOrDefault(projectModule, Collections.emptySet())) {
               if (!projectModules.contains(target)) {
                   direct.add(target);
               }
           }
       }
       int directDependencies = direct.size();
       int dependencyCount = moduleCount - projectModules.size();

       StringBuilder json = new StringBuilder();
       json.append("{\n  \"summary\":{")
               .append("\"modules\":").append(dependencyCount)
               .append(",\"directDependencies\":").append(directDependencies)
               .append(",\"transitiveDependencies\":").append(dependencyCount - directDependencies)
               .append(",\"dependencyEdges\":").append(edgeCount)
               .append(",\"maxLevel\":").append(maxLevel)
               .append(",\"averageCe\":").append(ratio(edgeCount, moduleCount))
               .append(",\"density\":").append(ratio(edgeCount, (long) moduleCount * (moduleCount - 1)))
               .append(",\"propagationCost\":").append(ratio(reachableTotal, (long) moduleCount * moduleCount))
               .append(",\"groups\":").append(groupNames().size())
               .append(",\"projectModules\":").append(projectModules.size())
               .append("},\n  \"modules\":[\n").append(modules).append("\n  ],\n")
               .append("  \"groups\":[\n").append(groupsJson()).append("\n  ]\n}\n");
       return json.toString();
   }

   private String groupsJson() {
       StringBuilder json = new StringBuilder();
       for (String group : groupNames()) {
           int members = 0;
           int internal = 0;
           int outgoing = 0;
           int incoming = 0;
           for (String name : graph.keySet()) {
               boolean inGroup = group.equals(groups.get(name));
               if (inGroup) {
                   members++;
               }
               for (String target : graph.get(name)) {
                   boolean targetInGroup = group.equals(groups.get(target));
                   if (inGroup && targetInGroup) {
                       internal++;
                   } else if (inGroup) {
                       outgoing++;
                   } else if (targetInGroup) {
                       incoming++;
                   }
               }
           }
           if (json.length() > 0) {
               json.append(",\n");
           }
           json.append("    {\"name\":").append(quote(group))
                   .append(",\"modules\":").append(members)
                   .append(",\"internal\":").append(internal)
                   .append(",\"ca\":").append(incoming)
                   .append(",\"ce\":").append(outgoing)
                   .append(",\"instability\":").append(ratio(outgoing, incoming + outgoing))
                   .append(",\"relationalCohesion\":").append(ratio(internal + 1, members))
                   .append(",\"cohesionRatio\":").append(ratio(internal, internal + outgoing))
                   .append("}");
       }
       return json.toString();
   }

   private List<String> groupNames() {
       Set<String> names = new HashSet<>();
       for (String name : graph.keySet()) {
           names.add(groups.getOrDefault(name, "other"));
       }
       List<String> sorted = new ArrayList<>(names);
       sorted.sort(Comparator.naturalOrder());
       return sorted;
   }

   private static Set<String> reachable(String start, Map<String, Set<String>> adjacency) {
       Set<String> seen = new HashSet<>();
       Deque<String> queue = new ArrayDeque<>(adjacency.getOrDefault(start, Collections.emptySet()));
       while (!queue.isEmpty()) {
           String next = queue.poll();
           if (!next.equals(start) && seen.add(next)) {
               queue.addAll(adjacency.getOrDefault(next, Collections.emptySet()));
           }
       }
       return seen;
   }

   private static String ratio(long numerator, long denominator) {
       return denominator == 0 ? "0" : String.format(Locale.ROOT, "%.4f", (double) numerator / denominator);
   }

   static String quote(String value) {
       StringBuilder quoted = new StringBuilder("\"");
       for (char c : value.toCharArray()) {
           if (c == '"' || c == '\\') {
               quoted.append('\\').append(c);
           } else if (c < 0x20 || c == '<') {
               quoted.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
           } else {
               quoted.append(c);
           }
       }
       return quoted.append('"').toString();
   }
}

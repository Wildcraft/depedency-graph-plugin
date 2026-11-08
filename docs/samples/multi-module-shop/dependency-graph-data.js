window.dependencyGraphData = [
  {"imports":["demo.shop-domain","org.slf4j-slf4j-api"],"group":"demo.shop","label":"application","level":0,"name":"demo.shop-application","project":true},
  {"imports":["org.apache.commons-commons-lang3"],"group":"demo.shop","label":"domain","level":0,"name":"demo.shop-domain","project":true},
  {"imports":["demo.shop-application","org.springframework-spring-web"],"group":"demo.shop","label":"web","level":0,"name":"demo.shop-web","project":true},
  {"imports":[],"group":"io.micrometer","label":"micrometer-commons","level":3,"name":"io.micrometer-micrometer-commons"},
  {"imports":["io.micrometer-micrometer-commons"],"group":"io.micrometer","label":"micrometer-observation","level":2,"name":"io.micrometer-micrometer-observation"},
  {"imports":[],"group":"org.apache.commons","label":"commons-lang3","level":1,"name":"org.apache.commons-commons-lang3"},
  {"imports":[],"group":"org.slf4j","label":"slf4j-api","level":1,"name":"org.slf4j-slf4j-api"},
  {"imports":["org.springframework-spring-core"],"group":"org.springframework","label":"spring-beans","level":2,"name":"org.springframework-spring-beans"},
  {"imports":["org.springframework-spring-jcl"],"group":"org.springframework","label":"spring-core","level":2,"name":"org.springframework-spring-core"},
  {"imports":[],"group":"org.springframework","label":"spring-jcl","level":3,"name":"org.springframework-spring-jcl"},
  {"imports":["io.micrometer-micrometer-observation","org.springframework-spring-beans","org.springframework-spring-core"],"group":"org.springframework","label":"spring-web","level":1,"name":"org.springframework-spring-web"}
];
window.dependencyGraphDepth = 3;
window.dependencyGraphProject = {"name":"Shop","groupId":"demo.shop","artifactId":"shop-parent","version":"1.0","generated":"2026-11-07 22:58:03"};
window.dependencyGraphMetrics = {
  "summary":{"modules":8,"directDependencies":3,"transitiveDependencies":5,"dependencyEdges":11,"maxLevel":3,"averageCe":1.0000,"density":0.1000,"propagationCost":0.1901,"groups":5,"projectModules":3},
  "modules":[
    {"name":"demo.shop-application","group":"demo.shop","level":0,"project":true,"ca":1,"ce":2,"instability":0.6667,"transitiveCa":1,"transitiveCe":3},
    {"name":"demo.shop-domain","group":"demo.shop","level":0,"project":true,"ca":1,"ce":1,"instability":0.5000,"transitiveCa":2,"transitiveCe":1},
    {"name":"demo.shop-web","group":"demo.shop","level":0,"project":true,"ca":0,"ce":2,"instability":1.0000,"transitiveCa":0,"transitiveCe":10},
    {"name":"io.micrometer-micrometer-commons","group":"io.micrometer","level":3,"project":false,"ca":1,"ce":0,"instability":0.0000,"transitiveCa":3,"transitiveCe":0},
    {"name":"io.micrometer-micrometer-observation","group":"io.micrometer","level":2,"project":false,"ca":1,"ce":1,"instability":0.5000,"transitiveCa":2,"transitiveCe":1},
    {"name":"org.apache.commons-commons-lang3","group":"org.apache.commons","level":1,"project":false,"ca":1,"ce":0,"instability":0.0000,"transitiveCa":3,"transitiveCe":0},
    {"name":"org.slf4j-slf4j-api","group":"org.slf4j","level":1,"project":false,"ca":1,"ce":0,"instability":0.0000,"transitiveCa":2,"transitiveCe":0},
    {"name":"org.springframework-spring-beans","group":"org.springframework","level":2,"project":false,"ca":1,"ce":1,"instability":0.5000,"transitiveCa":2,"transitiveCe":2},
    {"name":"org.springframework-spring-core","group":"org.springframework","level":2,"project":false,"ca":2,"ce":1,"instability":0.3333,"transitiveCa":3,"transitiveCe":1},
    {"name":"org.springframework-spring-jcl","group":"org.springframework","level":3,"project":false,"ca":1,"ce":0,"instability":0.0000,"transitiveCa":4,"transitiveCe":0},
    {"name":"org.springframework-spring-web","group":"org.springframework","level":1,"project":false,"ca":1,"ce":3,"instability":0.7500,"transitiveCa":1,"transitiveCe":5}
  ],
  "groups":[
    {"name":"demo.shop","modules":3,"internal":2,"ca":0,"ce":3,"instability":1.0000,"relationalCohesion":1.0000,"cohesionRatio":0.4000},
    {"name":"io.micrometer","modules":2,"internal":1,"ca":1,"ce":0,"instability":0.0000,"relationalCohesion":1.0000,"cohesionRatio":1.0000},
    {"name":"org.apache.commons","modules":1,"internal":0,"ca":1,"ce":0,"instability":0.0000,"relationalCohesion":1.0000,"cohesionRatio":0},
    {"name":"org.slf4j","modules":1,"internal":0,"ca":1,"ce":0,"instability":0.0000,"relationalCohesion":1.0000,"cohesionRatio":0},
    {"name":"org.springframework","modules":4,"internal":4,"ca":1,"ce":1,"instability":0.5000,"relationalCohesion":1.2500,"cohesionRatio":0.8000}
  ]
};

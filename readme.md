
# TODO
| S.No | Task                                                        | Status |
|------|-------------------------------------------------------------|--------|
| 1    | Expand the MOJO to write dependency tree in required format | [x]    |
| 2    | Learn to write test cases & documentation                   | [ ]    |
| 3    | Learn about Maven Plugin and Maven Report Plugin            | [ ]    |


# Intermediate output format
`
var myData = [
    {"imports":[],"name":"java-java.base"},
    {"imports":["java-java.base"],"name":"java-java.compiler"},
    {"imports":["java-java.base"],"name":"java-java.datatransfer"},
    {"imports":["java-java.base"],"name":"java-java.instrument"},
    {"imports":["java-java.base"],"name":"java-java.logging"},
    {"imports":["java-java.base"],"name":"java-java.management"},
    {"imports":["java-java.base"],"name":"java-java.scripting"},
    {"imports":["java-java.base"],"name":"jdk-jdk.httpserver"},
    {"imports":["java-java.base"],"name":"jdk-jdk.incubator.httpclient"},
    {"imports":["java-java.base"],"name":"jdk-jdk.internal.ed"},
    {"imports":["java-java.base"],"name":"jdk-jdk.internal.jvmstat"},
    {"imports":["java-java.base"],"name":"jdk-jdk.internal.le"},
    {"imports":["java-java.base"],"name":"jdk-jdk.internal.opt"},
    {"imports":["java-java.base"],"name":"jdk-jdk.internal.vm.ci"},
    {"imports":["java-java.base"],"name":"jdk-jdk.jartool"},
    {"imports":["java-java.base"],"name":"java-java.xml"},
    {"imports":["java-java.base"],"name":"jdk-jdk.crypto.ec"},
    {"imports":["java-java.base"],"name":"java-java.smartcardio"},
    {"imports":["java-java.base"],"name":"jdk-jdk.charsets"},
	{"imports":["java-java.base"],"name":"jdk-jdk.jdwp.agent"},
	{"imports":["java-java.base"],"name":"jdk-jdk.localedata"},
	{"imports":["java-java.base"],"name":"jdk-jdk.net"},
	{"imports":["java-java.base"],"name":"jdk-jdk.pack"},
	{"imports":["java-java.base"],"name":"jdk-jdk.unsupported"},
	{"imports":["java-java.base"],"name":"jdk-jdk.zipfs"},
	{"imports":["java-java.base"],"name":"jdk-jdk.sctp"},
	{"imports":["java-java.base"],"name":"java-java.xml.ws.annotation"},
	{"imports":["java-java.base","transitive java-java.datatransfer","java-java.logging"],"name":"java-java.activation"},
	{"imports":["java-java.base","transitive java-java.desktop","java-java.logging","java-java.naming","transitive java-java.rmi","java-java.transaction","jdk-jdk.unsupported"],"name":"java-java.corba"},
	{"imports":["java-java.base","transitive java-java.datatransfer","java-java.prefs","transitive java-java.xml"],"name":"java-java.desktop"},
	{"imports":["java-java.base","transitive java-java.management","java-java.naming","transitive java-java.rmi"],"name":"java-java.management.rmi"},
	{"imports":["java-java.base","java-java.security.sasl"],"name":"java-java.naming"},
	{"imports":["java-java.base","java-java.xml"],"name":"java-java.prefs"},
	{"imports":["java-java.base","java-java.logging"],"name":"java-java.rmi"},
	{"imports":["java-java.base","transitive java-java.compiler","transitive java-java.datatransfer","transitive java-java.desktop","transitive java-java.instrument","transitive java-java.logging","transitive java-java.management","transitive java-java.management.rmi","transitive java-java.naming","transitive java-java.prefs","transitive java-java.rmi","transitive java-java.scripting","transitive java-java.security.jgss","transitive java-java.security.sasl","transitive java-java.sql","transitive java-java.sql.rowset","transitive java-java.xml","transitive java-java.xml.crypto"],"name":"java-java.se"},
	{"imports":["transitive java-java.activation","java-java.base","transitive java-java.corba","transitive java-java.se","transitive java-java.transaction","transitive java-java.xml.bind","transitive java-java.xml.ws","transitive java-java.xml.ws.annotation"],"name":"java-java.se.ee"},
	{"imports":["java-java.base","java-java.naming"],"name":"java-java.security.jgss"},
	{"imports":["java-java.base","java-java.logging"],"name":"java-java.security.sasl"},
]`

# Good Hints for projection of work
1. Maven clean plugin is simplest plugin to explore
2. 

## References
Following references were used while developing this maven plugin
1. https://www.baeldung.com/maven-plugin
2. https://maven.apache.org/plugin-developers/index.html
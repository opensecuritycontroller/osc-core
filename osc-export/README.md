# OSC Export

The **osc-export** project creates an OSGi bundle index from the transitive dependencies specified in its POM file. The index is defined in `server.bnd` and used by `server.bndrun` to resolve the required bundles and produce an executable JAR. 

### Running
The `server.bndrun` file can be used to run the server from Eclipse (Run OSGi), using the [bndtools plugin](http://bndtools.org/installation.html) and is then exported by the `bnd-export-maven-plugin`. This project uses antrun to copy the resources into the [**osc-server-bom**](../osc-server-bom) project.

### Debugging

Locally debugging the server is easier using `server-debug.bndrun`.
* This adds [Felix Web Console](http://felix.apache.org/documentation/subprojects/apache-felix-web-console.html) and the [GoGo shell](http://felix.apache.org/documentation/subprojects/apache-felix-gogo.html).
* Both `server.bndrun` and `server-debug.bndrun` can be used with the Eclipse Java debugger.

### Dependencies
This project will list all dependencies needed to create an executable JAR of OSC.
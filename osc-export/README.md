# Project org.osc.export

This project creates an OSGi bundle index from the transitive dependencies specified in its pom.xml.

The index is used by export.bndrun to resolve the required bundles and produce an executable jar.


A local distribution is created in this project, by copying plugins and webapp from other projects, which is also copied to ../serverBOM.

This allows export.bndrun to be run from Eclipse (Run OSGi), using bndtools plugin.


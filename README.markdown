# The Digital Parrot with Histroy #

The Digital parrot with History is research software. 
It bases on the [Digital Parrot](https://github.com/schweerelos/The-Digital-Parrot), 
which is an implementation of the retrieval aspects of an augmented memory system 
based on Cognitive Psychology. For more information, see [http://hdl.handle.net/10289/5263](http://hdl.handle.net/10289/5263).

In addition to the Digital Parrot, the Digital parrot with History has implemented a history system 
which records modifications in the memory data and enables the users to access former memory data.

## Installation ##

### Compilation ###

The repository includes settings for Eclipse.

#### Webrenderer ####

The Digital Parrot uses Webrenderer for the map view. You'll need to
download the appropriate version of [Webrenderer (Swing edition)](http://www.webrenderer.com/products/swing/product/) for
your operating system.

You should receive an archive that contains, among other files, three
jar files. You need to install these into your local maven
repository. An example for Linux and Webrenderer version 5.0.9:

    mvn install:install-file -DgroupId=com.webrenderer \
    -DartifactId=corecomponents-swing-linux -Dversion=5.0.9 -Dpackaging=jar \
    -Dfile=corecomponents-swing-linux.jar

    mvn install:install-file -DgroupId=com.webrenderer \
    -DartifactId=webrenderer-swing-linux -Dversion=5.0.9 -Dpackaging=jar \
    -Dfile=webrenderer-swing-linux.jar

    mvn install:install-file -DgroupId=com.webrenderer \
    -DartifactId=webrenderer-swing -Dversion=5.0.9 -Dpackaging=jar \
    -Dfile=webrenderer-swing.jar

If you're using a different operating system or a different version of
Webrenderer, you'll need to adjust the dependencies in pom.xml
appropriately.

Create a directory called *.digital-parrot* in your home directory. In
this directory, create a file called parrot.properties with the
following structure:

    [map view license info]
    webrenderer.license.type = <license type, eg 30dtrial>
    webrenderer.license.data = <license key>

Fill in your Webrenderer license type and key; if you downloaded the
30-day trial version, you will find this in the file *TestBrowser.java*
that came with the download.

#### Memory Data Ontologies ###

The conceptual data which describes the memories semantic is located in *src/main/resources/owl/*. 
Copy the directory and its content into your *.digital-parrot* directory

Example memory data are included in *src/main/resources/data/*.

## Running ##

To run the Digital Parrot with History on a given data plus history file:

    java -jar target/digital-parrot-with-history-1.0-SNAPSHOT.jar <your-path-to>/data/scenario/facts4.rdf <your-path-to>/data/history-fr1-fr4.rdf
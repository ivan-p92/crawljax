Crawljax Maven Archtypes
========================

Crawljax Plugin
---------------

To generate a template for Crawljax plugin development using Maven:

     mvn archetype:generate 
       -DgroupId=sample 
       -DartifactId=sample-plugin 
       -DarchetypeGroupId=com.crawljax.plugins.archetypes  
       -DarchetypeArtifactId=crawljax-plugins-archetype 
       -DarchetypeVersion=1.0
       
You can change ``sample`` with your own plugin's name.

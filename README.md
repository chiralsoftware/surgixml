# surgixml
In-place scriptable XML editor which carefully preserves formatting, comments and whitespace

# Why

There are many XML editor tools out there. The purpose of this tool is to edit XML files in a way
that will result in clean diffs by carefully preserving whitespace and comments. This means
that we can modify Apache Tomcat's default `server.xml` file, and do a diff and see clearly 
what has changed.

# Native

This package compiles to a native executable easily using GraalVM native-image tool. This makes it
well suited for use in bash scripts.

# Example

See the file `fix-server-xml.sh` for an example of common edits to a Tomcat `server.xml` file.

# Releases

The release will always be called current, so scripts can download the current version easily.
A Linux native image will always be provided.

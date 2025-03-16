<h1 align="center"><p><img src="https://avatars.githubusercontent.com/u/200624913?s=200&u=8e9c07296332eaa1bb1d62573842fcbf3f21cf07&v=4"></p><p>XmlPullKmp</p></h1>

[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](http://kotlinlang.org)


# XmlPullKmp
Kotlin Multiplatform implementation of XmlPullParser. This library is designed to be a drop in replacement of the XmlPullParser interface.
> :warning:This library is still in development and may not be fully functional. Currently it relies on transitive dependency to [fleeksoft's IO library](https://github.com/fleeksoft/fleeksoft-io).

___
### Installation

This library is stored on [Maven Central repository](https://central.sonatype.com/artifact/io.github.xmlpullkmp/xmlpullkmp). To add library to Your project paste the following snippet in your TOML file.
```
[versions]
xmlpullkmp = "<latest_version>"

[libraries]
xmlpullkmp = { module = "io.github.xmlpullkmp:xmlpullkmp", version.ref = "xmlpullkmp" }  
```

## Open source
This library is a port of [this](https://github.com/sonatype/plexus-utils) implementation of XmlPullParser to Kotlin Multiplatform.

## License
This library is licensed under the Apache License, Version 2.0.

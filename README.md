<!-- PROJECT LOGO -->
<br />
<p align="center">
  <a href="https://github.com/tomon-world/tomon-sdk-kotlin">
    <img src="images/LOGO%20with%20TOMON.png" alt="Logo" width="1200" height="300">
  </a>

</p>


# Tomon-sdk-kotlin

## About

Tomon SDK for Kotlin/Java is a maven module which provide a more convenient way for the developer to interact with the [Tomon API](https://developer.tomon.co/docs/).

- Javascript like EventEmitter for listening the specific event occurs
- Use official Tomon API

## Installation

1.declare github repository
    
**gradle**

```xml
repositories {
    maven {
            url "https://maven.pkg.github.com/tomon-world/tomon-sdk-kotlin"
            credentials {
                username = {{Your github username}}
                password = {{Your github token (read:packages, repo)}}
            }
    }
}
```

2.Install using maven or gradle

**Gradle**
```xml
implementation 'co.tomon:tomon-sdk-kotlin:1.0.2'
```

**Maven**
```xml
<dependency>
  <groupId>co.tomon</groupId>
  <artifactId>tomon-sdk-kotlin</artifactId>
  <version>1.0.2</version>
</dependency>
```


please see [example](https://github.com/tomon-world/tomon-sdk-kotlin/tree/master/example)

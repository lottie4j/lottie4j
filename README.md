# Lottie4J :: Java library to handle Lottie files

The aim is to create Java libraries to handle and play Lottie files with Java.

Further technical information about this project can be found on [lottie4j.com](https://lottie4j.com).

## Structure of the sources

### Implementation Libraries

* `core`: Lottie data model + reading and writing of Lottie files.
* `fxplayer`: JavaFX component to play Lottie animations, using `core`.

### Demo applications

* `fxdemo`: Demo application demonstrating the minimal use of `fxplayer`.
* `fxfileviewer`: JavaFX application to visualize the structure of a Lottie file, using `fxplayer`.

## More Info About Lottie Files

https://lottiefiles.github.io/lottie-docs/
Meet LottieDocs, the ultimate guide to the Lottie format!
This document contains a human-readable description of the Lottie format, complete with interactive examples, a JSON
editor to help you debug issues & more.
Watch the video tutorial at https://youtu.be/uHxi9nEfUR0
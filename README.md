# Lottie4J :: Java library to handle Lottie files

Aim is to create a JavaFX player capable of playing Lottie files.

https://lottiefiles.com/what-is-lottie

There is a Lottie Android project but this is aiming for native Android integration, not recent Java versions and/or
JavaFX.

## Lottie file format

dotLottie package: https://dotlottie.io/intro/#prelude

JSON Lottie animation file: https://lottiefiles.github.io/lottie-docs/schema/lottie.schema.json

### Creating Lottie animations

Adobe AfterEffects is used a lot to create Lottie animations. But there is a free
alternative: [Haiku](https://www.haikuanimator.com/) that was turned
into [an opensource project in August 2021](https://www.haikuanimator.com/blog/open-source).

## IDE settings

To run the JavaFX Demo application, the JavaFX runtime is needed. You can install this separately by downloading it from
the [Gluon website](https://gluonhq.com/products/javafx/), or use a JDK which has JavaFX included, for instance Azul
Zulu.

```
$ sdk install java 17.0.3.fx-zulu
```

In your IDE select this SDK as the runtime for your project.
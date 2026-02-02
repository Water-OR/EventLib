# EventLib v2

**EventLib** is a high-performance, thread-safe event dispatching library for **Java 8+**.
It is designed for modular systems that require precise control over execution order.
It is standardized using **JSpecify**, **JetBrains Annotations**, and **Google Error Prone**

[![](https://jitpack.io/#Water-OR/EventLib.svg)](https://jitpack.io/#Water-OR/EventLib)

## ✨ Features
- **🛡️ Thread-Safety:** Built with `StampedLock` using a shared-exclusive locking strategy for maximum throughput.
- **🧬 Phase Ordering:** Advanced phase management with `PhaseManager`.
  Handles complex dependency graphs gracefully through topological sorting with advanced cycle handling.
- **🌿 Inheritance Support:** Automatically dispatches events to listeners registered for superclasses and interfaces.
- **💎 Modern API:** Fluent builder pattern and full support for Java 8 functional interfaces.

## 🚀 Quick Start
1. Create an Event Bus
```java
EventBus<String> bus = EventBus.create("default");
```

2. Register Listeners
```java
// Register to default phase
bus.register(MyEvent.class, e -> System.out.println("I received the event!"));

// Register to specific phase
bus.register(MyEvent.class, "other", e -> System.out.println("Ciallo～(∠・ω< )⌒★"));
```

3. Post Event
```java
bus.post(new MyEvent());
```

Define Phase Order 
```java
bus.getPhases().link("earlier", "later");
```

Resource Management
```java
EventBus.Registration<String> reg = bus.register(MyEvent.class, e -> { /* do sth */ });
try (EventBus.Resource res = reg.asResource()) { // reg can be inlined if unused
    bus.post(new MyEvent());
    // do sth
} // Automatically unregister here
```

And More

## 📄 License
This project is licensed under the MIT License.

## 💖 Acknowledgements & Inspirations
This project is standing on the shoulders of giants. Special thanks to:

- **[Fabric API](https://github.com/FabricMC/fabric-api)**: The phase ordering system is heavily inspired by Fabric's event system design.
- **[Lombok](https://projectlombok.org/)**: For making Java 8 development feel as smooth as modern languages.
- **[JSpecify](https://jspecify.org/)**: For providing the standard for Java nullness annotations.
- **[JetBrains Annotations](https://github.com/JetBrains/java-annotations)**: Empowering our API with `@Contract` definitions and mutability constraints.
- **[Google Error Prone](https://errorprone.info/)**: Standardizing our return value checks and preventing common programming pitfalls.
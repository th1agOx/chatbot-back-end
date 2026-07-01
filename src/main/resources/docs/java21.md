# Java 21 (LTS)

Java 21 é uma versão de Long-Term Support (LTS) lançada em setembro de 2023. Introduziu diversos recursos que transformam a forma de programar em Java.

## Virtual Threads (Project Loom)

Virtual threads são threads leves gerenciadas pela JVM, permitindo escalar concorrência com baixo custo. Diferente de threads OS, milhares de virtual threads podem ser criadas sem degradação de performance.

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> System.out.println("Virtual thread"));
}
```

## Record Patterns

Permite desestruturar records diretamente em pattern matching:

```java
if (obj instanceof Point(int x, int y)) {
    System.out.println(x + ", " + y);
}
```

## Pattern Matching for Switch

Switch expressions agora aceitam patterns:

```java
return switch (obj) {
    case String s -> "String: " + s;
    case Integer i -> "Int: " + i;
    case null -> "nulo";
    default -> "outro";
};
```

## Sequenced Collections

Novas interfaces `SequencedCollection`, `SequencedSet`, `SequencedMap` com métodos `addFirst`, `addLast`, `getFirst`, `getLast`, `reversed()`.

## Text Blocks (desde Java 15)

Strings multilinha com formatação preservada:

```java
var json = """
    {
        "nome": "João",
        "idade": 30
    }
    """;
```

## Records

Classes imutáveis e transparentes para dados:

```java
public record Pessoa(String nome, int idade) {}
```

## Outros Recursos

- Unnamed Patterns: `case _ ->`
- Virtual Threads: `Thread.startVirtualThread()`
- String Templates (Preview em 21, finalized em 23)

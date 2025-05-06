# Nested Set Tree Utilities for Spring Boot JPA (Kotlin Implementation)

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Maven badge](https://maven-badges.herokuapp.com/maven-central/com.mewebstudio/spring-boot-jpa-nested-set-kotlin/badge.svg?style=flat)](https://central.sonatype.com/artifact/com.mewebstudio/spring-boot-jpa-nested-set-kotlin)
[![javadoc](https://javadoc.io/badge2/com.mewebstudio/spring-boot-jpa-nested-set-kotlin/javadoc.svg)](https://javadoc.io/doc/com.mewebstudio/spring-boot-jpa-nested-set-kotlin)

This package provides a generic and reusable implementation of the [Nested Set Model](https://en.wikipedia.org/wiki/Nested_set_model) for managing hierarchical data using Spring Boot and JPA.

It is designed to be extended and adapted for any entity that implements the `INestedSetNode<ID, T : INestedSetNode<ID, T>>` interface.

---

## 📦 Package Overview

**Package:** `com.mewebstudio.springboot.jpa.nestedset.kotlin`

### Core Components

- **`INestedSetNode<ID, T : INestedSetNode<ID, T>>`**  
  Interface that defines the structure of a nested set node (left, right, parent).

- **`INestedSetNodeResponse<ID>`**  
  Interface for representing nodes with children, used for building hierarchical responses.

- **`JpaNestedSetRepository<T : INestedSetNode<ID, T>, ID> : JpaRepository<T, ID>`**  
  Base JPA repository interface with custom queries for nested set operations (e.g. find ancestors, descendants, siblings).

- **`AbstractNestedSetService<T : INestedSetNode<ID, T>, ID>`**  
  Abstract service class that implements common logic for creating, moving, and restructuring nodes in a nested set tree.

---

## ✅ Features

- Create new nodes in the correct position in the nested set.
- Move nodes up or down among siblings.
- Retrieve ancestors and descendants of a node.
- Rebuild the entire tree from an unordered list of nodes.
- Shift and close gaps in the tree on node deletion.
- Generic and type-safe structure, reusable across multiple domain entities.

---

## 📥 Installation

#### for maven users
Add the following dependency to your `pom.xml` file:
```xml
<dependency>
  <groupId>com.mewebstudio</groupId>
  <artifactId>spring-boot-jpa-nested-set-kotlin</artifactId>
  <version>0.1.0</version>
</dependency>
```
#### for gradle users
Add the following dependency to your `build.gradle` file:
```groovy
implementation 'com.mewebstudio:spring-boot-jpa-nested-set-kotlin:0.1.0'
```

## 🚀 Usage

### 1. Example entity class `INestedSetNode<ID, T : INestedSetNode<ID, T>>`
```kotlin
@Entity
class Category(
    // implement getId, getLeft, getRight, getParent, etc.
) : AbstractBaseEntity(), INestedSetNode<String, Category>
```

### 2. Example repository interface `JpaNestedSetRepository<T : INestedSetNode<ID, T>, ID> : JpaRepository<T, ID>`
```kotlin
interface CategoryRepository : JpaNestedSetRepository<Category, String>
```

### 3. Example service class `AbstractNestedSetService<T : INestedSetNode<ID, T>, ID>`
```kotlin
@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) : AbstractNestedSetService<Category, String>(categoryRepository) {
    // Implement additional service methods as needed
}
```

## 🔁 Other Implementations

[Spring Boot JPA Nested Set (Java Maven Package)](https://github.com/mewebstudio/spring-boot-jpa-nested-set)

## 💡 Example Implementations

[Spring Boot JPA Nested Set - Kotlin Implementation](https://github.com/mewebstudio/spring-boot-jpa-nested-set-kotlin-impl)

[Spring Boot JPA Nested Set - Java Implementation](https://github.com/mewebstudio/spring-boot-jpa-nested-set-java-impl)

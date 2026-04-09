# SEDA Project Template

This repository is a starter template for the SEDA bachelor/master project.
It provides a working JavaFX + Maven setup, including:
- a runnable JavaFX app skeleton with FXML
- unit test support with JUnit
- an example requirements document in LaTeX under `docs/`
- a gitlab CI pipeline that automatically builds and tests the project on push

## Prerequisites

- Java 25+
- Maven 3.9+
- A LaTeX distribution with `pdflatex` available on your `PATH` (e.g., **miktex** or **texlive**)
- Optional: gitlab-ci-local (Requires docker)

## IntelliJ Setup (Recommended)
This template works with any IDE or platform, but we recommend IntelliJ.
The ultimate version is free for students, but the community edition also works fine.
IntelliJ makes debugging, building and testing straightforward and integrates well with Maven and JavaFX.

IntelliJ can be downloaded [from their website](https://www.jetbrains.com/idea/download/) 
or using the [JetBrains Toolbox](https://www.jetbrains.com/toolbox-app/) or with package managers like `winget` or `snap`.
Visit the [installation guide](https://www.jetbrains.com/help/idea/installation-guide.html) for additional information.

You can do most setup directly in IntelliJ:
- **Maven is bundled with IntelliJ**, so you do not need to install Maven manually.
- **Java can be downloaded via IntelliJ** (`Ctrl+Alt+Shift+S > SDKs > + > Download JDK`).
  This avoids changing your global `PATH` on your machine.

### Recommended IntelliJ Plugins
For this template, we recommend:
- **Maven** (usually enabled by default)
- **JUnit** (usually enabled by default)
- **JavaFX** 
- **[TeXiFy-IDEA](https://plugins.jetbrains.com/plugin/9473-texify-idea)** 

Install plugins from `Ctrl+Alt+S > Plugins`.

### Build the Project

Run from the repository root:

```sh
mvn package
```

This compiles the project, runs tests, and creates artifacts in `target/`.

### Build the Docs

The template includes a `docs` Maven profile that compiles `docs/requirements_example.tex` to PDF.

```sh
mvn generate-resources -P docs
```

Generated PDF output is copied to `target/docs/`

### Run the Project

Use the JavaFX Maven plugin:

```sh
mvn javafx:run
```

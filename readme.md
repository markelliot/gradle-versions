Gradle Versions
===============
A plugin for helping detect new versions of dependencies and build plugins and for upgrading those dependencies
and plugins. The update tasks are compatible with `nebula.dependency-recommender` and 
`com.palantir.consistent-versions` versions.props files.

Usage
-----
Apply this plugin using the standard Gradle plugin block:
```gradle
plugins {
   // ...
   id "com.markelliot.versions" version "0.1.11"
}
```

Run one of the available tasks:
* `checkNewVersions`: prints dependencies and plugins with available updates by project, generates
  a `report.yml` file in `${buildDir}/com.markelliot.versions/` with the same details.
* `updateVersionsProps`: (root project only) merges all `report.yml`s and updates root project versions.props
  with the merged recommendations. If two projects produce conflicting version recommendations, no
  update is applied for that dependency.
* `updatePlugins`: (root project only) merges all `report.yml`s and updates all buildscripts `plugins` blocks
  that declare a specific plugin version to use the recommended update.
* `updateGradleWrapper`: (root project only) finds a `gradle-report.yml` and if one exists updates the Gradle
  wrapper to point at the latest version's new distributionUrl.

Caveats
-------
This plugin restricts updates such that:
* Recommended versions do not contain `alpha` or `beta` in the version string.
* Recommended versions may have empty status metadata or the status must be `release`

License
-------
This repository is subject to the [Apache 2.0 License](LICENSE).

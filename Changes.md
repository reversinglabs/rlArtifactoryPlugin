# Changes

This file lists the changes between releases of the rlArtifactoryPlugin, newest first.
Each release lists **Fixed**, **Changed**, **Added** and **Upgrade notes** where applicable.

## 2.1.0

The first release after 2.0.1. It adds Maven and Gradle repositories, follows the rl-protect rules for profiles,
and fixes how the properties file is read.

### Upgrade
Replace **both** of these files in the `plugins` directory, then restart Artifactory or reload the plugins:

- `rlArtifactoryPlugin.groovy`;
- `lib/org-reversinglabs-securesoftwareapi.jar`.

The shipped profiles (`rl-oss-minimum.json`, `rl-oss-baseline.json`, `rl-oss-hardened.json`) are unchanged. Make sure
`rl-oss-baseline.json` is in the `plugins` directory: it is now the fallback when your profile cannot be loaded.

Keep your own `rlArtifactoryPlugin.properties` and `rl-profile.json`, but check them against the upgrade notes below.
The shipped `rlArtifactoryPlugin.properties` has updated comments and defaults; compare it with yours.

### Upgrade notes
- **Check your local profile.** A block or allow entry without a complete `audit` (author, ISO 8601 timestamp,
  reason), or an override with an empty `patterns` list, now makes the whole profile invalid. The plugin then logs
  a WARN and uses `rl-oss-baseline.json` instead. rl-protect rejects the same profiles.
- **Check your purl patterns.** Patterns now follow the rl-protect rules (see "Changed"). Some patterns that matched
  before may no longer match, and the other way round.
- **Maven and Gradle repositories are analyzed after the upgrade** unless `community.inspectPackageTypes` is set. The
  default is now `['npm','pypi','nuget','gems','maven','gradle']`, and each Maven or Gradle binary download costs one
  Community lookup. An explicit list keeps its types. To keep the 2.0 behaviour, set
  `community.inspectPackageTypes = ['npm','pypi','nuget','gems']`.
- **Settings that did nothing before now take effect:** `block_downloads_failed = false`, `never_block_repo_list`,
  `community.autoAdaptToRateLimit = false`, `community.profile.cacheRefreshDelay`, and a `refreshDelay` or
  `notfoundDelay` of `0`. Check that their values are what you want.
- **More Community requests for a while:** packages found in the Community are now refreshed after
  `community.refreshDelay` (default 24 h), as documented. Before, it was `community.notfoundDelay` (240 h).
- The Portal profile cache file is now `<cacheDirectory>/rlArtifactoryPlugin-profile-<org>[-<group>].json`. The old
  `<cacheDirectory>/rl-profile.json` is no longer used and can be deleted.
- **Artifactory 7.173 and later** disable user plugins when the `plugins` directory is empty at the first start, and
  keep them disabled after a plugin is added. See "Activate the plugin" in the README.

### Fixed
- **Community lookups were disabled with the shipped properties file.** Without a `verbose` line, the configuration
  failed to load, so no Community lookups were done and no `secure.software.*` properties were set. Blocking on
  `rl-scan-artifactory` results (`RL.*` properties) kept working.
- Properties that were ignored now work: `block_downloads_failed = false`, `never_block_repo_list`,
  `community.autoAdaptToRateLimit = false`, `community.profile.cacheRefreshDelay` (default now 30 minutes, as
  documented), and `community.refreshDelay = 0` / `community.notfoundDelay = 0`.
- **Packages found in the Community were refreshed after `notfoundDelay` instead of `refreshDelay`.**
- **A missing or invalid profile no longer means an empty policy.** The plugin logs a WARN and uses
  `rl-oss-baseline.json`. Portal accounts: the Portal profile is loaded even when no local profile is present, and
  when the Portal profile cannot be loaded, the local profile is used (WARN).
- **The cached Portal profile is kept per organization and group**, so switching organization or group no longer
  uses the old profile.
- **An override with `"blocker": "fail"` but without `"enabled"` let the package through.** `enabled` now defaults
  to `true`, as in rl-protect.
- **The `secure.software.verdict` property no longer turns into `null`** from the second download of an artifact on.
- **A property you set yourself with the name of a Community result** (for example `status` or `purl`) stopped the
  plugin from writing its own `secure.software.*` result, so a blocked package could be served. The plugin now
  always writes its results and never changes your own properties.
- **Copies of Artifactory's own properties under `secure.software.*`** (for example `secure.software.npm.name`) are
  no longer written, and copies made by earlier versions are removed on the next download.
- Error log lines that include an exception now show the exception text instead of a literal `{}`.
- `community.inspectPackageTypes` with only unsupported types no longer means "analyze everything": unsupported
  names are ignored with a WARN.

### Added
- **Maven repositories.** Binaries (`.jar`, `.war`, `.ear`, `.aar`, not `-sources.jar` / `-javadoc.jar`) are looked
  up in the Spectra Assure Community; `.pom`, checksums, `maven-metadata.xml` and `.module` files are served without
  a lookup. Block messages link to the Maven report.
- **Gradle repositories**, handled as Maven (Artifactory's Gradle repositories use the Maven layout).
- **Override `patterns`:** an override can be limited to some packages with purl patterns.
- **Governance `private` list:** packages you publish yourself. When such a package is found in the public
  Community data, the plugin logs a WARN about possible dependency confusion. The download is not blocked.

### Changed
- **Purl patterns follow the rl-protect rules:** one glob over the whole purl (`*` also crosses `/`),
  case-insensitive, `@scope` equals `%40scope`, PyPI names are normalized, `+` in a version matches `%2B`, a pattern
  without a version covers every version, and qualifiers can be globbed.
- **Profiles are checked as rl-protect checks them.** Each problem in an invalid profile is logged as a WARN, and the
  plugin falls back to `rl-oss-baseline.json` (a Portal profile falls back to the local profile).
- Profile assessment settings accept `"warn"` as well as `"warning"`.
- Normal results (the package report on every lookup, the policy on every profile load) are no longer logged at
  ERROR: they are INFO messages, shown only with `verbose`.
- A WARN is logged at startup when `admin_name` or `admin_email` is empty, and when a proxy is configured but cannot
  be used.
- The User-Agent is now `rlArtifactoryPlugin v2.1.0`.

### Documentation
- `README-properties.md`: customizing a local profile, purl patterns, the refresh delays, the repository types and
  their Community ecosystem, and the `RL.*` properties of `rl-scan-artifactory` (including `RL.meta.*`), which the
  plugin never changes.
- `README.md`: how to enable user plugins on Artifactory 7.173 and later.

## 2.0.1

The previous release. No changelog was kept before 2.1.0.

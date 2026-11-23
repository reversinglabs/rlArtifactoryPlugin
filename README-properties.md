# rlArtifactoryPlugin properties

When the plugin is loaded, it reads configuration settings from the `rlArtifactoryPlugin.properties` file located in the `plugins` 
directory on the JFrog Artifactory server (typically `/opt/jfrog/artifactory/var/etc/artifactory/plugins`). 
The file uses the standard Java properties file format.

The file is read again on every download, so changes take effect without a restart. If the file is missing or
cannot be parsed, the plugin logs an ERROR for each download and runs with its built-in defaults (no Spectra Assure Community lookups, an empty admin contact, `block_downloads_failed = true` and no `never_block_*` lists). 
Artifacts already recorded as blocked (by an earlier lookup or by `rl-scan-artifactory`) therefore **stay blocked**, even when the broken file had `block_downloads_failed = false`.

## Properties of `rl-scan-artifactory` (`RL.*`)

The plugin never writes, changes or deletes properties whose name starts with `RL.`: they belong to
`rl-scan-artifactory`. That includes the `RL.meta.<name>` properties, which `rl-scan-artifactory` can write to hold
the information of its meta file. The plugin only reads `RL.timestamp`, `RL.scan-status` and `RL.scan-report`, to
decide whether a download is blocked. Its own properties all start with `secure.software.`.

## General settings

### Admin contact information

These properties are used to provide a contact name and email in the HTTP 403 response body when the plugin blocks the download of an artifact. 
Contact information can be useful to developers or other users who might not understand why they're prevented from downloading
an artifact.

Example:

```groovy
    admin_name = 'John Doe'
    admin_email = 'john.doe@company.com'
```

NOTE: The name and email will only be visible if the client software requesting the download shows the 403 response. 
Not all client tools do this, so it is recommended to configure logging as mentioned in the [Logging README](README-logging.md).

### Settings related to blocking downloads

These properties allow modifying the plugin behavior related to blocking artifact downloads. 
By default, blocking is enabled for all repositories in your Artifactory instance. 
Blocking only happens if the `secure.software.status` property on the artifact is set to `fail`. The plugin sets that property when the artifact does not pass the configured security policy.

To configure the plugin to skip the blocking action regardless of an artifact's security status, set `block_downloads_failed` to 
'false' as follows. The default value is 'true'. 

This effectively stops the plugin from blocking any downloads. This configuration is recommended for intitial setup and testing of the plugin. 

```groovy
    block_downloads_failed = false
```

To skip the blocking action for all artifacts in specific repositories, you can configure `never_block_repo_list` as in 
the following example.

```groovy
    never_block_repo_list = [
        'repo-name-1',
        'repo-name-2'
    ]
```

To ensure that download requests from specific IP addresses will never be blocked, you can configure `never_block_ip_list` as in 
the following example. For Spectra Assure customers on the Essentials or Enterprise tier, this setting is recommended to allow downloads to the machine that runs the `rl-scan-artifactory` integration.

```groovy
    never_block_ip_list = [
        '1.2.3.4',
        '5.6.7.8'
    ]
```

## API settings

API connectivity is required to retrieve the security status of OSS packages that are indexed on Spectra Assure Community. 
The plugin supports two different APIs:

- **Spectra Assure Community API** (free, limited monthly lookups)
- **Spectra Assure Portal API** (for customers on the Essentials or Enterprise tier, unlimited lookups)

Both APIs require setting the 'server' and 'token' properties. 

When using the free Community API, configure the server and token as follows. **The Community API token begins with 'rlcmm-'**.

```groovy
    community.server = 'data.reversinglabs.com'
    community.token = '<your API token>'
```

If using the Portal API, configure the server to match the secure.software endpoint that was assigned to your organization (e.g., 
'my.secure.software/company'). **The Portal API token begins with 'rls3c-'**. 

An additional property is required to set the Portal organization. Optionally, the Portal group can also be configured.

```groovy
    community.server = 'my.secure.software/company'
    community.token = '<your API token>'
    community.profile.portalOrganization = '<your organization as it appears in the Portal UI>'
    community.profile.portalGroup = '<group name as it appears in the Portal UI>' // optional
```

## Policy profile settings

A default policy profile will be used by the plugin automatically.

A ReversingLabs policy profile is defined in JSON format according to the [rl-profile schema](https://docs.secure.software/cli/rl-profile-schema).
Three (3) pre-defined profiles are provided:

- Minimum security level: `rl-oss-minimum.json`
- Baseline security level: `rl-oss-baseline.json`
- Hardened security level: `rl-oss-hardened.json`

By default, the plugin uses the baseline profile, which is loaded from `rl-profile.json` located in the same directory as the properties file. 
If desired, the plugin can be configured to use a different profile by setting the 'community.profile.path' property. The value is 
relative to the Artifactory 'plugins' directory.

```groovy
    community.profile.path = 'rl-profile.json'
```

If the configured profile file is missing or invalid, the plugin logs a warning and falls back to
`rl-oss-baseline.json` in the `plugins` directory. Keep that file installed.

### Customizing a local profile

Start from one of the three provided profiles and edit `configuration.policy`. The plugin evaluates a profile the same
way as rl-protect, and accepts the same profiles. The main settings:

- `governance.community.allow` / `block`: packages that are always allowed / always blocked.
- `governance.community.min_package_age` / `min_version_age`: block packages or versions that are too new (days).
- `governance.community.private`: packages you publish yourself (see below).
- `overrides`: change the result of one policy (rule ID, e.g. `SQ31102`), optionally only for some packages.

Each `allow`, `block` and `private` entry is a purl pattern **with an audit record** (who, when, why):

```json
"governance": {
  "community": {
    "min_package_age": 30,
    "min_version_age": 1,
    "allow": [
      { "pattern": "pkg:pypi/flask@3.1.*",
        "audit": { "author": "jane@example.com", "timestamp": "2026-11-06T10:00:00+0000", "reason": "Reviewed" } }
    ],
    "block": [
      { "pattern": "pkg:npm/ua-parser-js@0.7.*",
        "audit": { "author": "jane@example.com", "timestamp": "2026-11-06T10:00:00+0000", "reason": "Compromised releases" } }
    ],
    "private": [
      { "pattern": "pkg:npm/@mycorp/*",
        "audit": { "author": "jane@example.com", "timestamp": "2026-11-06T10:00:00+0000", "reason": "Our internal scope" } }
    ]
  }
}
```

**Purl patterns** (allow, block, private and override `patterns`):

- `*` matches any text, including `/`; `[abc]` and `[!abc]` match one character; `\` escapes; `?` is a literal
  (it starts the qualifiers in a purl).
- Matching ignores case. `pkg:npm/@scope/name` and `pkg:npm/%40scope/name` are the same.
  PyPI names are compared with `-`, `_` and `.` all as `-` (`typing_extensions` = `typing-extensions`).
- A pattern without a version (`pkg:pypi/six`) covers every version; `pkg:pypi/six@1.16.*` covers some.
- Qualifiers are ignored unless the pattern has one (`pkg:pypi/six@1.16.0?artifact=*.whl`).

**Overrides limited to some packages.** Without `patterns` an override applies to every package. With `patterns`
it applies only to the packages that match. An empty `patterns` list is not allowed.

```json
"overrides": {
  "SQ31102": {
    "enabled": false,
    "patterns": ["pkg:npm/jshint@*"],
    "audit": { "author": "jane@example.com", "timestamp": "2026-11-06T10:00:00+0000", "reason": "False positive" }
  }
}
```

`"enabled": false` turns the policy off for those packages; `"blocker": "fail"` makes it always fail.
A missing `enabled` means `true`.

**Private packages and dependency confusion.** List the packages you publish yourself under `private`. When such a
package is found in the public Spectra Assure Community data, someone has published a package with your name on a
public registry: a possible dependency-confusion attack. The plugin logs a **WARN** with the matching entry and its
audit, for example:

```
Declared-private package 'pkg:npm/%40mycorp/tools@1.0.0' resolves on the public registry - possible dependency confusion (declared private by jane@example.com at 2026-11-06T10:00:00+0000: Our internal scope)
```

The download itself is **not** blocked by this; add a `block` entry if you want that.

**Profile checks.** A profile with an error is not used: the plugin logs a WARN per problem and falls back (see
above). Typical errors: an `allow`/`block`/`private` entry without a complete `audit`; an audit timestamp that is not
ISO 8601 (`2026-11-06T10:00:00+0000`); a pattern that is not a purl or has an unclosed `[`; an empty `patterns` list;
`blocker` other than `pass`/`fail`; `rl_scan_level` outside 1–5.

NOTE: If using the Portal API, the profile defined at the organization or group level on your Portal instance will be used. 

### For Portal API users only

For Portal API users, the plugin can use **either** an organization-level profile **or** a group-level profile. The correct profile will automatically be downloaded when configured.

When using an organization profile, omit the `community.profile.portalGroup` property.

When using a group profile, both the `community.profile.portalOrganization` and the `community.profile.portalGroup` must be set.

```groovy
    community.profile.portalOrganization = '<org name>'
    community.profile.portalGroup = '<group name>'
```

When using any profile from the Portal, the profile is downloaded and cached for a configurable amount of time. The location of the cached profile and the cache refresh time can be modified as follows.

```groovy
    // community.profile.cacheDirectory: string, optional, default: '/var/tmp'. // must exist and be writable.
    community.profile.cacheDirectory = '/var/tmp'

    // community.profile.cacheRefreshDelay: integer (minutes), optional, default: 30.
    community.profile.cacheRefreshDelay = 30
```

## Other settings

If your Artifactory server needs a proxy to reach the internet, you can configure a proxy server as follows. The username and password 
are required only if the proxy requires authentication.

```groovy
    proxy.server = '<proxy dns-name or ip-addr>'
    proxy.port = <proxy port>
    proxy.username = '<proxy username>'
    proxy.password = '<proxy password>'
```

The rlArtifactoryPlugin currently supports the following types of Artifactory repositories:

 - npm
 - PyPI
 - NuGet
 - RubyGems
 - Maven (since 2.2.0; only binaries are looked up: `.jar`, `.war`, `.ear`, `.aar`, not `-sources.jar` / `-javadoc.jar`.
   Other files in a Maven repo, such as `.pom`, checksums and `maven-metadata.xml`, are served without a lookup.)
 - Gradle (since 2.3.0; Artifactory Gradle repositories use the Maven layout, so the same files are looked up as for
   Maven. Gradle `.module` files and Gradle plugin marker `.pom` files are served without a lookup.)

To exclude certain repository/package types from being analyzed by the plugin, configure the `community.inspectPackageTypes` list
as desired. All supported repositories are included by default.

```groovy
    community.inspectPackageTypes = [ 'npm','pypi','nuget','gems','maven','gradle']
```

The names in `community.inspectPackageTypes` are **Artifactory repository package types**, not Spectra Assure
Community ecosystems. The plugin looks up each file by its SHA-256 hash, and the Community reports the package it matched with the file. A Gradle repository holds Maven artifacts, so there is no separate Gradle ecosystem:

| Artifactory repository type | Spectra Assure Community ecosystem | Purl in profile patterns |
|---|---|---|
| `npm` | npm | `pkg:npm/...` |
| `pypi` | PyPI | `pkg:pypi/...` |
| `nuget` | NuGet | `pkg:nuget/...` |
| `gems` | RubyGems | `pkg:gem/...` |
| `maven` | Maven | `pkg:maven/...` |
| `gradle` | Maven | `pkg:maven/...` |

NOTE: Gradle plugins that are only published on the Gradle Plugin Portal (`plugins.gradle.org`), and not on
Maven Central, are probably not in the Spectra Assure Community. For such a file the plugin finds no data, and the
download is allowed, as for any other file the Community does not know.

Unsupported names are ignored with a WARN. If the list contains no supported type at all, **no** repository is
analyzed (and a WARN is logged); before 2.2.0 such a list meant "analyze everything".

If the plugin starts to see HTTP 429 responses from the API, the rate at which requests are sent is adapted automatically. 
This can be disabled - although not recommended - by setting `community.autoAdaptToRateLimit` to 'false'. 

Additionally, an API request that receives the HTTP 429 response is automatically retried. The default delay of the retry is 10 seconds, but this can be changed with the `community.defaultDelayRateLimit` setting.

```groovy
    community.autoAdaptToRateLimit = true
    community.defaultDelayRateLimit = 10
```

To save bandwith and cost, the API is not invoked by the plugin when recent Community data already exists as properties 
on the artifact or when there is no Community data available for the artifact.

The time delays for when data is deemed recent or not can be configured as follows. Both values must be specified in hours.

```groovy
    // if community data exists on the artifact, refresh it after x number of hours, default: 24
    community.refreshDelay = 24

    // if community data does not exist on the artifact, default: 240 (10 days)
    community.notfoundDelay = 240
```

The age of the last lookup is counted in whole hours, rounded down. The next lookup is done when the last one is
at least `delay + 1` hours old: with `refreshDelay = 24` after 25 hours, with `0` after 1 hour. A delay of `0`
therefore means "at most once per hour", not "on every download". An artifact without any lookup yet is always
looked up.


# About rlArtifactoryPlugin

ReversingLabs provides this JFrog Artifactory plugin to check the safety of open source software (OSS) packages and dependencies in your projects. It leverages the [Spectra Assure Community](https://secure.software/)
data set and can be used with a free Community API token. Alternatively, Spectra Assure customers on the Essentials or Enterprise tier can use a Portal API token for unlimited lookups. More information about [supported API tiers](https://docs.secure.software/community/tiers-guide) is available in the official Spectra Assure Community documentation.

A key feature of the plugin is blocking downloads of unsafe software artifacts, preventing them from being used by development teams and spreading in corporate environments.
When the Artifactory REST API is used to download/retrieve an unsafe artifact, the plugin returns an HTTP 403 error and the response includes a link to the report so the users can better understand why the error occurred.

NOTE: A separate ReversingLabs integration ([rl-scan-artifactory](https://pypi.org/project/rl-scan-artifactory/)) is available to Spectra Assure customers on the Essentials or Enterprise tier. This integration can be used to scan full binary software packages with Spectra Assure. It may be used in conjunction with this plugin, but it is not required.


## Plugin installation

The `plugins` directory is typically located in:

- `/opt/jfrog/artifactory/var/etc/artifactory/plugins/`

The `plugins-lib` directory is typically:

- `/opt/jfrog/artifactory/var/etc/artifactory/plugins/lib/`

If needed, create the `plugins-lib` directory
and set the user and group to `artifactory` with the following commands:

```sh
    # as root or as user artifactory
    cd /opt/jfrog/artifactory/var/etc/artifactory/plugins/
    mkdir -p lib/
    chown artifactory:artifactory lib/
    chmod 775 lib/
```

Install steps:

1. Copy the `rlArtifactoryPlugin.groovy` script into the `plugins` directory.
1. Copy the `rlArtifactoryPlugin.properties` file into the `plugins` directory.
1. Place the `org-reversinglabs-securesoftwareapi.jar` file in the `plugins-lib` directory.
1. Copy the 4 `json` files into the `plugins` directory.

The 4 json files are:

- `rl-oss-baseline.json`
- `rl-oss-hardened.json`
- `rl-oss-minimum.json`
- `rl-profile.json`

Ensure all files are set to 'user:group' of `artifactory:artifactory` and mode 644:

```sh
    # as root or as user artifactory
    cd /opt/jfrog/artifactory/var/etc/artifactory/plugins/

    chown artifactory:artifactory rlArtifactoryPlugin.groovy
    chown artifactory:artifactory rlArtifactoryPlugin.properties
    chown artifactory:artifactory lib/org-reversinglabs-securesoftwareapi.jar
    chown artifactory:artifactory rl-*.json

    chmod 644 rlArtifactoryPlugin.groovy
    chmod 644 rlArtifactoryPlugin.properties
    chmod 644 lib/org-reversinglabs-securesoftwareapi.jar
    chmod 644 rl-*.json
```

## General plugin configuration

To configure the plugin as desired, open `rlArtifactoryPlugin.properties` at the following location:

```
    /opt/jfrog/artifactory/var/etc/artifactory/plugins/rlArtifactoryPlugin.properties
```

All configuration options are documented in [README-properties](README-properties.md)

## Activate the plugin

Restart Artifactory to activate the plugin and apply configuration changes. To reload the plugin without restarting, Artifactory's `Reload plugins` API endpoint can be used.

NOTE: Recent Artifactory versions (seen with 7.173) **disable user plugins automatically** when the `plugins`
directory is empty at startup, and keep them disabled after a plugin is added. The plugin then does not run and its
log stays empty. You can recognize this state in two ways:

- `artifactory-service.log` has *"The plugin infrastructure was disabled automatically on an earlier startup and
  stays disabled"*;
- `GET /artifactory/api/plugins` answers *"Plugin infrastructure is disabled."*

To enable user plugins, add this line to `/opt/jfrog/artifactory/var/etc/artifactory/artifactory.system.properties`
and restart Artifactory:

```
artifactory.plugin.infrastructure.enabled=true
```

When the log says the infrastructure is *"disabled by entitlement"*, this setting does not help. In that case, it is recommended to contact JFrog for assistance.

## Logging

Optionally, the plugin can be configured to log all activity to a file. This is helpful for troubleshooting purposes.
Find more information on how to set this up in the [Logging README](README-logging.md).

## Reference: Artifact properties

After looking up an OSS package in the Spectra Assure Community data set, the plugin sets a number of properties on the artifact in Artifactory.
All properties begin with 'secure.software'. The *secure.software.status* property indicates whether the package passed policy or not, and determines if the plugin will prevent the artifact from being downloaded.

The full list of properties set by the plugin is provided below.

| Name | Type |  Notes |
| --   | --   | --     |
| secure.software.artifact | string | |
| secure.software.assessments | string | |
| secure.software.community | string | |
| secure.software.community_url | url | Link to the full report on Spectra Assure Community |
| secure.software.deprecated | boolean | |
| secure.software.first_published | date | |
| secure.software.hash | string | SHA-256 hash of the package |
| secure.software.is_quarantined | boolean | Whether the package is marked as quarantined on Spectra Assure Community |
| secure.software.lastUpdate | date | |
| secure.software.namespace | string | |
| secure.software.overrides | string | |
| secure.software.package | string | Name of the package |
| secure.software.package_days_ago | integer | Number of days since the initial version was published on the public registry |
| secure.software.product | string | Name of the software product |
| secure.software.published | date | Date the package was published on the public registry |
| secure.software.purl | string | Package URL of the package |
| secure.software.qual_assessment | string | |
| secure.software.removed | boolean | 'true' indicates package version was removed from the public registry |
| secure.software.status | string | 'fail' causes the plugin to block download |
| secure.software.type | string | |
| secure.software.uri | string | |
| secure.software.url_community | string | |
| secure.software.verdict | string | |
| secure.software.version_days_ago | integer | Number of days since the package version was published |
| secure.software.version | string | Version number of the package |
| secure.software.was_archived | boolean |  |

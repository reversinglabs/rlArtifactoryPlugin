# rlArtifactoryPlugin properties

When the plugin is loaded, it reads configuration settings from the `rlArtifactoryPlugin.properties` file located in the `plugins` 
directory on the JFrog Artifactory server (typically `/opt/jfrog/artifactory/var/etc/artifactory/plugins`). 
The file uses the standard Java properties file format.

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

To exclude certain repository/package types from being analyzed by the plugin, configure the `community.inspectPackageTypes` list
as desired. All supported repositories are included by default.

```groovy
    community.inspectPackageTypes = [ 'npm','pypi','nuget','gems']
```

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


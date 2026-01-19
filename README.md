# rlArtifactoryPlugin

ReversingLabs provides a separate Artifactory plugin
called `rlArtifactoryPlugin` that can automatically
block download attempts for artifacts with the scan result set to `fail`.

To install the plugin,
copy the `rlArtifactoryPlugin.groovy` script into the `/opt/jfrog/artifactory/var/etc/artifactory/plugins/` directory.

The plugin works in combination with:
[rl-scan-artifactory](https://github.com/reversinglabs/rl-scan-artifactory) .

## Plugin configuration

To activate the plugin, create the `rlArtifactoryPlugin.properties` file in the plugin directory:

```
/opt/jfrog/artifactory/var/etc/artifactory/plugins/rlArtifactoryPlugin.properties
```

### Block
In the `rlArtifactoryPlugin.properties` file, set the `block_downloads_failed` field to `'true'`.
This is required for the plugin to function.

```
block_downloads_failed = 'true'
```

### Allow

The `rlArtifactoryPlugin.properties` file also allows optional allowlisting for repositories and IP addresses.

To ensure that specific repositories will never block any download request,
you can populate the field `never_block_repo_list` like in the following example:

```
never_block_repo_list = [
    'repoName1',
    'repoName2'
]
```

To ensure that download requests from specific IP addresses will never be blocked,
you can populate the field `never_block_ip_list` like in the following example:

```
never_block_ip_list = [
    '1.2.3.4',
    '5.6.7.8'
]
```

This option is particularly suited for allowing all downloads from the host that runs the `rl-scan-artifactory` integration.

### Feedback

You can also add information to the 403 reply from rlArtifactoryPlugin how to contact an administrator by using:

```
admin_name = 'Your Friendly Administrator'
admin_email = 'your.friendly@administrator.noreply'
```

### Report

The 403 reply from rlArtifactoryPlugin will show the link to the report
so the user can find out more about the reasons whey the block was  enforced.

### Activate

Restart Artifactory to activate the plugin and apply configuration changes.


## Logging

Optionally, logging can be configured to write all activities of the `rlArtifactoryPlugin` plugin to the default logging directory in Artifactory:

- `/opt/jfrog/artifactory/var/log/rlArtifactoryPlugin.log`

To do this, modify the logging configuration file:

- `/opt/jfrog/artifactory/var/etc/artifactory/logback.xml`

At the end of the file just before the closing XML tag `</configuration>`, add the following:

```xml
  <!--Plugin: rlArtifactoryPlugin appender -->
  <appender name="rlArtifactoryPlugin" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <File>${log.dir}/rlArtifactoryPlugin.log</File>
    <encoder>
      <pattern>%date{yyyy-MM-dd'T'HH:mm:ss.SSS, UTC}Z [%p] - %m%n</pattern>
    </encoder>
    <rollingPolicy class="ch.qos.logback.core.rolling.FixedWindowRollingPolicy">
      <FileNamePattern>${log.dir.archived}/rlArtifactoryPlugin.%i.log.gz</FileNamePattern>
      <maxIndex>13</maxIndex>
    </rollingPolicy>
    <triggeringPolicy class="ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy">
      <MaxFileSize>25MB</MaxFileSize>
    </triggeringPolicy>
  </appender>
  <!--Plugin: rlArtifactoryPlugin logger -->
  <logger name="rlArtifactoryPlugin" level="info" additivity="false">
    <level value="info" />
    <appender-ref ref="rlArtifactoryPlugin" />
  </logger>
```

You can modify the `maxIndex` and `MaxFileSize` values to suit your requirements.

Restart Artifactory to apply your logging configuration changes.

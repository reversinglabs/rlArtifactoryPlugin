# rlArtifactoryPlugin

## Logging

Optionally, logging can be configured to write all activities of the `rlArtifactoryPlugin` integration to the default logging directory in Artifactory:

- `/opt/jfrog/artifactory/var/log/rlArtifactoryPlugin.log`

To do this, modify the logging configuration file:

- `/opt/jfrog/artifactory/var/etc/artifactory/logback.xml`

At the end of the file, before the closing XML tag `</configuration>`, add the following:

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

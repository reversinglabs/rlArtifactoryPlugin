# Artifactory logging for rlArtifactoryPlugin.groovy

Add at the end of `/var/opt/jfrog/artifactory/etc/artifactory/logback.xml`
but before the last line with `</configuration>`

change `maxIndex` and `MaxFileSize` to your requirements.

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

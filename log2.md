root@ls-V56axQHn:~/langchain4j-agent# sudo kubectl logs -n langchain4j-agent -l app=langchain4j-agent --tail=100
09:58:02,123 |-INFO in ch.qos.logback.classic.util.ContextInitializer@24faea88 - ch.qos.logback.classic.joran.SerializedModelConfigurator.configure() call lasted 9 milliseconds. ExecutionStatus=INVOKE_NEXT_IF_ANY
09:58:02,123 |-INFO in ch.qos.logback.classic.util.ContextInitializer@24faea88 - Trying to configure with ch.qos.logback.classic.util.DefaultJoranConfigurator
09:58:02,177 |-INFO in ch.qos.logback.classic.util.ContextInitializer@24faea88 - Constructed configurator of type class ch.qos.logback.classic.util.DefaultJoranConfigurator
09:58:02,206 |-INFO in ch.qos.logback.classic.LoggerContext[default] - Could NOT find resource [logback-test.xml]
09:58:02,209 |-INFO in ch.qos.logback.classic.LoggerContext[default] - Could NOT find resource [logback.xml]
09:58:02,209 |-INFO in ch.qos.logback.classic.util.ContextInitializer@24faea88 - ch.qos.logback.classic.util.DefaultJoranConfigurator.configure() call lasted 32 milliseconds. ExecutionStatus=INVOKE_NEXT_IF_ANY
09:58:02,209 |-INFO in ch.qos.logback.classic.util.ContextInitializer@24faea88 - Trying to configure with ch.qos.logback.classic.BasicConfigurator
09:58:02,210 |-INFO in ch.qos.logback.classic.util.ContextInitializer@24faea88 - Constructed configurator of type class ch.qos.logback.classic.BasicConfigurator
09:58:02,210 |-INFO in ch.qos.logback.classic.BasicConfigurator@3a320ade - Setting up default configuration.
09:58:02,406 |-INFO in ch.qos.logback.core.ConsoleAppender[console] - BEWARE: Writing to the console can be very slow. Avoid logging to the
09:58:02,406 |-INFO in ch.qos.logback.core.ConsoleAppender[console] - console in production environments, especially in high volume systems.
09:58:02,406 |-INFO in ch.qos.logback.core.ConsoleAppender[console] - See also https://logback.qos.ch/codes.html#slowConsole
09:58:02,406 |-INFO in ch.qos.logback.classic.util.ContextInitializer@24faea88 - ch.qos.logback.classic.BasicConfigurator.configure() call lasted 196 milliseconds. ExecutionStatus=NEUTRAL
09:58:06,218 |-INFO in ConfigurationWatchList(mainURL=jar:nested:/app/app.jar/!BOOT-INF/classes/!/logback-spring.xml, fileWatchList={}, urlWatchList=[}) - URL [jar:nested:/app/app.jar/!BOOT-INF/classes/!/logback-spring.xml] is not of type file
09:58:06,713 |-INFO in ch.qos.logback.core.joran.util.ConfigurationWatchListUtil@bcec031 - Adding [jar:nested:/app/app.jar/!BOOT-INF/lib/spring-boot-3.4.2.jar!/org/springframework/boot/logging/logback/defaults.xml] to configuration watch list.
09:58:06,713 |-INFO in ConfigurationWatchList(mainURL=jar:nested:/app/app.jar/!BOOT-INF/classes/!/logback-spring.xml, fileWatchList={}, urlWatchList=[}) - Cannot watch [jar:nested:/app/app.jar/!BOOT-INF/lib/spring-boot-3.4.2.jar!/org/springframework/boot/logging/logback/defaults.xml] as its protocol is not one of file, http or https.
09:58:06,724 |-INFO in ch.qos.logback.core.model.processor.ConversionRuleModelHandler - registering conversion word applicationName with class [org.springframework.boot.logging.logback.ApplicationNameConverter]
09:58:06,725 |-INFO in ch.qos.logback.core.model.processor.ConversionRuleModelHandler - registering conversion word clr with class [org.springframework.boot.logging.logback.ColorConverter]
09:58:06,725 |-INFO in ch.qos.logback.core.model.processor.ConversionRuleModelHandler - registering conversion word correlationId with class [org.springframework.boot.logging.logback.CorrelationIdConverter]
09:58:06,725 |-INFO in ch.qos.logback.core.model.processor.ConversionRuleModelHandler - registering conversion word esb with class [org.springframework.boot.logging.logback.EnclosedInSquareBracketsConverter]
09:58:06,725 |-INFO in ch.qos.logback.core.model.processor.ConversionRuleModelHandler - registering conversion word wex with class [org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter]
09:58:06,725 |-INFO in ch.qos.logback.core.model.processor.ConversionRuleModelHandler - registering conversion word wEx with class [org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter]
09:58:06,776 |-INFO in ch.qos.logback.core.model.processor.ModelInterpretationContext@21005f6c - value "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n" substituted for "${CONSOLE_LOG_PATTERN:-%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd'T'HH:mm:ss.SSSXXX}}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}){} %clr(${PID:-}){magenta} %clr(--- %esb(){APPLICATION_NAME}%esb{APPLICATION_GROUP}[%15.15t] ${LOG_CORRELATION_PATTERN:-}){faint}%clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"
09:58:06,776 |-INFO in ch.qos.logback.core.model.processor.ModelInterpretationContext@21005f6c - value "UTF-8" substituted for "${CONSOLE_LOG_CHARSET:-${file.encoding:-UTF-8}}"
09:58:06,776 |-INFO in ch.qos.logback.core.model.processor.ModelInterpretationContext@21005f6c - value "TRACE" substituted for "${CONSOLE_LOG_THRESHOLD:-TRACE}"
09:58:06,776 |-INFO in ch.qos.logback.core.model.processor.ModelInterpretationContext@21005f6c - value "" substituted for "${CONSOLE_LOG_STRUCTURED_FORMAT:-}"
09:58:06,777 |-INFO in ch.qos.logback.core.model.processor.ModelInterpretationContext@21005f6c - value "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n" substituted for "${FILE_LOG_PATTERN:-%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd'T'HH:mm:ss.SSSXXX}} ${LOG_LEVEL_PATTERN:-%5p} ${PID:-} --- %esb(){APPLICATION_NAME}%esb{APPLICATION_GROUP}[%t] ${LOG_CORRELATION_PATTERN:-}%-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"
09:58:06,777 |-INFO in ch.qos.logback.core.model.processor.ModelInterpretationContext@21005f6c - value "UTF-8" substituted for "${FILE_LOG_CHARSET:-${file.encoding:-UTF-8}}"
09:58:06,777 |-INFO in ch.qos.logback.core.model.processor.ModelInterpretationContext@21005f6c - value "TRACE" substituted for "${FILE_LOG_THRESHOLD:-TRACE}"
09:58:06,777 |-INFO in ch.qos.logback.core.model.processor.ModelInterpretationContext@21005f6c - value "" substituted for "${FILE_LOG_STRUCTURED_FORMAT:-}"
09:58:06,809 |-INFO in ch.qos.logback.classic.model.processor.LoggerModelHandler - Setting level of logger [org.apache.catalina.startup.DigesterFactory] to ERROR
09:58:06,809 |-INFO in ch.qos.logback.classic.jul.LevelChangePropagator@32f0fba8 - Propagating ERROR level on Logger[org.apache.catalina.startup.DigesterFactory] onto the JUL framework
09:58:06,810 |-INFO in ch.qos.logback.classic.model.processor.LoggerModelHandler - Setting level of logger [org.apache.catalina.util.LifecycleBase] to ERROR
09:58:06,810 |-INFO in ch.qos.logback.classic.jul.LevelChangePropagator@32f0fba8 - Propagating ERROR level on Logger[org.apache.catalina.util.LifecycleBase] onto the JUL framework
09:58:06,810 |-INFO in ch.qos.logback.classic.model.processor.LoggerModelHandler - Setting level of logger [org.apache.coyote.http11.Http11NioProtocol] to WARN
09:58:06,810 |-INFO in ch.qos.logback.classic.jul.LevelChangePropagator@32f0fba8 - Propagating WARN level on Logger[org.apache.coyote.http11.Http11NioProtocol] onto the JUL framework
09:58:06,810 |-INFO in ch.qos.logback.classic.model.processor.LoggerModelHandler - Setting level of logger [org.apache.sshd.common.util.SecurityUtils] to WARN
09:58:06,810 |-INFO in ch.qos.logback.classic.jul.LevelChangePropagator@32f0fba8 - Propagating WARN level on Logger[org.apache.sshd.common.util.SecurityUtils] onto the JUL framework
09:58:06,810 |-INFO in ch.qos.logback.classic.model.processor.LoggerModelHandler - Setting level of logger [org.apache.tomcat.util.net.NioSelectorPool] to WARN
09:58:06,810 |-INFO in ch.qos.logback.classic.jul.LevelChangePropagator@32f0fba8 - Propagating WARN level on Logger[org.apache.tomcat.util.net.NioSelectorPool] onto the JUL framework
09:58:06,810 |-INFO in ch.qos.logback.classic.model.processor.LoggerModelHandler - Setting level of logger [org.eclipse.jetty.util.component.AbstractLifeCycle] to ERROR
09:58:06,810 |-INFO in ch.qos.logback.classic.jul.LevelChangePropagator@32f0fba8 - Propagating ERROR level on Logger[org.eclipse.jetty.util.component.AbstractLifeCycle] onto the JUL framework
09:58:06,811 |-INFO in ch.qos.logback.classic.model.processor.LoggerModelHandler - Setting level of logger [org.hibernate.validator.internal.util.Version] to WARN
09:58:06,811 |-INFO in ch.qos.logback.classic.jul.LevelChangePropagator@32f0fba8 - Propagating WARN level on Logger[org.hibernate.validator.internal.util.Version] onto the JUL framework
09:58:06,811 |-INFO in ch.qos.logback.classic.model.processor.LoggerModelHandler - Setting level of logger [org.springframework.boot.actuate.endpoint.jmx] to WARN
09:58:06,811 |-INFO in ch.qos.logback.classic.jul.LevelChangePropagator@32f0fba8 - Propagating WARN level on Logger[org.springframework.boot.actuate.endpoint.jmx] onto the JUL framework
09:58:06,811 |-INFO in ch.qos.logback.core.model.processor.AppenderModelHandler - Processing appender named [CONSOLE]
09:58:06,811 |-INFO in ch.qos.logback.core.model.processor.AppenderModelHandler - About to instantiate appender of type [ch.qos.logback.core.ConsoleAppender]
09:58:06,818 |-INFO in ch.qos.logback.core.model.processor.ImplicitModelHandler - Assuming default type [ch.qos.logback.classic.encoder.PatternLayoutEncoder] for [encoder] property
09:58:06,822 |-INFO in ch.qos.logback.core.model.processor.ModelInterpretationContext@21005f6c - value "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n" substituted for "${CONSOLE_LOG_PATTERN}"
09:58:07,015 |-INFO in ch.qos.logback.core.ConsoleAppender[CONSOLE] - BEWARE: Writing to the console can be very slow. Avoid logging to the
09:58:07,015 |-INFO in ch.qos.logback.core.ConsoleAppender[CONSOLE] - console in production environments, especially in high volume systems.
09:58:07,015 |-INFO in ch.qos.logback.core.ConsoleAppender[CONSOLE] - See also https://logback.qos.ch/codes.html#slowConsole
09:58:07,016 |-WARN in ch.qos.logback.core.model.processor.AppenderModelHandler - Appender named [FILE] not referenced. Skipping further processing.
09:58:07,016 |-INFO in ch.qos.logback.classic.model.processor.RootLoggerModelHandler - Setting level of ROOT logger to INFO
09:58:07,016 |-INFO in ch.qos.logback.classic.jul.LevelChangePropagator@32f0fba8 - Propagating INFO level on Logger[ROOT] onto the JUL framework
09:58:07,016 |-INFO in ch.qos.logback.core.model.processor.AppenderRefModelHandler - Attaching appender named [CONSOLE] to Logger[ROOT]
09:58:07,016 |-INFO in ch.qos.logback.core.model.processor.DefaultProcessor@545de5a4 - End of configuration.
09:58:07,018 |-INFO in org.springframework.boot.logging.logback.SpringBootJoranConfigurator@29ef6856 - Registering current configuration as safe fallback point


.   ____          _            __ _ _
/\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
\\/  ___)| |_)| | | | | || (_| |  ) ) ) )
'  |____| .__|_| |_|_| |_\__, | / / / /
=========|_|==============|___/=/_/_/_/

:: Spring Boot ::                (v3.4.2)

2026-05-29 09:58:07 [main] INFO  c.y.l.LangChain4jAgentDemoApplication - Starting LangChain4jAgentDemoApplication v1.0.0 using Java 21.0.11 with PID 6 (/app/app.jar started by ? in /app)
2026-05-29 09:58:07 [main] INFO  c.y.l.LangChain4jAgentDemoApplication - The following 1 profile is active: "production"
2026-05-29 09:58:17 [main] INFO  o.s.d.r.c.RepositoryConfigurationDelegate - Multiple Spring Data modules found, entering strict repository configuration mode
2026-05-29 09:58:17 [main] INFO  o.s.d.r.c.RepositoryConfigurationDelegate - Bootstrapping Spring Data JPA repositories in DEFAULT mode.
2026-05-29 09:58:18 [main] INFO  o.s.d.r.c.RepositoryConfigurationDelegate - Finished Spring Data repository scanning in 993 ms. Found 3 JPA repository interfaces.
2026-05-29 09:58:26 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat initialized with port 8080 (http)
2026-05-29 09:58:26 [main] INFO  o.a.catalina.core.StandardService - Starting service [Tomcat]
2026-05-29 09:58:26 [main] INFO  o.a.catalina.core.StandardEngine - Starting Servlet engine: [Apache Tomcat/10.1.34]
2026-05-29 09:58:27 [main] INFO  o.a.c.c.C.[Tomcat].[localhost].[/] - Initializing Spring embedded WebApplicationContext
2026-05-29 09:58:27 [main] INFO  o.s.b.w.s.c.ServletWebServerApplicationContext - Root WebApplicationContext: initialization completed in 18896 ms
2026-05-29 09:58:30 [main] INFO  o.h.jpa.internal.util.LogHelper - HHH000204: Processing PersistenceUnitInfo [name: default]
2026-05-29 09:58:30 [main] INFO  org.hibernate.Version - HHH000412: Hibernate ORM core version 6.6.5.Final
2026-05-29 09:58:30 [main] INFO  o.h.c.i.RegionFactoryInitiator - HHH000026: Second-level cache disabled
2026-05-29 09:58:32 [main] INFO  o.s.o.j.p.SpringPersistenceUnitInfo - No LoadTimeWeaver setup: ignoring JPA class transformer
2026-05-29 09:58:32 [main] INFO  com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Starting...
2026-05-29 09:58:35 [main] INFO  com.zaxxer.hikari.pool.HikariPool - HikariPool-1 - Added connection conn0: url=jdbc:h2:file:/app/data/ai-agent-db user=SA
2026-05-29 09:58:35 [main] INFO  com.zaxxer.hikari.HikariDataSource - HikariPool-1 - Start completed.
2026-05-29 09:58:35 [main] WARN  org.hibernate.orm.deprecation - HHH90000025: H2Dialect does not need to be specified explicitly using 'hibernate.dialect' (remove the property setting and it will be selected by default)
2026-05-29 09:58:35 [main] INFO  o.hibernate.orm.connections.pooling - HHH10001005: Database info:
Database JDBC URL [Connecting through datasource 'HikariDataSource (HikariPool-1)']
Database driver: undefined/unknown
Database version: 2.3.232
Autocommit mode: undefined/unknown
Isolation level: undefined/unknown
Minimum pool size: undefined/unknown
Maximum pool size: undefined/unknown
2026-05-29 09:58:41 [main] INFO  o.h.e.t.j.p.i.JtaPlatformInitiator - HHH000489: No JTA platform available (set 'hibernate.transaction.jta.platform' to enable JTA platform integration)
2026-05-29 09:58:41 [main] INFO  o.s.o.j.LocalContainerEntityManagerFactoryBean - Initialized JPA EntityManagerFactory for persistence unit 'default'
2026-05-29 09:58:44 [main] INFO  c.y.l.config.EmbeddingModelConfig - 初始化向量存储（In-Memory）
2026-05-29 09:58:44 [main] INFO  c.y.l.config.EmbeddingModelConfig - 初始化本地 Embedding 模型 (all-minilm-l6-v2)

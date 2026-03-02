# IA-AGGREGATOR Backend Architecture

## Section 1: Maven Multi-Module Structure

### 1.1 Overview

The project follows a **Modular Monolith** architecture using Clean Architecture layers as Maven modules. Each Maven module corresponds to a Clean Architecture ring, and within each module, the 8 business domains (auth, billing, chat, ai-gateway, partners, content, teams, analytics) are organized as Java packages.

### 1.2 Module Dependency Graph

```
ia-aggregator-presentation
       |
       v
ia-aggregator-application
       |
       v
ia-aggregator-domain
       ^
       |
ia-aggregator-infrastructure (implements domain interfaces)
       |
       v
ia-aggregator-common (shared by all)
```

**Dependency Rule**: Dependencies point inward. `domain` depends on nothing (except `common`). `application` depends on `domain`. `presentation` depends on `application`. `infrastructure` depends on `domain` (to implement interfaces) and `application` (for port implementations).

### 1.3 Parent POM (`pom.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.5</version>
        <relativePath/>
    </parent>

    <groupId>com.ia.aggregator</groupId>
    <artifactId>ia-aggregator</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>IA Aggregator Platform</name>
    <description>Multi-model AI aggregator platform</description>

    <modules>
        <module>ia-aggregator-common</module>
        <module>ia-aggregator-domain</module>
        <module>ia-aggregator-application</module>
        <module>ia-aggregator-infrastructure</module>
        <module>ia-aggregator-presentation</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Spring -->
        <spring-boot.version>3.3.5</spring-boot.version>
        <spring-cloud.version>2023.0.3</spring-cloud.version>

        <!-- Database -->
        <postgresql.version>42.7.4</postgresql.version>
        <flyway.version>10.20.1</flyway.version>

        <!-- Security -->
        <jjwt.version>0.12.6</jjwt.version>

        <!-- Resilience -->
        <resilience4j.version>2.2.0</resilience4j.version>

        <!-- Mapping -->
        <mapstruct.version>1.6.3</mapstruct.version>
        <lombok.version>1.18.34</lombok.version>

        <!-- API Documentation -->
        <springdoc.version>2.6.0</springdoc.version>

        <!-- Caching -->
        <redisson.version>3.36.0</redisson.version>

        <!-- Testing -->
        <testcontainers.version>1.20.3</testcontainers.version>
        <archunit.version>1.3.0</archunit.version>

        <!-- Observability -->
        <micrometer.version>1.13.6</micrometer.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Internal Modules -->
            <dependency>
                <groupId>com.ia.aggregator</groupId>
                <artifactId>ia-aggregator-common</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.ia.aggregator</groupId>
                <artifactId>ia-aggregator-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.ia.aggregator</groupId>
                <artifactId>ia-aggregator-application</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.ia.aggregator</groupId>
                <artifactId>ia-aggregator-infrastructure</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.ia.aggregator</groupId>
                <artifactId>ia-aggregator-presentation</artifactId>
                <version>${project.version}</version>
            </dependency>

            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- JWT -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>

            <!-- Resilience4j -->
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-spring-boot3</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-circuitbreaker</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-ratelimiter</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>
            <dependency>
                <groupId>io.github.resilience4j</groupId>
                <artifactId>resilience4j-retry</artifactId>
                <version>${resilience4j.version}</version>
            </dependency>

            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>

            <!-- SpringDoc OpenAPI -->
            <dependency>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
                <version>${springdoc.version}</version>
            </dependency>

            <!-- Redisson -->
            <dependency>
                <groupId>org.redisson</groupId>
                <artifactId>redisson-spring-boot-starter</artifactId>
                <version>${redisson.version}</version>
            </dependency>

            <!-- TestContainers -->
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- ArchUnit -->
            <dependency>
                <groupId>com.tngtech.archunit</groupId>
                <artifactId>archunit-junit5</artifactId>
                <version>${archunit.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <source>21</source>
                        <target>21</target>
                        <annotationProcessorPaths>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                                <version>${lombok.version}</version>
                            </path>
                            <path>
                                <groupId>org.mapstruct</groupId>
                                <artifactId>mapstruct-processor</artifactId>
                                <version>${mapstruct.version}</version>
                            </path>
                            <path>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok-mapstruct-binding</artifactId>
                                <version>0.2.0</version>
                            </path>
                        </annotationProcessorPaths>
                        <compilerArgs>
                            <arg>--enable-preview</arg>
                        </compilerArgs>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <configuration>
                        <excludes>
                            <exclude>
                                <groupId>org.projectlombok</groupId>
                                <artifactId>lombok</artifactId>
                            </exclude>
                        </excludes>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 1.4 Module POMs

#### `ia-aggregator-common/pom.xml`

```xml
<project>
    <parent>
        <groupId>com.ia.aggregator</groupId>
        <artifactId>ia-aggregator</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>ia-aggregator-common</artifactId>
    <name>IA Aggregator - Common</name>
    <description>Shared utilities, annotations, and cross-cutting concerns</description>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

#### `ia-aggregator-domain/pom.xml`

```xml
<project>
    <parent>
        <groupId>com.ia.aggregator</groupId>
        <artifactId>ia-aggregator</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>ia-aggregator-domain</artifactId>
    <name>IA Aggregator - Domain</name>
    <description>Domain entities, value objects, domain services, repository interfaces</description>

    <dependencies>
        <dependency>
            <groupId>com.ia.aggregator</groupId>
            <artifactId>ia-aggregator-common</artifactId>
        </dependency>
        <!-- Domain has ZERO framework dependencies -->
        <!-- Only Java SE + common module -->
    </dependencies>
</project>
```

#### `ia-aggregator-application/pom.xml`

```xml
<project>
    <parent>
        <groupId>com.ia.aggregator</groupId>
        <artifactId>ia-aggregator</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>ia-aggregator-application</artifactId>
    <name>IA Aggregator - Application</name>
    <description>Use cases, DTOs, port interfaces, mappers</description>

    <dependencies>
        <dependency>
            <groupId>com.ia.aggregator</groupId>
            <artifactId>ia-aggregator-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ia.aggregator</groupId>
            <artifactId>ia-aggregator-common</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
    </dependencies>
</project>
```

#### `ia-aggregator-infrastructure/pom.xml`

```xml
<project>
    <parent>
        <groupId>com.ia.aggregator</groupId>
        <artifactId>ia-aggregator</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>ia-aggregator-infrastructure</artifactId>
    <name>IA Aggregator - Infrastructure</name>
    <description>Repository implementations, external service adapters, configuration</description>

    <dependencies>
        <dependency>
            <groupId>com.ia.aggregator</groupId>
            <artifactId>ia-aggregator-domain</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ia.aggregator</groupId>
            <artifactId>ia-aggregator-application</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ia.aggregator</groupId>
            <artifactId>ia-aggregator-common</artifactId>
        </dependency>

        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>

        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
        </dependency>

        <!-- Resilience4j -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
        </dependency>

        <!-- Redis -->
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>

        <!-- Observability -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
    </dependencies>
</project>
```

#### `ia-aggregator-presentation/pom.xml`

```xml
<project>
    <parent>
        <groupId>com.ia.aggregator</groupId>
        <artifactId>ia-aggregator</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>ia-aggregator-presentation</artifactId>
    <name>IA Aggregator - Presentation</name>
    <description>REST controllers, WebSocket handlers, request/response objects</description>

    <dependencies>
        <dependency>
            <groupId>com.ia.aggregator</groupId>
            <artifactId>ia-aggregator-application</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ia.aggregator</groupId>
            <artifactId>ia-aggregator-infrastructure</artifactId>
        </dependency>
        <dependency>
            <groupId>com.ia.aggregator</groupId>
            <artifactId>ia-aggregator-common</artifactId>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        </dependency>

        <!-- Spring Boot Main Application -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 1.5 Complete Package Structure

```
ia-aggregator/
├── pom.xml (parent)
│
├── ia-aggregator-common/
│   └── src/main/java/com/ia/aggregator/common/
│       ├── annotation/
│       │   ├── AuditLog.java
│       │   ├── RateLimit.java
│       │   └── TenantAware.java
│       ├── constants/
│       │   ├── AppConstants.java
│       │   ├── CacheConstants.java
│       │   └── SecurityConstants.java
│       ├── enums/
│       │   ├── Currency.java
│       │   ├── Language.java
│       │   └── Status.java
│       ├── exception/
│       │   ├── BaseException.java
│       │   ├── BusinessException.java
│       │   ├── ErrorCode.java
│       │   └── TechnicalException.java
│       ├── result/
│       │   ├── Result.java
│       │   ├── PageResult.java
│       │   └── ErrorResult.java
│       ├── util/
│       │   ├── DateTimeUtils.java
│       │   ├── JsonUtils.java
│       │   ├── MaskUtils.java
│       │   ├── MoneyUtils.java
│       │   └── ValidationUtils.java
│       └── vo/
│           ├── Money.java
│           ├── Email.java
│           ├── PageRequest.java
│           └── DateRange.java
│
├── ia-aggregator-domain/
│   └── src/main/java/com/ia/aggregator/domain/
│       ├── auth/
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   ├── Role.java
│       │   │   ├── Permission.java
│       │   │   └── RefreshToken.java
│       │   ├── vo/
│       │   │   ├── UserId.java
│       │   │   ├── HashedPassword.java
│       │   │   ├── TokenPair.java
│       │   │   └── OAuthProvider.java
│       │   ├── event/
│       │   │   ├── UserRegisteredEvent.java
│       │   │   ├── UserLoggedInEvent.java
│       │   │   └── PasswordChangedEvent.java
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   ├── RoleRepository.java
│       │   │   └── RefreshTokenRepository.java
│       │   ├── service/
│       │   │   ├── PasswordEncoder.java (interface)
│       │   │   └── AuthDomainService.java
│       │   └── specification/
│       │       └── UserSpecification.java
│       │
│       ├── billing/
│       │   ├── entity/
│       │   │   ├── CreditAccount.java
│       │   │   ├── CreditTransaction.java
│       │   │   ├── Plan.java
│       │   │   ├── Subscription.java
│       │   │   ├── Invoice.java
│       │   │   └── PaymentMethod.java
│       │   ├── vo/
│       │   │   ├── Credits.java
│       │   │   ├── CreditCost.java
│       │   │   ├── PlanTier.java
│       │   │   ├── BillingCycle.java
│       │   │   └── InvoiceStatus.java
│       │   ├── event/
│       │   │   ├── CreditsConsumedEvent.java
│       │   │   ├── CreditsPurchasedEvent.java
│       │   │   ├── SubscriptionActivatedEvent.java
│       │   │   ├── SubscriptionCancelledEvent.java
│       │   │   └── PaymentFailedEvent.java
│       │   ├── repository/
│       │   │   ├── CreditAccountRepository.java
│       │   │   ├── CreditTransactionRepository.java
│       │   │   ├── PlanRepository.java
│       │   │   ├── SubscriptionRepository.java
│       │   │   └── InvoiceRepository.java
│       │   ├── service/
│       │   │   ├── CreditCalculationService.java
│       │   │   └── BillingDomainService.java
│       │   └── specification/
│       │       └── TransactionSpecification.java
│       │
│       ├── chat/
│       │   ├── entity/
│       │   │   ├── Conversation.java
│       │   │   ├── Message.java
│       │   │   └── Attachment.java
│       │   ├── vo/
│       │   │   ├── ConversationId.java
│       │   │   ├── MessageContent.java
│       │   │   ├── MessageRole.java
│       │   │   ├── TokenUsage.java
│       │   │   └── StreamChunk.java
│       │   ├── event/
│       │   │   ├── MessageSentEvent.java
│       │   │   ├── ConversationCreatedEvent.java
│       │   │   └── ConversationArchivedEvent.java
│       │   ├── repository/
│       │   │   ├── ConversationRepository.java
│       │   │   └── MessageRepository.java
│       │   └── service/
│       │       └── ChatDomainService.java
│       │
│       ├── aigateway/
│       │   ├── entity/
│       │   │   ├── AiModel.java
│       │   │   ├── ModelProvider.java
│       │   │   ├── PromptTemplate.java
│       │   │   └── ModelUsageRecord.java
│       │   ├── vo/
│       │   │   ├── ModelId.java
│       │   │   ├── ModelCapability.java
│       │   │   ├── PromptConfig.java
│       │   │   ├── CompletionRequest.java
│       │   │   ├── CompletionResponse.java
│       │   │   └── RoutingCriteria.java
│       │   ├── event/
│       │   │   ├── ModelInvokedEvent.java
│       │   │   └── ModelFailoverEvent.java
│       │   ├── repository/
│       │   │   ├── AiModelRepository.java
│       │   │   ├── ModelProviderRepository.java
│       │   │   └── ModelUsageRecordRepository.java
│       │   └── service/
│       │       ├── ModelRoutingService.java
│       │       └── AiGatewayDomainService.java
│       │
│       ├── partners/
│       │   ├── entity/
│       │   │   ├── Partner.java
│       │   │   ├── Coupon.java
│       │   │   ├── Commission.java
│       │   │   ├── Payout.java
│       │   │   └── PartnerTier.java
│       │   ├── vo/
│       │   │   ├── PartnerId.java
│       │   │   ├── CouponCode.java
│       │   │   ├── CommissionRate.java
│       │   │   ├── PayoutStatus.java
│       │   │   └── AffiliateLink.java
│       │   ├── event/
│       │   │   ├── CommissionEarnedEvent.java
│       │   │   ├── PayoutRequestedEvent.java
│       │   │   ├── CouponRedeemedEvent.java
│       │   │   └── PartnerTierUpgradedEvent.java
│       │   ├── repository/
│       │   │   ├── PartnerRepository.java
│       │   │   ├── CouponRepository.java
│       │   │   ├── CommissionRepository.java
│       │   │   └── PayoutRepository.java
│       │   ├── service/
│       │   │   ├── CommissionCalculationService.java
│       │   │   └── PartnerDomainService.java
│       │   └── specification/
│       │       ├── CouponSpecification.java
│       │       └── CommissionSpecification.java
│       │
│       ├── content/
│       │   ├── entity/
│       │   │   ├── Template.java
│       │   │   ├── Category.java
│       │   │   ├── Tag.java
│       │   │   └── FavoritePrompt.java
│       │   ├── vo/
│       │   │   ├── TemplateId.java
│       │   │   ├── TemplateType.java
│       │   │   └── Slug.java
│       │   ├── event/
│       │   │   └── TemplatePublishedEvent.java
│       │   ├── repository/
│       │   │   ├── TemplateRepository.java
│       │   │   ├── CategoryRepository.java
│       │   │   └── FavoritePromptRepository.java
│       │   └── service/
│       │       └── ContentDomainService.java
│       │
│       ├── teams/
│       │   ├── entity/
│       │   │   ├── Team.java
│       │   │   ├── TeamMember.java
│       │   │   ├── TeamInvitation.java
│       │   │   └── TeamCreditPool.java
│       │   ├── vo/
│       │   │   ├── TeamId.java
│       │   │   ├── TeamRole.java
│       │   │   ├── InvitationStatus.java
│       │   │   └── CreditAllocation.java
│       │   ├── event/
│       │   │   ├── MemberAddedEvent.java
│       │   │   ├── MemberRemovedEvent.java
│       │   │   └── CreditPoolDepletedEvent.java
│       │   ├── repository/
│       │   │   ├── TeamRepository.java
│       │   │   ├── TeamMemberRepository.java
│       │   │   └── TeamInvitationRepository.java
│       │   └── service/
│       │       └── TeamDomainService.java
│       │
│       ├── analytics/
│       │   ├── entity/
│       │   │   ├── UsageMetric.java
│       │   │   ├── CostReport.java
│       │   │   └── AuditEntry.java
│       │   ├── vo/
│       │   │   ├── MetricType.java
│       │   │   ├── TimeGranularity.java
│       │   │   ├── ReportPeriod.java
│       │   │   └── AggregatedMetric.java
│       │   ├── event/
│       │   │   └── ReportGeneratedEvent.java
│       │   ├── repository/
│       │   │   ├── UsageMetricRepository.java
│       │   │   ├── CostReportRepository.java
│       │   │   └── AuditEntryRepository.java
│       │   └── service/
│       │       └── AnalyticsDomainService.java
│       │
│       └── shared/
│           ├── entity/
│           │   └── BaseEntity.java
│           ├── event/
│           │   └── DomainEvent.java
│           ├── repository/
│           │   └── BaseRepository.java
│           └── specification/
│               └── BaseSpecification.java
│
├── ia-aggregator-application/
│   └── src/main/java/com/ia/aggregator/application/
│       ├── auth/
│       │   ├── dto/
│       │   │   ├── RegisterUserCommand.java
│       │   │   ├── LoginCommand.java
│       │   │   ├── RefreshTokenCommand.java
│       │   │   ├── UserResponse.java
│       │   │   └── TokenResponse.java
│       │   ├── mapper/
│       │   │   └── AuthMapper.java
│       │   ├── port/
│       │   │   ├── in/
│       │   │   │   ├── RegisterUserUseCase.java
│       │   │   │   ├── LoginUseCase.java
│       │   │   │   ├── RefreshTokenUseCase.java
│       │   │   │   └── OAuthLoginUseCase.java
│       │   │   └── out/
│       │   │       ├── TokenProvider.java
│       │   │       ├── OAuthClient.java
│       │   │       └── UserEventPublisher.java
│       │   └── usecase/
│       │       ├── RegisterUserUseCaseImpl.java
│       │       ├── LoginUseCaseImpl.java
│       │       ├── RefreshTokenUseCaseImpl.java
│       │       └── OAuthLoginUseCaseImpl.java
│       │
│       ├── billing/
│       │   ├── dto/
│       │   │   ├── PurchaseCreditsCommand.java
│       │   │   ├── ConsumeCreditsCommand.java
│       │   │   ├── CreateSubscriptionCommand.java
│       │   │   ├── CreditBalanceResponse.java
│       │   │   └── InvoiceResponse.java
│       │   ├── mapper/
│       │   │   └── BillingMapper.java
│       │   ├── port/
│       │   │   ├── in/
│       │   │   │   ├── PurchaseCreditsUseCase.java
│       │   │   │   ├── ConsumeCreditsUseCase.java
│       │   │   │   ├── GetCreditBalanceUseCase.java
│       │   │   │   └── ManageSubscriptionUseCase.java
│       │   │   └── out/
│       │   │       ├── PaymentGateway.java
│       │   │       ├── InvoiceGenerator.java
│       │   │       └── BillingEventPublisher.java
│       │   └── usecase/
│       │       ├── PurchaseCreditsUseCaseImpl.java
│       │       ├── ConsumeCreditsUseCaseImpl.java
│       │       ├── GetCreditBalanceUseCaseImpl.java
│       │       └── ManageSubscriptionUseCaseImpl.java
│       │
│       ├── chat/
│       │   ├── dto/
│       │   │   ├── SendMessageCommand.java
│       │   │   ├── CreateConversationCommand.java
│       │   │   ├── ConversationResponse.java
│       │   │   └── MessageResponse.java
│       │   ├── mapper/
│       │   │   └── ChatMapper.java
│       │   ├── port/
│       │   │   ├── in/
│       │   │   │   ├── SendMessageUseCase.java
│       │   │   │   ├── CreateConversationUseCase.java
│       │   │   │   ├── GetConversationHistoryUseCase.java
│       │   │   │   └── StreamResponseUseCase.java
│       │   │   └── out/
│       │   │       ├── AiModelClient.java
│       │   │       ├── StreamEmitter.java
│       │   │       └── ChatEventPublisher.java
│       │   └── usecase/
│       │       ├── SendMessageUseCaseImpl.java
│       │       ├── CreateConversationUseCaseImpl.java
│       │       ├── GetConversationHistoryUseCaseImpl.java
│       │       └── StreamResponseUseCaseImpl.java
│       │
│       ├── aigateway/
│       │   ├── dto/
│       │   │   ├── RouteRequestCommand.java
│       │   │   ├── ModelListResponse.java
│       │   │   └── ModelUsageResponse.java
│       │   ├── mapper/
│       │   │   └── AiGatewayMapper.java
│       │   ├── port/
│       │   │   ├── in/
│       │   │   │   ├── RouteToModelUseCase.java
│       │   │   │   ├── ListAvailableModelsUseCase.java
│       │   │   │   └── GetModelUsageUseCase.java
│       │   │   └── out/
│       │   │       ├── AiProviderClient.java
│       │   │       ├── SemanticCachePort.java
│       │   │       └── ModelRegistryPort.java
│       │   └── usecase/
│       │       ├── RouteToModelUseCaseImpl.java
│       │       ├── ListAvailableModelsUseCaseImpl.java
│       │       └── GetModelUsageUseCaseImpl.java
│       │
│       ├── partners/
│       │   ├── dto/
│       │   │   ├── RegisterPartnerCommand.java
│       │   │   ├── CreateCouponCommand.java
│       │   │   ├── RedeemCouponCommand.java
│       │   │   ├── RequestPayoutCommand.java
│       │   │   ├── PartnerDashboardResponse.java
│       │   │   └── CommissionReportResponse.java
│       │   ├── mapper/
│       │   │   └── PartnerMapper.java
│       │   ├── port/
│       │   │   ├── in/
│       │   │   │   ├── RegisterPartnerUseCase.java
│       │   │   │   ├── ManageCouponUseCase.java
│       │   │   │   ├── RedeemCouponUseCase.java
│       │   │   │   ├── CalculateCommissionUseCase.java
│       │   │   │   └── RequestPayoutUseCase.java
│       │   │   └── out/
│       │   │       ├── PayoutGateway.java
│       │   │       └── PartnerEventPublisher.java
│       │   └── usecase/
│       │       ├── RegisterPartnerUseCaseImpl.java
│       │       ├── ManageCouponUseCaseImpl.java
│       │       ├── RedeemCouponUseCaseImpl.java
│       │       ├── CalculateCommissionUseCaseImpl.java
│       │       └── RequestPayoutUseCaseImpl.java
│       │
│       ├── content/
│       │   ├── dto/
│       │   │   ├── CreateTemplateCommand.java
│       │   │   ├── TemplateResponse.java
│       │   │   └── CategoryResponse.java
│       │   ├── mapper/
│       │   │   └── ContentMapper.java
│       │   ├── port/
│       │   │   ├── in/
│       │   │   │   ├── ManageTemplateUseCase.java
│       │   │   │   ├── SearchTemplatesUseCase.java
│       │   │   │   └── ManageFavoritesUseCase.java
│       │   │   └── out/
│       │   │       └── ContentEventPublisher.java
│       │   └── usecase/
│       │       ├── ManageTemplateUseCaseImpl.java
│       │       ├── SearchTemplatesUseCaseImpl.java
│       │       └── ManageFavoritesUseCaseImpl.java
│       │
│       ├── teams/
│       │   ├── dto/
│       │   │   ├── CreateTeamCommand.java
│       │   │   ├── InviteMemberCommand.java
│       │   │   ├── AllocateCreditsCommand.java
│       │   │   ├── TeamResponse.java
│       │   │   └── TeamMemberResponse.java
│       │   ├── mapper/
│       │   │   └── TeamMapper.java
│       │   ├── port/
│       │   │   ├── in/
│       │   │   │   ├── CreateTeamUseCase.java
│       │   │   │   ├── ManageTeamMembersUseCase.java
│       │   │   │   └── AllocateTeamCreditsUseCase.java
│       │   │   └── out/
│       │   │       ├── TeamNotificationPort.java
│       │   │       └── TeamEventPublisher.java
│       │   └── usecase/
│       │       ├── CreateTeamUseCaseImpl.java
│       │       ├── ManageTeamMembersUseCaseImpl.java
│       │       └── AllocateTeamCreditsUseCaseImpl.java
│       │
│       └── analytics/
│           ├── dto/
│           │   ├── GenerateReportCommand.java
│           │   ├── UsageDashboardResponse.java
│           │   ├── CostAnalysisResponse.java
│           │   └── AuditLogResponse.java
│           ├── mapper/
│           │   └── AnalyticsMapper.java
│           ├── port/
│           │   ├── in/
│           │   │   ├── GetUsageDashboardUseCase.java
│           │   │   ├── GenerateCostReportUseCase.java
│           │   │   └── QueryAuditLogUseCase.java
│           │   └── out/
│           │       ├── MetricsCollector.java
│           │       └── ReportExporter.java
│           └── usecase/
│               ├── GetUsageDashboardUseCaseImpl.java
│               ├── GenerateCostReportUseCaseImpl.java
│               └── QueryAuditLogUseCaseImpl.java
│
├── ia-aggregator-infrastructure/
│   └── src/main/java/com/ia/aggregator/infrastructure/
│       ├── auth/
│       │   ├── adapter/
│       │   │   ├── GoogleOAuthAdapter.java
│       │   │   └── GitHubOAuthAdapter.java
│       │   ├── persistence/
│       │   │   ├── entity/
│       │   │   │   ├── UserJpaEntity.java
│       │   │   │   ├── RoleJpaEntity.java
│       │   │   │   └── RefreshTokenJpaEntity.java
│       │   │   ├── mapper/
│       │   │   │   └── UserPersistenceMapper.java
│       │   │   ├── repository/
│       │   │   │   ├── UserJpaRepository.java
│       │   │   │   └── RefreshTokenJpaRepository.java
│       │   │   └── UserRepositoryImpl.java
│       │   └── security/
│       │       ├── JwtTokenProvider.java
│       │       ├── BcryptPasswordEncoderAdapter.java
│       │       └── UserDetailsServiceImpl.java
│       │
│       ├── billing/
│       │   ├── adapter/
│       │   │   ├── StripePaymentAdapter.java
│       │   │   ├── AsaasPaymentAdapter.java
│       │   │   └── InvoiceGeneratorAdapter.java
│       │   └── persistence/
│       │       ├── entity/
│       │       │   ├── CreditAccountJpaEntity.java
│       │       │   ├── CreditTransactionJpaEntity.java
│       │       │   ├── PlanJpaEntity.java
│       │       │   └── SubscriptionJpaEntity.java
│       │       ├── mapper/
│       │       │   └── BillingPersistenceMapper.java
│       │       ├── repository/
│       │       │   ├── CreditAccountJpaRepository.java
│       │       │   └── SubscriptionJpaRepository.java
│       │       └── CreditAccountRepositoryImpl.java
│       │
│       ├── chat/
│       │   ├── adapter/
│       │   │   └── SseStreamEmitterAdapter.java
│       │   └── persistence/
│       │       ├── entity/
│       │       │   ├── ConversationJpaEntity.java
│       │       │   └── MessageJpaEntity.java
│       │       ├── mapper/
│       │       │   └── ChatPersistenceMapper.java
│       │       ├── repository/
│       │       │   ├── ConversationJpaRepository.java
│       │       │   └── MessageJpaRepository.java
│       │       └── ConversationRepositoryImpl.java
│       │
│       ├── aigateway/
│       │   ├── adapter/
│       │   │   ├── OpenRouterAdapter.java
│       │   │   ├── OpenAiDirectAdapter.java
│       │   │   ├── AnthropicDirectAdapter.java
│       │   │   └── SemanticCacheAdapter.java
│       │   ├── acl/
│       │   │   ├── OpenRouterACL.java
│       │   │   ├── OpenAiACL.java
│       │   │   └── AnthropicACL.java
│       │   └── persistence/
│       │       ├── entity/
│       │       │   ├── AiModelJpaEntity.java
│       │       │   └── ModelUsageRecordJpaEntity.java
│       │       ├── repository/
│       │       │   └── AiModelJpaRepository.java
│       │       └── AiModelRepositoryImpl.java
│       │
│       ├── partners/
│       │   ├── adapter/
│       │   │   └── PartnerPayoutAdapter.java
│       │   └── persistence/
│       │       ├── entity/
│       │       │   ├── PartnerJpaEntity.java
│       │       │   ├── CouponJpaEntity.java
│       │       │   └── CommissionJpaEntity.java
│       │       ├── mapper/
│       │       │   └── PartnerPersistenceMapper.java
│       │       ├── repository/
│       │       │   ├── PartnerJpaRepository.java
│       │       │   └── CouponJpaRepository.java
│       │       └── PartnerRepositoryImpl.java
│       │
│       ├── content/
│       │   └── persistence/
│       │       ├── entity/
│       │       │   ├── TemplateJpaEntity.java
│       │       │   └── CategoryJpaEntity.java
│       │       ├── repository/
│       │       │   └── TemplateJpaRepository.java
│       │       └── TemplateRepositoryImpl.java
│       │
│       ├── teams/
│       │   ├── adapter/
│       │   │   └── TeamNotificationAdapter.java
│       │   └── persistence/
│       │       ├── entity/
│       │       │   ├── TeamJpaEntity.java
│       │       │   └── TeamMemberJpaEntity.java
│       │       ├── repository/
│       │       │   └── TeamJpaRepository.java
│       │       └── TeamRepositoryImpl.java
│       │
│       ├── analytics/
│       │   ├── adapter/
│       │   │   └── CsvReportExporterAdapter.java
│       │   └── persistence/
│       │       ├── entity/
│       │       │   ├── UsageMetricJpaEntity.java
│       │       │   └── AuditEntryJpaEntity.java
│       │       ├── repository/
│       │       │   ├── UsageMetricJpaRepository.java
│       │       │   └── AuditEntryJpaRepository.java
│       │       └── UsageMetricRepositoryImpl.java
│       │
│       ├── config/
│       │   ├── SecurityConfig.java
│       │   ├── JpaConfig.java
│       │   ├── RedisConfig.java
│       │   ├── WebSocketConfig.java
│       │   ├── WebClientConfig.java
│       │   ├── Resilience4jConfig.java
│       │   ├── FlywayConfig.java
│       │   ├── AsyncConfig.java
│       │   ├── CorsConfig.java
│       │   └── OpenApiConfig.java
│       │
│       ├── event/
│       │   ├── SpringEventPublisher.java
│       │   └── OutboxEventStore.java
│       │
│       └── multitenancy/
│           ├── TenantContext.java
│           ├── TenantFilter.java
│           └── TenantAwareInterceptor.java
│
└── ia-aggregator-presentation/
    └── src/main/java/com/ia/aggregator/presentation/
        ├── IaAggregatorApplication.java (Spring Boot main class)
        │
        ├── auth/
        │   ├── controller/
        │   │   └── AuthController.java
        │   └── request/
        │       ├── RegisterRequest.java
        │       ├── LoginRequest.java
        │       └── RefreshTokenRequest.java
        │
        ├── billing/
        │   ├── controller/
        │   │   ├── CreditController.java
        │   │   ├── SubscriptionController.java
        │   │   └── WebhookController.java
        │   └── request/
        │       ├── PurchaseCreditsRequest.java
        │       └── CreateSubscriptionRequest.java
        │
        ├── chat/
        │   ├── controller/
        │   │   └── ChatController.java
        │   ├── handler/
        │   │   └── ChatWebSocketHandler.java
        │   └── request/
        │       ├── SendMessageRequest.java
        │       └── CreateConversationRequest.java
        │
        ├── aigateway/
        │   ├── controller/
        │   │   └── AiModelController.java
        │   └── request/
        │       └── CompletionRequest.java
        │
        ├── partners/
        │   ├── controller/
        │   │   ├── PartnerController.java
        │   │   └── CouponController.java
        │   └── request/
        │       ├── RegisterPartnerRequest.java
        │       ├── CreateCouponRequest.java
        │       └── RequestPayoutRequest.java
        │
        ├── content/
        │   ├── controller/
        │   │   └── TemplateController.java
        │   └── request/
        │       └── CreateTemplateRequest.java
        │
        ├── teams/
        │   ├── controller/
        │   │   └── TeamController.java
        │   └── request/
        │       ├── CreateTeamRequest.java
        │       └── InviteMemberRequest.java
        │
        ├── analytics/
        │   ├── controller/
        │   │   ├── DashboardController.java
        │   │   └── AuditController.java
        │   └── request/
        │       └── GenerateReportRequest.java
        │
        └── shared/
            ├── advice/
            │   └── GlobalExceptionHandler.java
            ├── filter/
            │   ├── JwtAuthenticationFilter.java
            │   ├── RateLimitFilter.java
            │   └── RequestLoggingFilter.java
            ├── response/
            │   ├── ApiResponse.java
            │   ├── ApiErrorResponse.java
            │   └── PagedResponse.java
            └── interceptor/
                └── AuditInterceptor.java
```

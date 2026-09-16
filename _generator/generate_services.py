#!/usr/bin/env python3
"""
Stamps out every services/<name>/ microservice from _generator/services.yaml.

This IS the "template microservices pattern" deliverable: add or change an entry in
services.yaml, re-run this script, and the pom.xml, Dockerfile, application.yml,
Application.java, TemporalConfig.java (worker-registering variant for workflow/activity
services, client-only variant for gateway/simulator), and the k8s manifests for that
service are all regenerated consistently. It does NOT touch the hand-written business
logic files (workflow impls, activity impls, controllers, etc.) — those are copied in
separately by copy_impls.py, once, from the monolith.

Usage: python3 generate_services.py
"""
import pathlib
import yaml

GEN_DIR = pathlib.Path(__file__).resolve().parent
ROOT = GEN_DIR.parent
SERVICES_DIR = ROOT / "services"
BASE_PKG = "com.bank.payment.orchestration"
BASE_PKG_PATH = BASE_PKG.replace(".", "/")

TASK_QUEUE_PREFIX = "com.bank.payment.orchestration.workflow.TaskQueues."

EXTRA_DEP_XML = {
    "spring-kafka": '<dependency><groupId>org.springframework.kafka</groupId><artifactId>spring-kafka</artifactId></dependency>',
    "spring-boot-starter-amqp": '<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-amqp</artifactId></dependency>',
    "spring-boot-starter-jms": '<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-jms</artifactId></dependency>',
    "activemq-client": '<dependency><groupId>org.apache.activemq</groupId><artifactId>activemq-client</artifactId><version>6.1.2</version></dependency>',
    "springdoc-openapi-starter-webmvc-ui": '<dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId></dependency>',
}


def pascal_case(kebab: str) -> str:
    return "".join(part.capitalize() for part in kebab.split("-"))


def load_manifest():
    data = yaml.safe_load((GEN_DIR / "services.yaml").read_text())
    return data["services"]


def render_pom(svc: dict) -> str:
    extra = "\n        ".join(EXTRA_DEP_XML[d] for d in svc.get("extra_deps", []))
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.bank.payment</groupId>
        <artifactId>payment-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>{svc['name']}</artifactId>
    <packaging>jar</packaging>
    <name>{svc['name']}</name>
    <description>{svc['description']}</description>

    <dependencies>
        <dependency>
            <groupId>com.bank.payment</groupId>
            <artifactId>payment-common</artifactId>
        </dependency>
        <!-- io.temporal:temporal-sdk is inherited transitively via payment-common;
             not redeclared here to avoid two places to bump the version. -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        {extra}
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.temporal</groupId>
            <artifactId>temporal-testing</artifactId>
            <scope>test</scope>
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
"""


def render_dockerfile(svc: dict) -> str:
    name = svc["name"]
    return f"""# Build from the payment-platform REPO ROOT, not this directory:
#   docker build -f services/{name}/Dockerfile -t {name}:local .
# Needs the whole reactor in context so payment-common resolves as a local Maven
# module. For a real CI pipeline, publish payment-common to your internal Maven repo
# (Nexus/Artifactory/GitHub Packages) as its own release step first, then this build
# only needs services/{name}/ as context and a plain `mvn -pl {name} package` —
# see README "Building payment-common independently."

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY payment-common payment-common
COPY services/{name} services/{name}
RUN mvn -q -pl services/{name} -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
USER appuser
WORKDIR /app
COPY --from=build /workspace/services/{name}/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
"""


def render_application_yml(svc: dict) -> str:
    name = svc["name"]
    svc_type = svc["type"]
    lines = [
        "spring:",
        f"  application:",
        f"    name: {name}",
        "  threads:",
        "    virtual:",
        "      enabled: true",
    ]

    autoexclude = []
    needs_data_block = svc["uses_mongo"] or svc["uses_redis"]
    if needs_data_block:
        lines.append("  data:")
    if svc["uses_mongo"]:
        lines += [
            "    mongodb:",
            "      uri: ${MONGO_URI:mongodb://localhost:27017/payments}",
        ]
    else:
        autoexclude += [
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
            "org.springframework.boot.autoconfigure.mongo.MongoHealthContributorAutoConfiguration",
        ]

    if svc["uses_redis"]:
        lines += [
            "    redis:",
            "      host: ${REDIS_HOST:localhost}",
            "      port: ${REDIS_PORT:6379}",
        ]
    else:
        autoexclude += [
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        ]

    if "spring-kafka" in svc.get("extra_deps", []):
        lines += [
            "  kafka:",
            "    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}",
            "    consumer:",
            "      group-id: " + name,
            "      auto-offset-reset: earliest",
            "      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer",
            "      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer",
            "      properties:",
            "        spring.json.trusted.packages: com.bank.payment.orchestration.domain",
            "    producer:",
            "      key-serializer: org.apache.kafka.common.serialization.StringSerializer",
            "      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer",
        ]

    if "spring-boot-starter-amqp" in svc.get("extra_deps", []):
        lines += [
            "  rabbitmq:",
            "    host: ${RABBITMQ_HOST:localhost}",
            "    port: ${RABBITMQ_PORT:5672}",
            "    username: ${RABBITMQ_USER:guest}",
            "    password: ${RABBITMQ_PASS:guest}",
        ]

    if autoexclude:
        lines += ["  autoconfigure:", "    exclude:"]
        for cls in autoexclude:
            lines.append(f"      - {cls}")

    lines += [
        "",
        "server:",
        "  port: 8080",
        "",
        "management:",
        "  endpoints:",
        "    web:",
        "      exposure:",
        "        include: health,info,metrics,prometheus",
        "  endpoint:",
        "    health:",
        "      probes:",
        "        enabled: true",
        "  health:",
        "    livenessstate:",
        "      enabled: true",
        "    readinessstate:",
        "      enabled: true",
        "",
        "payment-orchestration:",
    ]

    if svc_type in ("workflow", "activity"):
        tq_const = svc["task_queue"]
        lines += [
            "  temporal:",
            "    target: ${TEMPORAL_TARGET:127.0.0.1:7233}",
            "    namespace: ${TEMPORAL_NAMESPACE:payments}",
            f"    task-queue: {tq_const}_TQ",
        ]
    else:
        lines += [
            "  temporal:",
            "    target: ${TEMPORAL_TARGET:127.0.0.1:7233}",
            "    namespace: ${TEMPORAL_NAMESPACE:payments}",
        ]

    if svc_type in ("gateway", "simulator"):
        lines += [
            "  activemq:",
            "    broker-url: ${ACTIVEMQ_URL:tcp://localhost:61616}",
            "    username: ${ACTIVEMQ_USER:admin}",
            "    password: ${ACTIVEMQ_PASS:admin}",
            "    inbound-queue: payments.inbound.mt",
            "  rabbit:",
            "    inbound-queue: payments.inbound.mx",
            "  kafka-topics:",
            "    normalized-payments: payments.normalized",
            "    partner-request-prefix: partner.",
            "    partner-response-prefix: partner.",
        ]

    if svc_type == "simulator":
        lines += [
            "  simulator:",
            "    enabled: ${SIMULATOR_ENABLED:true}",
            "    default-mode: ${SIMULATOR_DEFAULT_MODE:MIXED}",
            "    short-delay-min-seconds: 0",
            "    short-delay-max-seconds: 2",
            "    mixed:",
            "      reject-probability: 0.3",
            "      delay-probability: 0.4",
            "      long-delay-min-seconds: 5",
            "      long-delay-max-seconds: 45",
        ]

    if svc_type in ("workflow", "activity"):
        lines += [
            "  repair:",
            "    max-attempts: 3",
            "    sla-timeout-minutes: 240",
            "  partner:",
            "    response-sla-seconds: 30",
            "  correlation:",
            "    pending-ttl-seconds: 900",
        ]

    if svc.get("http"):
        lines += [
            "",
            "springdoc:",
            "  swagger-ui:",
            "    path: /swagger-ui.html",
            "  api-docs:",
            "    path: /v3/api-docs",
        ]

    return "\n".join(lines) + "\n"


def render_application_java(svc: dict) -> str:
    class_name = pascal_case(svc["name"]) + "Application"
    return f"""package {BASE_PKG};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class {class_name} {{
    public static void main(String[] args) {{
        SpringApplication.run({class_name}.class, args);
    }}
}}
"""


def render_worker_config_java(svc: dict, activity_impl_class: str = None, workflow_impl_class: str = None) -> str:
    """TemporalConfig for a workflow-service or activity-service: registers exactly one
    impl type/bean against exactly one task queue. Client-only services (gateway/
    simulator) use render_client_config_java instead — see that function."""
    tq_const = svc["task_queue"]
    if workflow_impl_class:
        register_line = f"worker.registerWorkflowImplementationTypes({workflow_impl_class}.class);"
        activity_param = ""
        activity_register = ""
        impl_import = f"import {BASE_PKG}.workflow.impl.{workflow_impl_class};"
    else:
        register_line = ""
        activity_param = f",\n            {activity_impl_class} {activity_impl_class[0].lower() + activity_impl_class[1:]}"
        bean_name = activity_impl_class[0].lower() + activity_impl_class[1:]
        activity_register = f"worker.registerActivitiesImplementations({bean_name});"
        impl_import = f"import {BASE_PKG}.activities.{activity_impl_class};"

    return f"""package {BASE_PKG}.config;

{impl_import}
import {BASE_PKG}.workflow.TaskQueues;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This service registers a SINGLE workflow or activity implementation against a SINGLE
 * task queue (TaskQueues.{tq_const}) — the whole point of the decomposition. Every
 * cross-service call (child-workflow stub, activity stub) elsewhere in the codebase
 * targets this queue explicitly via ChildWorkflowOptions/ActivityOptions.setTaskQueue(),
 * never relying on a shared default.
 */
@Configuration
public class TemporalConfig {{

    @Bean
    public WorkflowServiceStubs workflowServiceStubs(
            @Value("${{payment-orchestration.temporal.target}}") String target) {{
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
    }}

    @Bean
    public WorkflowClient workflowClient(
            WorkflowServiceStubs serviceStubs,
            @Value("${{payment-orchestration.temporal.namespace}}") String namespace) {{
        return WorkflowClient.newInstance(serviceStubs,
                WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    }}

    @Bean
    public WorkerFactory workerFactory(WorkflowClient client{activity_param}) {{
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TaskQueues.{tq_const});
        {register_line}
        {activity_register}
        return factory;
    }}

    @Bean
    public SmartLifecycle temporalWorkerLifecycle(WorkerFactory workerFactory) {{
        return new SmartLifecycle() {{
            private volatile boolean running = false;

            @Override
            public void start() {{
                workerFactory.start();
                running = true;
            }}

            @Override
            public void stop() {{
                workerFactory.shutdown();
                running = false;
            }}

            @Override
            public boolean isRunning() {{
                return running;
            }}

            @Override
            public int getPhase() {{
                return Integer.MAX_VALUE - 1;
            }}
        }};
    }}
}}
"""


def render_client_config_java() -> str:
    """WorkflowClient-only config for gateway/simulator services — they start/signal
    workflows but never host a Worker themselves."""
    return f"""package {BASE_PKG}.config;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Client-only Temporal wiring — this service starts/signals workflows but hosts no Worker. */
@Configuration
public class TemporalClientConfig {{

    @Bean
    public WorkflowServiceStubs workflowServiceStubs(
            @Value("${{payment-orchestration.temporal.target}}") String target) {{
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().setTarget(target).build());
    }}

    @Bean
    public WorkflowClient workflowClient(
            WorkflowServiceStubs serviceStubs,
            @Value("${{payment-orchestration.temporal.namespace}}") String namespace) {{
        return WorkflowClient.newInstance(serviceStubs,
                WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
    }}
}}
"""


def render_k8s_deployment(svc: dict) -> str:
    name = svc["name"]
    res = svc["resources"]
    env_common = """        - name: TEMPORAL_TARGET
          valueFrom:
            configMapKeyRef: {name: payment-platform-config, key: temporal-target}
        - name: TEMPORAL_NAMESPACE
          valueFrom:
            configMapKeyRef: {name: payment-platform-config, key: temporal-namespace}"""
    extra_env = ""
    if svc["uses_mongo"]:
        extra_env += """
        - name: MONGO_URI
          valueFrom:
            secretKeyRef: {name: payment-platform-secrets, key: mongo-uri}"""
    if svc["uses_redis"]:
        extra_env += """
        - name: REDIS_HOST
          valueFrom:
            configMapKeyRef: {name: payment-platform-config, key: redis-host}
        - name: REDIS_PORT
          valueFrom:
            configMapKeyRef: {name: payment-platform-config, key: redis-port}"""
    if "spring-kafka" in svc.get("extra_deps", []):
        extra_env += """
        - name: KAFKA_BOOTSTRAP
          valueFrom:
            configMapKeyRef: {name: payment-platform-config, key: kafka-bootstrap}"""
    if svc["type"] in ("gateway", "simulator"):
        extra_env += """
        - name: RABBITMQ_HOST
          valueFrom:
            configMapKeyRef: {name: payment-platform-config, key: rabbitmq-host}
        - name: ACTIVEMQ_URL
          valueFrom:
            configMapKeyRef: {name: payment-platform-config, key: activemq-url}"""

    return f"""apiVersion: apps/v1
kind: Deployment
metadata:
  name: {name}
  labels:
    app: {name}
    tier: {svc['type']}
spec:
  replicas: {svc['replicas']}
  selector:
    matchLabels:
      app: {name}
  template:
    metadata:
      labels:
        app: {name}
        tier: {svc['type']}
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      containers:
      - name: {name}
        # Plain placeholder image name+tag — overridden per-environment via the root
        # kustomization.yaml's `images:` transformer (kustomize edit set image ...),
        # not Helm. Do not hand-edit this line per environment; edit the kustomization.
        image: "{name}:latest"
        ports:
        - containerPort: 8080
          name: http
        env:
{env_common}{extra_env}
        resources:
          requests:
            cpu: "{res['cpu_request']}"
            memory: "{res['mem_request']}"
          limits:
            cpu: "{res['cpu_limit']}"
            memory: "{res['mem_limit']}"
        readinessProbe:
          httpGet: {{path: /actuator/health/readiness, port: 8080}}
          initialDelaySeconds: 15
          periodSeconds: 10
        livenessProbe:
          httpGet: {{path: /actuator/health/liveness, port: 8080}}
          initialDelaySeconds: 30
          periodSeconds: 15
        startupProbe:
          httpGet: {{path: /actuator/health, port: 8080}}
          failureThreshold: 30
          periodSeconds: 5
"""


def render_k8s_service(svc: dict) -> str:
    name = svc["name"]
    return f"""apiVersion: v1
kind: Service
metadata:
  name: {name}
  labels:
    app: {name}
spec:
  selector:
    app: {name}
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  type: ClusterIP
"""


def render_k8s_hpa(svc: dict) -> str:
    name = svc["name"]
    max_replicas = max(svc["replicas"] * 3, 3)
    return f"""apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: {name}
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: {name}
  minReplicas: {svc['replicas']}
  maxReplicas: {max_replicas}
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  # NOTE: CPU is a reasonable default trigger, but the more meaningful signal for a
  # Temporal worker is its OWN task-queue backlog (schedule-to-start latency), not CPU —
  # a worker can be CPU-idle while a queue backs up if concurrency limits are too low.
  # Consider a KEDA ScaledObject on a Temporal/Prometheus queue-depth metric instead of
  # or alongside this HPA once you have that metric wired up.
"""


def render_k8s_kustomization(svc: dict) -> str:
    return f"""apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - deployment.yaml
  - service.yaml
  - hpa.yaml
"""


def generate_service(svc: dict):
    svc_dir = SERVICES_DIR / svc["name"]
    java_dir = svc_dir / "src" / "main" / "java" / BASE_PKG_PATH.replace("/", "/")
    resources_dir = svc_dir / "src" / "main" / "resources"
    k8s_dir = svc_dir / "k8s"
    for d in (java_dir, resources_dir, k8s_dir):
        d.mkdir(parents=True, exist_ok=True)

    (svc_dir / "pom.xml").write_text(render_pom(svc))
    (svc_dir / "Dockerfile").write_text(render_dockerfile(svc))
    (resources_dir / "application.yml").write_text(render_application_yml(svc))
    (java_dir / (pascal_case(svc["name"]) + "Application.java")).write_text(render_application_java(svc))

    config_dir = java_dir / "config"
    config_dir.mkdir(exist_ok=True)
    if svc["type"] in ("gateway", "simulator"):
        (config_dir / "TemporalClientConfig.java").write_text(render_client_config_java())
    # workflow/activity TemporalConfig.java is written by copy_impls.py once it knows
    # the exact impl class name being registered (it calls back into this module's
    # render_worker_config_java).

    (k8s_dir / "deployment.yaml").write_text(render_k8s_deployment(svc))
    (k8s_dir / "service.yaml").write_text(render_k8s_service(svc))
    (k8s_dir / "hpa.yaml").write_text(render_k8s_hpa(svc))
    (k8s_dir / "kustomization.yaml").write_text(render_k8s_kustomization(svc))

    print(f"generated {svc['name']}")


def main():
    services = load_manifest()
    for svc in services:
        generate_service(svc)


if __name__ == "__main__":
    main()

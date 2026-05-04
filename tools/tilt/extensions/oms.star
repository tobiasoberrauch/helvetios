"""
Tilt extension for apps/oms-service/. Active in Phase 3 (US1).

Builds the OMS service via `./gradlew :apps:oms-service:bootBuildImage`,
deploys via the Helm chart in apps/oms-service/helm/, and live-reloads
on changes to src/main/java/.
"""

custom_build(
    'ghcr.io/tobiasoberrauch/swisstms-oms-service',
    './gradlew :apps:oms-service:bootBuildImage --imageName=$EXPECTED_REF',
    deps=['apps/oms-service/src/main', 'apps/oms-service/build.gradle.kts',
          'libs/domain-model/src/main', 'libs/audit-chain/src/main',
          'libs/time-sync/src/main', 'libs/observability/src/main'],
    live_update=[
        sync('apps/oms-service/src/main/resources/', '/workspace/BOOT-INF/classes/'),
    ],
)

k8s_yaml(helm('apps/oms-service/helm', name='oms', namespace='default',
              set=['env.SPRING_PROFILES_ACTIVE=dev']))

k8s_resource(
    workload='oms-oms-service',
    port_forwards=['8080:8080'],
    resource_deps=['postgres'],
    labels=['core'],
)

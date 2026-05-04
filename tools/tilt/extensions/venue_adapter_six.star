"""
Tilt extension for apps/venue-adapter-six/. Active in Phase 3 (US1).
"""

custom_build(
    'ghcr.io/tobiasoberrauch/swisstms-venue-adapter-six',
    './gradlew :apps:venue-adapter-six:bootBuildImage --imageName=$EXPECTED_REF',
    deps=['apps/venue-adapter-six/src/main', 'apps/venue-adapter-six/build.gradle.kts',
          'libs/domain-model/src/main', 'libs/fix-codec/src/main',
          'libs/time-sync/src/main'],
)

k8s_yaml(helm('apps/venue-adapter-six/helm', name='venue-six', namespace='default'))

k8s_resource(
    workload='venue-six-venue-adapter-six',
    port_forwards=['8101:8101'],
    labels=['adapter'],
)

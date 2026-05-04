# Test-Venue-Stub

Minimaler `VenueGatewayPort`-Adapter für den Phase-5-Test
`AddVenueWithoutTouchingDomainTest`. Beweist, dass ein neuer Adapter
allein durch Spring-DI vom OMS gefunden und benutzt wird, ohne dass
Domain-, OMS-, EMS-, Reconciler-, Reporting- oder Surveillance-Code
geändert wurde.

Der Adapter selbst lebt in
`tests/architecture/src/test/java/ch/swisstms/architecture/TestVenueAdapter.java`
(als Test-Fixture, nicht als deployable App).

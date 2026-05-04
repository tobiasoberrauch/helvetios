# T143 — SIX Trade Repository SFTP submission stub.
#
# atmoz/sftp drops anything we upload into /home/swisstms/upload. The reporting-service
# integration tests (DailyReportingBatchTest) bind-mount this dir and assert that the FinfraG
# Art.39 file lands intact + with the correct sha256.
FROM atmoz/sftp:alpine

# Bake the canonical user once so docker-compose/tilt can start without an entrypoint flag.
# Password matches mocks/six-tr-submission/users.conf.
COPY users.conf /etc/sftp/users.conf
EXPOSE 22

FROM openjdk:8
COPY ./target/universal/pea /opt/pea
EXPOSE 9000
CMD /opt/pea/bin/pea \
  -J-Xms128M -J-Xmx1048m \
  -Dconfig.resource=application.conf \
  -Dpidfile.path=/dev/null

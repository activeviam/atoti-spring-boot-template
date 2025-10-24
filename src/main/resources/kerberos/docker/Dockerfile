FROM debian:trixie-slim

EXPOSE 749 88

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get -qq update; apt-get -qq install locales krb5-kdc krb5-admin-server; apt-get -qq clean

RUN locale-gen "en_US.UTF-8"
RUN echo "LC_ALL=\"en_US.UTF-8\"" >> /etc/default/locale

ENV REALM=EXAMPLE.COM
ENV SUPPORTED_ENCRYPTION_TYPES=aes256-cts-hmac-sha1-96:normal
ENV USER_PRINCIPAL=admin
ENV USER_PASSWORD=admin
ENV APP_PRINCIPAL=HTTP/app.example.com

COPY init-script.sh /tmp/
RUN chmod +x /tmp/init-script.sh
ENTRYPOINT ["/tmp/init-script.sh"]

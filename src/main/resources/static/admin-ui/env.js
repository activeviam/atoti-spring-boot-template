var baseUrl = window.location.href.split("/admin/ui")[0];

window.env = {
    jwtServer: {
        url: baseUrl,
        version: "6.1.0-rc"
    },
    contentServerUrl: baseUrl,
    contentServerVersion: "6.1.0-rc",
    activePivotServers: {
        "pivot-spring-boot": {
            url: baseUrl,
            version: "6.1.0-rc",
        },
    },
};

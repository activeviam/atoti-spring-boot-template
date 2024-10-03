## Traces

### How to observe

The SpringBoot app and the stack should be both running.<br>
Run a query from the UI:<br>
![alt text](./img/traces_0.png)

Check the logs and pickup one traceId, pick a trace related to a query log (_[activepivot, query]_):<br>
![alt text](./img/traces_2.png)

Connect to Grafana and use Tempo in order to query that traceId.<br>
Go to `http://localhost:3000/`, then search for that trace:<br>
![alt text](./img/traces_1.png)
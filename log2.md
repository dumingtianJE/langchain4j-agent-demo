sudo kubectl describe pod -n langchain4j-agent redis-55dc64946b-svfn4Name:             langchain4j-agent-6899d77667-fl96j
Namespace:        langchain4j-agent
Priority:         0
Service Account:  default
Node:             <none>
Labels:           app=langchain4j-agent
pod-template-hash=6899d77667
version=v1
Annotations:      prometheus.io/path: /actuator/prometheus
prometheus.io/port: 8080
prometheus.io/scrape: true
Status:           Pending
IP:               
IPs:              <none>
Controlled By:    ReplicaSet/langchain4j-agent-6899d77667
Containers:
app:
Image:      langchain4j-agent:latest
Port:       8080/TCP
Host Port:  0/TCP
Limits:
cpu:     500m
memory:  768Mi
Requests:
cpu:      250m
memory:   384Mi
Liveness:   http-get http://:8080/actuator/health/liveness delay=60s timeout=5s period=10s #success=1 #failure=3
Readiness:  http-get http://:8080/actuator/health/readiness delay=30s timeout=5s period=10s #success=1 #failure=3
Startup:    http-get http://:8080/actuator/health delay=30s timeout=5s period=10s #success=1 #failure=30
Environment Variables from:
app-config  ConfigMap  Optional: false
app-secret  Secret     Optional: false
Environment:
SPRING_REDIS_PASSWORD:  <set to the key 'REDIS_PASSWORD' in secret 'app-secret'>  Optional: false
Mounts:
/app/data from app-data (rw)
/app/logs from app-logs (rw)
/var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-7pjpl (ro)
Conditions:
Type           Status
PodScheduled   False
Volumes:
app-data:
Type:       PersistentVolumeClaim (a reference to a PersistentVolumeClaim in the same namespace)
ClaimName:  app-data-pvc
ReadOnly:   false
app-logs:
Type:       PersistentVolumeClaim (a reference to a PersistentVolumeClaim in the same namespace)
ClaimName:  app-logs-pvc
ReadOnly:   false
kube-api-access-7pjpl:
Type:                    Projected (a volume that contains injected data from multiple sources)
TokenExpirationSeconds:  3607
ConfigMapName:           kube-root-ca.crt
ConfigMapOptional:       <nil>
DownwardAPI:             true
QoS Class:                   Burstable
Node-Selectors:              <none>
Tolerations:                 node.kubernetes.io/not-ready:NoExecute op=Exists for 300s
node.kubernetes.io/unreachable:NoExecute op=Exists for 300s
Events:
Type     Reason            Age   From               Message
  ----     ------            ----  ----               -------
Warning  FailedScheduling  17s   default-scheduler  running PreBind plugin "VolumeBinding": binding volumes: timed out waiting for the condition

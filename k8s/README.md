# 실습

### nginx-deploy.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-hello
  labels:
    app: nginx
spec:
  replicas: 1
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
        - name: nginx
          image: nginxdemos/hello
```

Nginx를 실행하는 간단한 Deployment 파일입니다.

spec.template.metadata.labels.app: nginx

레이블이 app: nginx로 지정되었습니다. 향후 이 레이블 이름을 기준으로 서비스 리소스가 Nginx 파드를 선택합니다.

### clusterIP-svc.yml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-svc
spec:
  selector:  # 선택해야 할 파드를 지정
    app: nginx  # 파드 Label과 동일
  type: ClusterIP
  ports:
    - name: tcp
      port: 80
      targetPort: 80
```

위에서 선언한 Nginx Deployment와 연결하는 서비스 명세서(매니페스트) 파일입니다.

spec.selector.app: nginx

- 서비스는 selector 속성을 이용하여 서비스 리소스가 연결할 파드를 선택(select)합니다. 여러 개의 파드 중에서 내가 선택할 파드를 레이블(app: nginx)을 이용하여 지정합니다.

spec.ports.port: 80

- 서비스가 노출하는 포트 번호를 지정합니다. 클라이언트로 사용하는 다른 파드에서 연결하는 포트 번호입니다.

spec.ports.targetPort: 80

- 서비스가 바라보는 타겟 파드의 포트 번호입니다. 위의 경우 80 이므로 타겟이 되는 nginx 파드의 80 포트로 연결합니다.

위 YAML 파일 기준으로 Deployment, Service 리소스를 생성합니다.

```bash
% kubectl apply -f nginx-deploy.yaml -f clusterIP-svc.yml
deployment.apps/nginx-hello created
service/nginx-svc created
```

```bash
% kubectl get deploy,svc
NAME                          READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/nginx-hello   1/1     1            1           42h

NAME                 TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)   AGE
service/nginx-svc    ClusterIP   10.106.41.199   <none>        80/TCP    42h
```

위와 같이 nginx-svc 라는 이름으로 서비스 리소스가 생성되었습니다. 이제 다른 파드에서 nginx-hello 파드를 연결하기 위해서 서비스 이름인 nginx-svc를 사용합니다.

### netshoot-deploy.yaml

서비스 이름으로 통신 테스트를 위해 네트워크 트러블슈팅 용도로 많이 사용하는 netshoot 이미지를 새로운 파드로 실행합니다.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: netshoot
  labels:
    app: netshoot
spec:
  replicas: 1
  selector:
    matchLabels:
      app: netshoot
  template:
    metadata:
      labels:
        app: netshoot
    spec:
      containers:
        - name: netshoot
          image: nicolaka/netshoot
          args:
            - sleep
            - infinity
```

netshoot-xxxx 이름의 파드가 실행됩니다.

```bash
% kubectl get pod netshoot-849567b86-rz7dr 
NAME                       READY   STATUS    RESTARTS      AGE
netshoot-849567b86-rz7dr   1/1     Running   2 (63m ago)   42h
```

해당 파드에 접속하여 nginx-svc 서비스 이름으로 파드 간 연결이 가능합니다. 마치 VM에 SSH로 연결하는 것처럼 파드에 접속하기 위하여 exec(실행) + bash 명령어를 사용합니다.

```bash
% kubectl exec -it netshoot-849567b86-rz7dr  -- bash
netshoot-849567b86-rz7dr:~# curl -I nginx-svc
HTTP/1.1 200 OK
Server: nginx/1.27.2
Date: Wed, 13 Nov 2024 00:32:20 GMT
Content-Type: text/html
Connection: keep-alive
Expires: Wed, 13 Nov 2024 00:32:19 GMT
Cache-Control: no-cache
```

nginx-svc 서비스 이름으로 curl 명령어가 정상으로 실행됩니다. 이처럼 쿠버네티스 환경에서 파드 간 연결에 서비스 이름을 사용하면 됩니다.

다음으로 서로 다른 네임스페이스 간 파드의 통신을 알아보겠습니다. 먼저 네임스페이스(Namespace)는 쿠버네티스 클러스터 내에서 리소스들을 구분하고 격리하는 논리적인 공간입니다. 네임스페이스를 사용하여 클러스터 내의 리소스를 그룹화하여 리소스 간 충돌이나 혼동을 방지할 수 있습니다. 같은 네임스페이스 안에서는 파드, 서비스 이름을 중복하여 사용할 수 없으나 네임스페이스가 다르면 같은 이름을 사용할 수 있습니다. 쿠버네티스 클러스터는 여러 개의 네임스페이스를 가질 수 있으며, 각 네임스페이스는 고유한 이름으로 식별됩니다.

```bash
% kubectl get namespace
NAME                   STATUS   AGE
default                Active   16d
kube-node-lease        Active   16d
kube-public            Active   16d
kube-system            Active   16d
```

같은 네임스페이스 내에서 파드 간 연결은 앞의 예제와 같이 서비스 이름(nginx-svc)으로 가능합니다. 하지만 네임스페이스가 다르면 연결을 위한 서비스 이름에 네임스페이스 이름까지 추가합니다. 예를 들어 nginx-svc가 nginx-ns 네임스페이스에서 실행 중이면 nginx-svc.nginx-ns로 {서비스 이름}.{네임스페이스 이름} 형식으로 연결합니다.

네임스페이스 생성

```bash
% kubectl create namespace nginx-ns
namespace/nginx-ns created
```

해당 네임스페이스에 Deployment, Service 리소스를 생성

```bash
% kubectl apply -f nginx-deploy.yaml -n nginx-ns         
deployment.apps/nginx-hello unchanged

% kubectl apply -f clusterIP-svc.yml -n nginx-ns
service/nginx-svc created
```

nginx-ns 네임스페이스에 nginx-svc 이름의 서비스를 생성한 예제입니다.

```bash
% kubectl get svc -n nginx-ns
NAME        TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)   AGE
nginx-svc   ClusterIP   10.107.182.229   <none>        80/TCP    53s
```

default 네임스페이스에서 실행 중인 파드에서 nginx-ns 네임스페이스의 서비스와 연결하기 위해서는 아래와 같이 nginx-ns 네임스페이스 이름을 추가합니다.

네임스페이스 이름없이 서비스 이름만 호출하면 연결하지 못합니다.

```bash
% kubectl exec -it netshoot-849567b86-rz7dr  -- bash
netshoot-849567b86-rz7dr:~# curl -I nginx-svc.nginx-ns
HTTP/1.1 200 OK
Server: nginx/1.27.2
Date: Wed, 13 Nov 2024 00:59:04 GMT
Content-Type: text/html
Connection: keep-alive
Expires: Wed, 13 Nov 2024 00:59:03 GMT
Cache-Control: no-cache
```

다음은 로드밸런싱 기능입니다. nginx 파드를 재시작하여 파드의 IP가 변경되거나 파드의 수량을 증가하면 서비스가 바라보는 타겟 IP 정보가 변경됩니다. 위와 같은 변동 사항을 서비스는 자동으로 감지하여 정보를 업데이트 합니다. 변경된 내역은 endpoint 리소스에서 확인할 수 있습니다.

실제 파드의 수량을 증가해 보겠습니다. 파드의 수량 증가는 scale 명령어를 사용합니다.

증가 전 endpoint IP : 10.1.0.128

```bash
% kubectl describe ep nginx-svc
Name:         nginx-svc
Namespace:    default
Labels:       <none>
Annotations:  endpoints.kubernetes.io/last-change-trigger-time: 2024-11-12T23:27:06Z
Subsets:
  Addresses:          10.1.0.128
  NotReadyAddresses:  <none>
  Ports:
    Name  Port  Protocol
    ----  ----  --------
    tcp   80    TCP

Events:  <none>
```

증가 후 endpoint IP가 5개로 증가

```bash
% kubectl scale deployment nginx-hello --replicas 5
deployment.apps/nginx-hello scaled

% kubectl describe ep nginx-svc                    
Name:         nginx-svc
Namespace:    default
Labels:       <none>
Annotations:  endpoints.kubernetes.io/last-change-trigger-time: 2024-11-13T01:06:08Z
Subsets:
  Addresses:          10.1.0.128,10.1.0.131,10.1.0.132,10.1.0.133,10.1.0.134
  NotReadyAddresses:  <none>
  Ports:
    Name  Port  Protocol
    ----  ----  --------
    tcp   80    TCP

Events:  <none>
```

nginx-svc라는 이름으로 접속하면 자동으로 5개의 파드에 균등하게 접속되는 걸 확인할 수 있습니다. 서비스가 로드밸런싱 하여 하나 이상의 파드에 트래픽을 분산시키기 때문입니다.

```bash
netshoot-849567b86-rz7dr:~# curl -S nginx-svc | grep address
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 12130    0 12130    0     0  4406k      0 --:--:-- --:--:-- --:--:-- 5922k
<p><span>Server&nbsp;address:</span> <span>10.1.0.134:80</span></p>
netshoot-849567b86-rz7dr:~# curl -S nginx-svc | grep address
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 12130    0 12130    0     0  4111k      0 --:--:-- --:--:-- --:--:-- 5922k
<p><span>Server&nbsp;address:</span> <span>10.1.0.132:80</span></p>
netshoot-849567b86-rz7dr:~# curl -S nginx-svc | grep address
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 12130    0 12130    0     0  7303k      0 --:--:-- --:--:-- --:--:-- 11.5M
<p><span>Server&nbsp;address:</span> <span>10.1.0.133:80</span></p>
netshoot-849567b86-rz7dr:~# curl -S nginx-svc | grep address
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 12130    0 12130    0     0  7025k      0 --:--:-- --:--:-- --:--:-- 11.5M
<p><span>Server&nbsp;address:</span> <span>10.1.0.128:80</span></p>
```
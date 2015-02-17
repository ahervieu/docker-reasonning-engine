docker container reasonning engine
==============

This project contains all the information to deploy a stack for monitoring and driving non functional properties of docker containers.

Warning : the following source code is a POC. It's a prototype that has been written quickly.

# Description :
This project relies on the following project :
* influxdb : a time series db (http://influxdb.com/)
* grafana : a dashboard tool (http://grafana.org/)
* cadvisor : Analyzes resource usage and performance characteristics of running containers. (https://github.com/google/cadvisor)
* docker container manager : a project to change non functionnal properties of running container (https://github.com/ahervieu/docker-manager)
                     
## Connecting to influx db :

* url : http://localhost:8083/
* usr/pass : root root
* dbname : cadvisor

For additional information please refer to : https://github.com/tutumcloud/tutum-docker-influxdb
                            
## Connecting to cadvisor:

* url : http://localhost:8080/

## Connecting to grafana:

* url : http://127.0.0.1/
* usr/pass : admin/mypass
                         
For additional information please refer to : https://github.com/tutumcloud/tutum-docker-grafana
# Getting started :


Requirements :
* docker
* docker rest sever
* fig
* maven
* java
* git

The following lines will download source code and deploy containers.
```
git clone https://github.com/ahervieu/docker-manager.git
cd docker-manager
sudo fig up
```

Start a toy container :
```
 sudo docker run --cap-add=NET_ADMIN -ti --cpuset=0 ahervieu/stress_diverse
```

Run the application
```
mvn exec:java -Dexec.mainClas"org.diverse.docker.reasoning.ReasoningEngine"
```
or in your favorite ide.

Then in toy container run the following command :
```
stress --cpu 2
```
And observe  results.
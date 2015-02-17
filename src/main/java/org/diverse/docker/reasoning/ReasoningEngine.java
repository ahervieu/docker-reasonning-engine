package org.diverse.docker.reasoning;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import org.joda.time.*;
import org.joda.time.format.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Created by aymeric on 16/02/15.
 * A very simple class to give an insight
 * of how you can manipulate docker container at runtime 
 */
public class ReasoningEngine {
    static Properties prop = new Properties() ;
    
    public static void main(String[] args) throws Exception {

        String propFileName = "config.properties" ;
        InputStream is = ReasoningEngine.class.getClassLoader().getResourceAsStream(propFileName);
        if(is != null){
            prop.load(is);
        }else
        {
            throw new FileNotFoundException("property file is missing");
        }
        //Connexion to docker Rest Server

        DockerClient dockerClient = DockerClientBuilder.getInstance("http://"+ prop.getProperty("DOCKER_HOST") + ":" + prop.getProperty("DOCKER_PORT")).build();
        //List of running containers :

        List<Container> containerList = dockerClient.listContainersCmd().exec() ;
        //Get the running container based on stress image
        Container c2 = containerList.stream().filter(c -> c.getImage().contains("stress_diverse")).collect(Collectors.toList()).get(0) ;
        if(c2 == null){
            throw new Exception("Please start a container with the following image : ahervieu/stress_diverse") ;
        }else {
            //Start Monitoring
            // if cpu comspution greater than 70 % reduce the CPU
            //Connecting to cadvisor the get the cpu consumption
            limitCPU(-1,c2.getId());
        

            boolean limit = false ;

            while(true) {
            Thread.sleep(5000);

                JSONObject jsonObj = getCAdvisorContainer(c2.getId());
                double cpuFreq = getInstantPercentCPUUsage(jsonObj);
                System.out.println("CPU 1 consumption :" +cpuFreq);
                if (cpuFreq > 90) {
                    System.err.println("Warning High CPU Consumption, limiting CPU utilization");
                    limitCPU(20,c2.getId());
                    limit = true ;

                }
                
                if (limit && cpuFreq < 10) {
                    System.err.println("Releasing constraints");
                    limitCPU(-1,c2.getId());
                    limit = false ;
                }
            }
        }

    }
    
    public static JSONObject getCAdvisorContainer(String ContainerId) throws IOException, UnirestException {

        HttpResponse<JsonNode> jsonResponse = Unirest.get("http://"+prop.getProperty("CADVISOR_HOST") +":" +
                prop.getProperty("CADVISOR_PORT") + "/api/" +
                prop.getProperty("CADVISOR_API") + "/docker/" + ContainerId).asJson();

        JSONObject res = jsonResponse.getBody().getObject();


     return res ;
    }
    
    /*
    Compute CPU 1 consumption
     */
    public static double getInstantPercentCPUUsage(JSONObject obj0) throws ParseException {

        JSONObject obj = obj0.getJSONObject(obj0.keys().next()) ;
        JSONObject firstValue = obj.getJSONArray("stats").getJSONObject(obj.getJSONArray("stats").length()-1);
        JSONObject secondValue = obj.getJSONArray("stats").getJSONObject(obj.getJSONArray("stats").length()-2);
        String ts1 = firstValue.getString("timestamp");
        String ts2 = secondValue.getString("timestamp");

        int cpuUsage1 = firstValue.getJSONObject("cpu").getJSONObject("usage").getInt("total");
        int cpuUsage2 = secondValue.getJSONObject("cpu").getJSONObject("usage").getInt("total");

        DateTimeFormatter parser = ISODateTimeFormat.dateTime();
        DateTime dt1 = parser.parseDateTime(ts1);
        DateTime dt2 = parser.parseDateTime(ts2);

        DateTimeFormatter formatter = DateTimeFormat.mediumDateTime();
        return (cpuUsage1-cpuUsage2)/((dt1.getSecondOfDay()-dt2.getSecondOfDay())*10000000) ;
    }
    
    public static void limitCPU(int Speed, String ContainerId) throws IOException, UnirestException {
        HttpResponse<JsonNode> jsonResponse = Unirest.get("http://"+prop.getProperty("DOCKER_MAN_HOST") +":" +prop.getProperty("DOCKER_MAN_PORT") +
                "/container_manager_rest_server/rest/api/" + ContainerId).asJson();

        JSONObject res = jsonResponse.getBody().getObject();

        res.put("cpu_freq", Speed);
        JsonNode jn = new JsonNode(res.toString()) ;
        HttpResponse<String> rep = Unirest.put("http://"+prop.getProperty("DOCKER_MAN_HOST") +":" +prop.getProperty("DOCKER_MAN_PORT") +
                "/container_manager_rest_server/rest/api/" + ContainerId).header("Content-Type","application/json").header("accept", "application/json").body(jn).asString();

    }
    
}

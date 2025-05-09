package ch.onigoetz.dockerstats;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerEventListener.class);
    private final DockerClient dockerClient;

    private final CacheInformation cacheInformation;
    private final EventRecorder eventRecorder;
    private final EventResultCallback eventResultCallback;

    public DockerEventListener(InfluxDBClient influxDBClient) {
        // Initialize Docker client
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(new com.github.dockerjava.core.DefaultDockerClientConfig.Builder().build().getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        dockerClient = DockerClientImpl
                .getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder().build(), httpClient);

        cacheInformation = new CacheInformation(dockerClient);
        eventRecorder = new EventRecorder(cacheInformation, influxDBClient);
        eventResultCallback = new EventResultCallback(eventRecorder);
    }

    public void startListening() {
        LOGGER.info("Starting Docker event listener...");
        LOGGER.info("Monitoring for interesting events...");
        
        try {
            dockerClient.eventsCmd().exec(eventResultCallback);
            
            // Keep the application running
            while (true) {
                TimeUnit.SECONDS.sleep(10);
            }
            
        } catch (InterruptedException e) {
            LOGGER.error("Event listener interrupted: ", e);
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            LOGGER.error("Usage: java -jar docker-events-stats-*.jar <influxHost> <influxToken> <influxOrg> <influxBucket>");
            System.exit(1);
        }

        String influxHost = args[0];
        String influxToken = args[1];
        String influxOrg = args[2];
        String influxBucket = args[3];

        var influxDBClient = InfluxDBClientFactory.create(influxHost, influxToken.toCharArray(), influxOrg, influxBucket);

        DockerEventListener listener = new DockerEventListener(influxDBClient);
        listener.startListening();
    }
}
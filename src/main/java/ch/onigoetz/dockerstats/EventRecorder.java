package ch.onigoetz.dockerstats;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

public class EventRecorder {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventRecorder.class);

	private final CacheInformation cacheInformation;
	private final InfluxDBClient influxDBClient;
	private final Set<String> recordedPulls = new HashSet<>();
	private final Set<String> recorderStarts = new HashSet<>();

	public EventRecorder(CacheInformation cacheInformation, InfluxDBClient influxDBClient) {
		this.cacheInformation = cacheInformation;
		this.influxDBClient = influxDBClient;
	}

	public void recordPull(String image, Long timestamp) {
		if (!recordedPulls.add(image)) {
			// We only record pulls once
			return;
		}

		var cacheStats = cacheInformation.isCacheHit(image);
		double sizeInMB = cacheStats.downloadSize() / (1024.0 * 1024.0);

		LOGGER.info("PULL: {}, {}, {}, {} MB Downloaded", image, timestamp, cacheStats.hitType(), sizeInMB);

		influxDBClient.getWriteApiBlocking().writePoint(Point
				.measurement("docker_image_pull")
				.addTag("image", image)
				.addTag("cacheHit", cacheStats.hitType().toString())
				.addField("downloadSize", cacheStats.downloadSize())
				.addField("count", 1)
				.time(timestamp, WritePrecision.S)
		);
	}

	public void recordStart(String image, Long timestamp) {
		String realImageName = image.contains(":") ? image : image + ":latest";

		if (!recorderStarts.add(realImageName)) {
			// We only record starts once
			return;
		}

		LOGGER.info("START: {}, {}", realImageName, timestamp);

		influxDBClient.getWriteApiBlocking().writePoint(Point
				.measurement("docker_container_create")
				.addTag("image", realImageName)
				.addField("count", 1)
				.time(timestamp, WritePrecision.S)
		);
	}
}

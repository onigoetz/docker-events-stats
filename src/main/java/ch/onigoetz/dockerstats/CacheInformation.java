package ch.onigoetz.dockerstats;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;

public class CacheInformation {
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheInformation.class);

	private final DockerClient dockerClient;

	enum CacheHitType {
		/** Was not in the cache previously, had to download all layers */
		MISS,
		/** Was already in the cache, full hit */
		HIT,
		/** We already knew of some layers */
		PARTIAL
	}

	record DownloadSize(CacheHitType hitType, Long downloadSize) {}

	record DockerImage(String id, String[] tags, Set<String> layers) {}

	Map<String, DockerImage> cachedImages = new HashMap<>();
	Set<String> knownDigests = new HashSet<>();

	CacheInformation(DockerClient dockerClient) {
		this.dockerClient = dockerClient;

		storeInitialCache();
	}

	void storeInitialCache() {
		List<Image> images = dockerClient.listImagesCmd().exec();
		for (Image image : images) {
			knownDigests.addAll(Arrays.asList(image.getRepoDigests()));
		}
	}

	DownloadSize isCacheHit(String image) {
		var imageDetails = getByRepoTag(image);

		if (imageDetails == null) {
			return null;
		}

		var isKnownDigest = false;
		for (String digest : imageDetails.getRepoDigests()) {
			if (knownDigests.contains(digest)) {
				isKnownDigest = true;
				break;
			}
		}

		if (isKnownDigest) {
			return new DownloadSize(CacheHitType.HIT, 0L);
		}

		if (imageDetails.getSharedSize() > 0L) {
			return new DownloadSize(CacheHitType.PARTIAL, imageDetails.getSize() - imageDetails.getSharedSize());
		}

		return new DownloadSize(CacheHitType.MISS, imageDetails.getSize());
	}

	private Image getByRepoTag(String imageName) {
		try {
			List<Image> images = dockerClient.listImagesCmd().exec();

			for (Image image : images) {
				for (String repoTag : image.getRepoTags()) {
					if (imageName.equals(repoTag)) {
						return image;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Error finding image details: ", e);
		}

		return null;
	}
}

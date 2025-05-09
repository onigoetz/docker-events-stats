package ch.onigoetz.dockerstats;

import java.io.Closeable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventType;

public class EventResultCallback implements ResultCallback<Event> {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventResultCallback.class);

	private final EventRecorder eventRecorder;

	EventResultCallback(EventRecorder recorder) {
		this.eventRecorder = recorder;
	}

	@Override
	public void onStart(Closeable closeable) {
		LOGGER.info("Event listener started");
	}

	@Override
	public void onNext(Event event) {
		String action = event.getAction();
		EventType type = event.getType();

		//LOGGER.info("action: {}, type: {}", action, type);

		// {"status":"pull","id":"alpine:latest","Type":"image","Action":"pull","Actor":{"ID":"alpine:latest","Attributes":{"name":"alpine"}},"scope":"local","time":1746734488,"timeNano":1746734488350036903}
		// {"status":"start","id":"bd07e61a747c5fb3f52f7e60d76466be7823012f993e03a665c0c89d4bd0d72e","from":"alpine","Type":"container","Action":"start","Actor":{"ID":"bd07e61a747c5fb3f52f7e60d76466be7823012f993e03a665c0c89d4bd0d72e","Attributes":{"image":"alpine","name":"goofy_sinoussi"}},"scope":"local","time":1746735573,"timeNano":1746735573558214163}

		if (action.equals("pull") && type.equals(EventType.IMAGE)) {
			eventRecorder.recordPull(event.getId(), event.getTime());
		}

		if (action.equals("start") && type.equals(EventType.CONTAINER)) {
			eventRecorder.recordStart(event.getActor().getAttributes().get("image"), event.getTime());
		}
	}

	@Override
	public void onError(Throwable throwable) {
		LOGGER.error("Error in event listener: ", throwable);
	}

	@Override
	public void onComplete() {
		LOGGER.info("Event listener completed");
	}

	@Override
	public void close() {
		LOGGER.info("Event listener closed");
	}
}

package za.org.grassroot.messaging.service;

import za.org.grassroot.messaging.domain.Notification;

/**
 * Created by luke on 2015/09/09.
 */
public interface MessageSendingService {

	void sendMessage(Notification notification);

	void sendMessage(String destination, Notification notification);

	void resendFailedGcmMessage(Notification notification);

    void sendPollingMessage();
}

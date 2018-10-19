package za.org.grassroot.messaging.service.whatsapp;

import za.org.grassroot.core.domain.Notification;

public interface WhatsAppBroker {

    void sendWhatsAppMessage(Notification notification); // because we need a lot of stuff

    void sendPriorityWhatsAppMessage(Notification notification); // in case we need in future, but for now, will just call generic

}

package za.org.grassroot.messaging.scheduling;


public interface SMSDeliveryReceiptFetcher {

    void fetchDeliveryReceiptsFromApiLog();

    void clearCallBackQueue();
}
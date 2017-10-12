package za.org.grassroot.messaging.service.sms;

import za.org.grassroot.core.enums.MessagingProvider;

/**
 * Created by luke on 2017/05/20.
 */
public class MockSmsResponse implements SmsGatewayResponse {

    private SmsResponseType smsResponseType;
    private boolean successful;

    public static MockSmsResponse make(SmsResponseType response, boolean successful) {
        MockSmsResponse mock = new MockSmsResponse();
        mock.smsResponseType = response;
        mock.successful = successful;
        return mock;
    }

    @Override
    public SmsResponseType getResponseType() {
        return smsResponseType;
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public Integer getErrorCode() {
        return null;
    }

    @Override
    public String getMessageKey() {
        return null;
    }

    @Override
    public MessagingProvider getProvider() {
        return null;
    }
}

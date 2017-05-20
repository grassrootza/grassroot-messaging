package za.org.grassroot.messaging.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonenumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhoneNumberUtil {

    private static final Logger logger = LoggerFactory.getLogger(PhoneNumberUtil.class);

    public static String convertPhoneNumber(String inputString) {
        try {
            com.google.i18n.phonenumbers.PhoneNumberUtil phoneNumberUtil = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(inputString, "ZA");

            if (phoneNumberUtil.isValidNumber(phoneNumber)) {
                return phoneNumberUtil.format(phoneNumber, com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.E164).replace("+", "");
            } else {
                logger.warn("Error! Phone number is not valid, string: {}", phoneNumber);
                return null;
            }
        } catch (NumberParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}

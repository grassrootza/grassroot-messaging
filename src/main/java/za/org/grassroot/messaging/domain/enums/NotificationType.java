package za.org.grassroot.messaging.domain.enums;

/**
 * Created by paballo on 2016/04/08.
 */
public enum NotificationType {

	// general types
	EVENT,
	TODO,
	CHAT,
	ACCOUNT,
	USER,
	LIVEWIRE,
	GENERAL;

	public static NotificationType fromDetailedType(NotificationDetailedType detailedType) {
		switch (detailedType) {
			case WELCOME:
				return USER;
			case EVENT_INFO:
			case EVENT_CANCELLED:
			case EVENT_CHANGED:
			case EVENT_REMINDER:
			case MEETING_RSVP_TOTALS:
			case MEETING_THANKYOU:
			case VOTE_RESULTS:
			case EVENT_RESPONSE:
				return EVENT;
			case TODO_INFO:
			case TODO_REMINDER:
				return TODO;
			case JOINREQUEST:
				return USER;
			case FREE_FORM_MESSAGE:
				return GENERAL;
			case ACCOUNT_BILLING_NOTIFICATION:
				return ACCOUNT;
			case LIVEWIRE_TO_REVIEW:
			case LIVEWIRE_MADE_CONTACT:
			case LIVEWIRE_ALERT_RELEASED:
				return LIVEWIRE;
			default:
				return GENERAL;
		}
	}


	//
}

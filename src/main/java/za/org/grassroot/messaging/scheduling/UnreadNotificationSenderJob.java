package za.org.grassroot.messaging.scheduling;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

/**
 * This job class is instantiated by Quartz, not Spring, so there is no bean injection mechanism in play here and
 * we have to extract required dependencies from Spring's ApplicationContext.
 */
@DisallowConcurrentExecution
public class UnreadNotificationSenderJob extends ApplicationContextAwareQuartzJobBean {
	@Override
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		ApplicationContext applicationContext = getApplicationContext(context);

		UnreadNotificationHandler unreadNotificationHandler = applicationContext.getBean(UnreadNotificationHandler.class);
		unreadNotificationHandler.processUnreadNotifications();
	}
}

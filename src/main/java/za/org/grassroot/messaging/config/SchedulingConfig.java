package za.org.grassroot.messaging.config;

import lombok.extern.slf4j.Slf4j;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import za.org.grassroot.messaging.scheduling.ApplicationContextAwareQuartzJobBean;
import za.org.grassroot.messaging.scheduling.receipts.FailedEmailResponseFetcher;
import za.org.grassroot.messaging.scheduling.receipts.SMSDeliveryReceiptFetcher;
import za.org.grassroot.messaging.scheduling.sending.BatchedNotificationSenderJob;
import za.org.grassroot.messaging.scheduling.sending.UnreadNotificationSenderJob;
import za.org.grassroot.messaging.scheduling.sending.UnsuccessfulSmsHandlerJob;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by luke on 2017/05/18.
 */
@Configuration @Slf4j
public class SchedulingConfig implements SchedulingConfigurer {

    private Environment env;
    private SMSDeliveryReceiptFetcher smsDeliveryReceiptFetcher;
    private FailedEmailResponseFetcher emailFailureFetcher;

    @Autowired
    public SchedulingConfig(Environment env, SMSDeliveryReceiptFetcher smsDeliveryReceiptFetcher) {
        this.env = env;
        this.smsDeliveryReceiptFetcher = smsDeliveryReceiptFetcher;
    }

    @Autowired(required = false)
    public void setEmailFailureFetcher(FailedEmailResponseFetcher emailFailureFetcher) {
        this.emailFailureFetcher = emailFailureFetcher;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        log.info("Setting standard scheduled tasks");
        taskRegistrar.setScheduler(taskExecutor());
        Boolean callBackQueueEnabled = env.getProperty("grassroot.callbackq.enabled", Boolean.class);
        if (callBackQueueEnabled != null && callBackQueueEnabled) {
            taskRegistrar.addFixedRateTask(smsDeliveryReceiptFetcher::clearCallBackQueue,
                    env.getProperty("grassroot.callbackq.interval", Long.class));
        }
        Boolean emailFetchingEnabled = env.getProperty("grassroot.email.failure.fetching", Boolean.class);
        if (emailFailureFetcher != null && emailFetchingEnabled != null && emailFetchingEnabled) {
            taskRegistrar.addFixedRateTask(emailFailureFetcher::fetchInvalidEmailAddesses,
                    env.getProperty("grassroot.email.failure.interval", Long.class, 60000L));
        }
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskExecutor() {
        return Executors.newScheduledThreadPool(10);
    }

    @Bean
    public JobDetailFactoryBean batchedNotificationSenderJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(BatchedNotificationSenderJob.class);
        factoryBean.setDurability(false);
        return factoryBean;
    }

    @Bean
    public CronTriggerFactoryBean batchedNotificationSenderCronTrigger(
            @Qualifier("batchedNotificationSenderJobDetail") JobDetail jobDetail) {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(jobDetail);
        factoryBean.setCronExpression("0/15 * * * * ?");
        factoryBean.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        return factoryBean;
    }

    @Bean
    public JobDetailFactoryBean unreadNotificationSenderJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(UnreadNotificationSenderJob.class);
        factoryBean.setDurability(false);
        return factoryBean;
    }

    @Bean
    public CronTriggerFactoryBean unreadNotificationSenderCronTrigger(
            @Qualifier("unreadNotificationSenderJobDetail") JobDetail jobDetail) {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(jobDetail);
        String unreadNotificationsHandlerCron = env.getProperty("unread.notifications.handler.cron", "0 0/1 * * * ?");
        factoryBean.setCronExpression(unreadNotificationsHandlerCron);
        factoryBean.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        return factoryBean;
    }

    @Bean
    public JobDetailFactoryBean unsuccessfulSmsHandlerJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(UnsuccessfulSmsHandlerJob.class);
        factoryBean.setDurability(false);
        return factoryBean;
    }

    @Bean
    public CronTriggerFactoryBean unsuccessfulSmsHandlerCronTrigger(
            @Qualifier("unsuccessfulSmsHandlerJobDetail") JobDetail jobDetail) {
        CronTriggerFactoryBean factoryBean = new CronTriggerFactoryBean();
        factoryBean.setJobDetail(jobDetail);
        String unsuccessfulNotificationsHandlerCron = env.getProperty("unsuccessful.notifications.handler.cron", "0 0/15 * * * ?");
        factoryBean.setCronExpression(unsuccessfulNotificationsHandlerCron);
        factoryBean.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        return factoryBean;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(@Qualifier("batchedNotificationSenderCronTrigger") CronTrigger sendTrigger,
                                                     @Qualifier("unreadNotificationSenderCronTrigger") CronTrigger unreadTrigger,
                                                     @Qualifier("unsuccessfulSmsHandlerCronTrigger") CronTrigger failedSendTrigger) {
        Properties quartzProperties = new Properties();

        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setAutoStartup(true);
        factory.setSchedulerName("grassroot-quartz");
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setQuartzProperties(quartzProperties);
        factory.setStartupDelay(0);
        factory.setApplicationContextSchedulerContextKey(ApplicationContextAwareQuartzJobBean.APPLICATION_CONTEXT_KEY);
        factory.setTriggers(sendTrigger, unreadTrigger, failedSendTrigger);

        return factory;
    }

}
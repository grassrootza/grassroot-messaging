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
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import za.org.grassroot.messaging.scheduling.*;

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

    @Autowired
    public SchedulingConfig(Environment env, SMSDeliveryReceiptFetcher smsDeliveryReceiptFetcher) {
        log.info("Constructing scheduling config");
        this.env = env;
        this.smsDeliveryReceiptFetcher = smsDeliveryReceiptFetcher;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        log.info("Setting standard scheduled tasks");
        taskRegistrar.setScheduler(taskExecutor());
        CronTask smsDeliveryFetchCronTask = new CronTask(smsDeliveryReceiptFetcher::fetchDeliveryReceiptsFromApiLog, " 0 0/1 * * * ?");
        Boolean smsFetchEnabled = env.getProperty("grassroot.smsfetcher.enabled", Boolean.class);
        if (smsFetchEnabled != null && smsFetchEnabled) {
            taskRegistrar.addCronTask(smsDeliveryFetchCronTask);
        }
        Boolean callBackQueueEnabled = env.getProperty("grassroot.callbackq.enabled", Boolean.class);
        if (callBackQueueEnabled != null && callBackQueueEnabled) {
            taskRegistrar.addFixedRateTask(smsDeliveryReceiptFetcher::clearCallBackQueue,
                    env.getProperty("grassroot.callbackq.interval", Long.class));
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
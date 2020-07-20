package za.org.grassroot.messaging.service

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Matchers
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import za.org.grassroot.core.domain.Notification
import za.org.grassroot.core.domain.User
import za.org.grassroot.core.domain.group.Group
import za.org.grassroot.core.domain.group.Membership
import za.org.grassroot.core.domain.notification.NotificationStatus
import za.org.grassroot.core.enums.MessagingProvider
import za.org.grassroot.core.repository.MembershipRepository
import za.org.grassroot.core.repository.NotificationRepository
import za.org.grassroot.messaging.domain.NotificationSpecifications
import java.util.*
import kotlin.collections.HashSet

class NotificationBrokerImplTest {

    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    private val notificationRepository: NotificationRepository = mock(NotificationRepository::class.java)
    private val membershipRepository: MembershipRepository = mock(MembershipRepository::class.java)

    private val notificationBroker: NotificationBroker = NotificationBrokerImpl(notificationRepository, membershipRepository)


    @Test
    fun `loadNotification should call notification repository once`() {
        notificationBroker.loadNotification("uid1")
        val captor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)

        verify(notificationRepository, times(1)).findByUid(captor.capture())
        verifyNoMoreInteractions(notificationRepository)
        assertEquals(captor.value, "uid1")
    }

    @Test
    fun `loadSentNotificationsWithUnknownDeliveryStatus should call notification repository once`() {
        notificationBroker.loadSentNotificationsWithUnknownDeliveryStatus(MessagingProvider.EMAIL)
        val captorMessagingProvider: ArgumentCaptor<MessagingProvider> = ArgumentCaptor.forClass(MessagingProvider::class.java)

        verify(notificationRepository, times(1)).findAll(NotificationSpecifications.getSentNotificationsWithUnknownDeliveryStatus(captorMessagingProvider.capture()))
        verifyNoMoreInteractions(notificationRepository)
    }

    @Test
    fun `loadBySendingKey should call notification repository once`() {
        notificationBroker.loadBySendingKey("sendingKey")

        verify(notificationRepository, times(1)).findOne(Matchers.any())
        verifyNoMoreInteractions(notificationRepository)
    }

    @Test
    fun `updateNotificationStatus should call notification repository once if notification doesn't exist`() {
        notificationBroker.updateNotificationStatus("notificationUid", NotificationStatus.READ, "errorMessage", false, false, "messageSendKey", MessagingProvider.EMAIL)

        verify(notificationRepository, times(1)).findByUid(Matchers.any())
        verifyNoMoreInteractions(notificationRepository)
    }

    @Test
    fun `updateNotificationStatus should call notification repository twice if notification exists`() {
        val notification = mock(Notification::class.java)
        Mockito.`when`(notificationRepository.findByUid(Matchers.any())).thenReturn(notification)

        notificationBroker.updateNotificationStatus("notificationUid", NotificationStatus.READ, "errorMessage", false, false, "messageSendKey", MessagingProvider.EMAIL)

        verify(notificationRepository, times(1)).findByUid(Matchers.any())
        verify(notificationRepository, times(1)).save(Matchers.any())
        verifyNoMoreInteractions(notificationRepository)
    }

    @Test
    fun `updateNotifications should call notification repository once`() {
        val notificationsSet = HashSet<Notification>()
        val notification = mock(Notification::class.java)
        notificationsSet.add(notification)
        notificationBroker.updateNotifications(notificationsSet)

        verify(notificationRepository, times(1)).saveAll(notificationsSet)
        verifyNoMoreInteractions(notificationRepository)
    }

    @Test
    fun `incrementReceiptFetchCount should call notification repository once if notification doesn't exist`() {
        notificationBroker.incrementReceiptFetchCount("uid1")

        verify(notificationRepository, times(1)).findByUid("uid1")
        verifyNoMoreInteractions(notificationRepository)
    }

    @Test
    fun `incrementReceiptFetchCount should call notification repository twice if notification exists`() {
        val notification = mock(Notification::class.java)
        Mockito.`when`(notificationRepository.findByUid(Matchers.any())).thenReturn(notification)

        notificationBroker.incrementReceiptFetchCount("uid1")

        verify(notificationRepository, times(1)).findByUid("uid1")
        verify(notificationRepository, times(1)).save(notification)
        verifyNoMoreInteractions(notificationRepository)
    }

    @Test
    fun `isUserSelfJoinedToGroup calls membership repository once`() {
        val notification = mock(Notification::class.java)
        val user = mock(User::class.java)
        Mockito.`when`(notification.getTarget()).thenReturn(user)
        val group =  mock(Group::class.java)
        Mockito.`when`(notification.getRelevantGroup()).thenReturn(group)
        val membership = mock(Membership::class.java)
        Mockito.`when`(membershipRepository.findOne(Matchers.any())).thenReturn(Optional.of(membership))
        notificationBroker.isUserSelfJoinedToGroup(notification)

        verify(membershipRepository, times(1)).findOne(Matchers.any())
    }

}
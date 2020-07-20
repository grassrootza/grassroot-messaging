package za.org.grassroot.messaging

import org.mockito.ArgumentCaptor

object MockitoHelper {
    fun <T> argumentCapture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
}
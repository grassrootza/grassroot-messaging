package za.org.grassroot.messaging.service

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectInputStream
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox
import za.org.grassroot.core.domain.media.MediaFileRecord
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets


@RunWith(PowerMockRunner::class)
@PowerMockIgnore("javax.management.*")
class StorageBrokerImplTest {

    @SuppressWarnings

    @InjectMocks
    private val storageBroker: StorageBroker =  StorageBrokerImpl()

    private lateinit var builder: AmazonS3ClientBuilder
    private lateinit var mediaFileRecord: MediaFileRecord
    private lateinit var s3client: AmazonS3
    private lateinit var s3Object: S3Object
    private lateinit var s3is: S3ObjectInputStream

    @Before
    fun `steup before`() {
        builder = mock(AmazonS3ClientBuilder::class.java)
        mediaFileRecord = mock(MediaFileRecord::class.java)
        s3client = mock(AmazonS3::class.java)
        s3Object = mock(S3Object::class.java)

        val content: String = "some content"
        s3is = S3ObjectInputStream(ByteArrayInputStream(StandardCharsets.US_ASCII.encode(content).array()), null)

        MockitoAnnotations.initMocks(this)
        Whitebox.setInternalState(StorageBrokerImpl::class.java, "builder", builder)

        Mockito.`when`(mediaFileRecord.getBucket()).thenReturn("bucketone")
        Mockito.`when`(mediaFileRecord.getKey()).thenReturn("file/name")
        Mockito.`when`(builder.build()).thenReturn(s3client)
        Mockito.`when`(s3client.getObject(any())).thenReturn(s3Object)
        Mockito.`when`(s3Object.getObjectContent()).thenReturn(s3is)
    }

    @Test
    fun `fetchFileFromRecord`() {

        Assert.assertNotNull(storageBroker.fetchFileFromRecord(mediaFileRecord))
    }
}
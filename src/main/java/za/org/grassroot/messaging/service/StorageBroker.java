package za.org.grassroot.messaging.service;

import za.org.grassroot.core.domain.media.MediaFileRecord;

import java.io.File;

// pretty much copying this from main platform, but it's quite simple, and least bad vs injecting AWS SDK into core
public interface StorageBroker {

    File fetchFileFromRecord(MediaFileRecord record);

}

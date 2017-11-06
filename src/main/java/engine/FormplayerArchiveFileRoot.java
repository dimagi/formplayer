package engine;

import org.commcare.modern.reference.ArchiveFileReference;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.util.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * Created by willpride on 4/25/17.
 */
public class FormplayerArchiveFileRoot extends ArchiveFileRoot {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private int MAX_RECENT = 5;

    @Override
    public String addArchiveFile(ZipFile zip, String appId) {
        String mGUID = super.addArchiveFile(zip, appId);
        redisTemplate.opsForValue().set(
                String.format("formplayer:archive:%s", mGUID),
                zip.getName()
        );
        return mGUID;
    }

    // Given an encoded path (IE jr://archive/ABC123) return a Reference to be used to access the actual filesystem
    @Override
    public Reference derive(String guidPath) throws InvalidReferenceException {
        String GUID = getGUID(guidPath);
        if (guidToFolderMap.containsKey(GUID)) {
            return new ArchiveFileReference(guidToFolderMap.get(GUID), GUID, getPath(guidPath));
        }
        try {
            String zipName = redisTemplate.opsForValue().get(String.format("formplayer:archive:%s", GUID));
            return new ArchiveFileReference(new ZipFile(zipName), GUID, getPath(guidPath));
        } catch (IOException e) {
            e.printStackTrace();
            throw new InvalidReferenceException(String.format("Error deriving reference with exception %s.", guidPath), guidPath);
        }
    }
}

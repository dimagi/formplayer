package engine;

import beans.debugger.XPathQueryItem;
import org.commcare.modern.reference.ArchiveFileReference;
import org.commcare.modern.reference.ArchiveFileRoot;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.Reference;
import org.javarosa.core.util.PropertyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.zip.ZipFile;

/**
 * Created by willpride on 4/25/17.
 */
public class FormplayerArchiveFileRoot extends ArchiveFileRoot {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOperations;

    private int MAX_RECENT = 5;

    @Override
    public String addArchiveFile(ZipFile zip) {
        String mGUID = PropertyUtils.genGUID(GUID_LENGTH);
        listOperations.leftPush(
                String.format("formplayer:archive:%s", mGUID),
                zip.getName()
        );
        return mGUID;
    }

    @Override
    public Reference derive(String guidPath) throws InvalidReferenceException {
        try {
            listOperations.trim(String.format("formplayer:archive:%s", getGUID(guidPath)), 0, MAX_RECENT);
            String zipName = listOperations.range(String.format("formplayer:archive:%s", getGUID(guidPath)), 0, MAX_RECENT).get(0);
            return new ArchiveFileReference(new ZipFile(zipName), getGUID(guidPath), getPath(guidPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

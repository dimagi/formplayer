package util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class RedisLock {

    public final String lockKey;

    public long lockedAt;

    public String threadName;

    public byte[] lockHost;

    public RedisLock(String lockKey) {
        this.lockKey = lockKey;
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknownHost";
        }
        this.lockHost = host.getBytes();
    }

    private String getLockKey() {
        return this.lockKey;
    }

    private String constructLockKey() {
        return "formplayer-user:" + this.lockKey;
    }

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd@HH:mm:ss.SSS");
        return "RedisLock [lockKey=" + constructLockKey()
                + ",lockedAt=" + dateFormat.format(new Date(this.lockedAt))
                + ", thread=" + this.threadName
                + ", lockHost=" + new String(this.lockHost)
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(this.lockHost);
        result = prime * result + ((this.lockKey == null) ? 0 : this.lockKey.hashCode());
        result = prime * result + (int) (this.lockedAt ^ (this.lockedAt >>> 32));
        result = prime * result + ((this.threadName == null) ? 0 : this.threadName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RedisLock other = (RedisLock) obj;
        if (!Arrays.equals(this.lockHost, other.lockHost)) {
            return false;
        }
        if (!this.lockKey.equals(other.lockKey)) {
            return false;
        }
        if (this.lockedAt != other.lockedAt) {
            return false;
        }
        if (this.threadName == null) {
            if (other.threadName != null) {
                return false;
            }
        } else if (!this.threadName.equals(other.threadName)) {
            return false;
        }
        return true;
    }

}
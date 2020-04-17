package objects;

public class FormVolatilityRecord {
    private String key;
    private long timeout;
    private String entityName;

    public FormVolatilityRecord(String key, long timeout, String entityName) {
        this.key = key;
        this.timeout = timeout;
        this.entityName = entityName;
    }

    public String getKey() {
        return key;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getDisplayMessage(String userTitle) {
        String message = String.format(
                "Warning: This form was started recently for %s by %s %s",
                entityName == null? "the same record" : entityName,
                userTitle,
                "%s");
        return message;
    }
}

package objects;

import java.io.Serializable;

/**
 * Created by willpride on 1/19/16.
 */
public class SerializableSession implements Serializable{
    private String id;
    private String instanceXml;
    private String formXml;


    public String getInstanceXml() {
        return instanceXml;
    }

    public void setInstanceXml(String instanceXml) {
        this.instanceXml = instanceXml;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode(){
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof SerializableSession){
            return obj.hashCode() == hashCode();
        }
        return false;
    }

    @Override
    public String toString(){
        return "Session [id=" + id + ", instance=" + instanceXml + ", form=" + formXml + "]";
    }

    public String getFormXml() {
        return formXml;
    }

    public void setFormXml(String formXml) {
        this.formXml = formXml;
    }
}

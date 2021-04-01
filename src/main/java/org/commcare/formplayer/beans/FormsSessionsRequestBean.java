package org.commcare.formplayer.beans;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class FormsSessionsRequestBean extends AuthenticatedRequestBean {
    private int pageNumber;
    private int pageSize;

    @JsonGetter(value = "page_number")
    public int getPageNumber() {
        return pageNumber;
    }

    @JsonSetter(value = "page_number")
    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    @JsonGetter(value = "page_size")
    public int getPageSize() {
        return pageSize;
    }

    @JsonSetter(value = "page_size")
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}

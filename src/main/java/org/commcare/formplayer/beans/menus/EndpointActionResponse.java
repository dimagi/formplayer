package org.commcare.formplayer.beans.menus;

public class EndpointActionResponse {

    private String urlTemplate;
    private boolean background;

    public EndpointActionResponse() {
    }

    public EndpointActionResponse(String urlTemplate, boolean background) {
        this.urlTemplate = urlTemplate;
        this.background = background;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public boolean isBackground() {
        return background;
    }
}

package org.commcare.formplayer.beans.debugger;

import org.commcare.formplayer.beans.menus.LocationRelevantResponseBean;

import java.util.HashSet;
import java.util.List;

/**
 * Response for the debugger tab
 */
public class MenuDebuggerContentResponseBean extends LocationRelevantResponseBean {
    private String appId;
    private AutoCompletableItem[] autoCompletableItems;
    private XPathQueryItem[] recentXPathQueries;

    public MenuDebuggerContentResponseBean(
            String appId,
            List<String> functionList,
            List<String> instanceIds,
            List<XPathQueryItem> recentXPathQueries) {
        this.appId = appId;
        HashSet<AutoCompletableItem> autoCompletable = new HashSet<>();
        for (String function: functionList) {
            autoCompletable.add(new FunctionAutocompletable(function));
        }
        for (String instanceId: instanceIds) {
            autoCompletable.add(new InstanceAutocompletableItem(instanceId));
        }
        this.autoCompletableItems = autoCompletable.toArray(new AutoCompletableItem[autoCompletable.size()]);
        this.recentXPathQueries = recentXPathQueries.toArray(new XPathQueryItem[recentXPathQueries.size()]);
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public XPathQueryItem[] getRecentXPathQueries() {
        return recentXPathQueries;
    }

    public void setRecentXPathQueries(XPathQueryItem[] recentXPathQueries) {
        this.recentXPathQueries = recentXPathQueries;
    }

    public AutoCompletableItem[] getAutoCompletableItems() {
        return autoCompletableItems;
    }

    public void setAutoCompletableItems(AutoCompletableItem[] autoCompletableItems) {
        this.autoCompletableItems = autoCompletableItems;
    }
}

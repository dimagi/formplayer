package beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by biyeun on 10/3/16.
 *
 * Use this to respond to requests related to in form navigation (next / previous / etc)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormEntryNavigationResponseBean extends FormEntryResponseBean {
  private boolean isAtLastIndex = false;
  private boolean isAtFirstIndex = false;
  private String currentIndex;

  public boolean getIsAtLastIndex() { return isAtLastIndex; }

  public void setIsAtLastIndex(boolean isAtLastIndex) {
    this.isAtLastIndex = isAtLastIndex;
  }

  public String getCurrentIndex() { return currentIndex; }

  public void setCurrentIndex(String currentIndex) {
    this.currentIndex = currentIndex;
  }

  public boolean getIsAtFirstIndex() {
    return isAtFirstIndex;
  }

  public void setIsAtFirstIndex(boolean atFirstIndex) {
    isAtFirstIndex = atFirstIndex;
  }
}

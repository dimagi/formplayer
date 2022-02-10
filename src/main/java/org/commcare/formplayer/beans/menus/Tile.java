package org.commcare.formplayer.beans.menus;

import org.commcare.suite.model.DetailField;

/**
 * Created by willpride on 6/9/16.
 */
public class Tile {

    private int gridX;
    private int gridY;
    private int gridWidth;
    private int gridHeight;
    private String cssId;
    private String fontSize;

    public Tile() {
    }

    public Tile(DetailField field) {
        gridX = field.getGridX();
        gridY = field.getGridY();
        gridWidth = field.getGridWidth();
        gridHeight = field.getGridHeight();
        cssId = field.getCssId();
        fontSize = field.getFontSize();
    }

    public int getGridX() {
        return gridX;
    }

    public void setGridX(int gridX) {
        this.gridX = gridX;
    }

    public int getGridY() {
        return gridY;
    }

    public void setGridY(int gridY) {
        this.gridY = gridY;
    }

    public int getGridWidth() {
        return gridWidth;
    }

    public void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
    }

    public int getGridHeight() {
        return gridHeight;
    }

    public void setGridHeight(int gridHeight) {
        this.gridHeight = gridHeight;
    }

    public String getCssId() {
        return cssId;
    }

    public void setCssId(String cssId) {
        this.cssId = cssId;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }

    @Override
    public String toString() {
        return "Tile [gridX=" + gridX + ", gridY=" + gridY + ", gridWidth=" + gridWidth
                + ", gridHeight=" + gridHeight +
                ", cssId=" + cssId + ", fontSize=" + fontSize + "]";
    }
}

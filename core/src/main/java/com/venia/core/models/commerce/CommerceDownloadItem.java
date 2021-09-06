package com.venia.core.models.commerce;

public interface CommerceDownloadItem {

    /**
     * Returns the name of the item.
     */
    String getName();

    /**
     * Returns the file size as formatted string.
     */
    String getSize();

    /**
     * Returns the absolute download link.
     */
    String getDownloadLink();

}

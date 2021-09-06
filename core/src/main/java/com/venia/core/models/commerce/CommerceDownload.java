package com.venia.core.models.commerce;

import com.adobe.cq.wcm.core.components.models.Component;

import java.util.List;

public interface CommerceDownload extends Component {

    /**
     * Returns a list of download items.
     */
    List<CommerceDownloadItem> getDownloads();

}

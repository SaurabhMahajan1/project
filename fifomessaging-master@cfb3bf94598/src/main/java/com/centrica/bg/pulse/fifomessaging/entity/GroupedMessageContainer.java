package com.centrica.bg.pulse.fifomessaging.entity;

import com.centrica.bg.pulse.fifomessaging.http.rest.MapValueComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by !basotim1 on 27/08/2016.
 */
public class GroupedMessageContainer {
    protected Log log = LogFactory.getLog(GroupedMessageContainer.class);;
    //TODO remove this
    //this is temporary to see response
    private Map<String,MessageWrapper> messsageStore_deleteit;

    String docId;
    // This has to be ordered set, as needs ordering can't be queue
    //private Set<Map<String, Object>> messages = new TreeSet<>(DATE_COMPARATOR);

    private Map<String,HashMap<String,Object>> messages;

    public GroupedMessageContainer(){
        messages = new ConcurrentHashMap<>();
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public void removeMessage(String key) {
        messages.remove(key);
    }

    /**
     * returns top element only
     * @return
     */
    public String getFirstMessageId() {
        Comparator<String> mapDateValueComparator = new MapValueComparator<String,HashMap<String,Object>>(messages);
        TreeMap<String, HashMap<String,Object>> tempMap = new TreeMap<String, HashMap<String,Object>>(mapDateValueComparator);
        tempMap.putAll(messages);
        return tempMap.firstEntry().getKey();
    }

    public void addMessage(String key,HashMap<String,Object> value){
        messages.put(key, value);
    }


    public Map<String, MessageWrapper> getMesssageStore_deleteit() {
        return messsageStore_deleteit;
    }

    public void setMesssageStore_deleteit(Map<String, MessageWrapper> messsageStore_deleteit) {
        this.messsageStore_deleteit = messsageStore_deleteit;
    }

    public boolean isContainerMessageEmpty(){
        return messages.isEmpty();
    }
}

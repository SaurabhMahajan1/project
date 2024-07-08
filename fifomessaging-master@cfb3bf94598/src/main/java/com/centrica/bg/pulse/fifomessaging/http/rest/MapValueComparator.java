package com.centrica.bg.pulse.fifomessaging.http.rest;

import com.centrica.bg.pulse.fifomessaging.constant.Constants;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by !basotim1 on 29/08/2016.
 */
public class MapValueComparator<K, V extends Map<String, Object>> implements Comparator<K> {

    Map<K, V> map = new HashMap<K, V>();

        public MapValueComparator(Map<K, V> map){
            this.map.putAll(map);
        }

        @Override
        public int compare(K s1, K s2) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormat.
                    forPattern(Constants.ISO_8601_DATE_PATTERN);
            DateTime timestamp1 = new DateTime(dateTimeFormatter.parseDateTime(map.get(s1).get(Constants.CREATE_TIMESTAMP).toString()), DateTimeZone.UTC);
            DateTime timestamp2 = new DateTime(dateTimeFormatter.parseDateTime(map.get(s2).get(Constants.CREATE_TIMESTAMP).toString()), DateTimeZone.UTC);
            return timestamp1.compareTo(timestamp2);
           //return -map.get(s1).compareTo(map.get(s2));//descending order
        }
 }

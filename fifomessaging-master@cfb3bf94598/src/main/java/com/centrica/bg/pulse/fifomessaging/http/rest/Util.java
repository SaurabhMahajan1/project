package com.centrica.bg.pulse.fifomessaging.http.rest;

import com.centrica.bg.pulse.fifomessaging.constant.Constants;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Created by !basotim1 on 26/08/2016.
 */
public class Util {
    private static final String CREATE_TIMESTAMP = "createTimestamp";
    //Comparator<SortedMap.Entry<String,Map<String, Object>>> mapComparatorByValue = (entry1, entry2) -> compareTime(entry1.getValue(),entry2.getValue());


    public static String generateISO8601(DateTime dateTime) {
        return  dateTime.toString(Constants.ISO_8601_DATE_PATTERN);

    }
    public static String getDateInString(DateTime dateTime,String format) {
        return  dateTime.toString(format);

       // toString("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    public static void main(String[] args) {
        String dateStr = "2016-10-11T17:49:27:435+0100";
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.
                forPattern(Constants.ISO_8601_DATE_PATTERN);
        System.out.println(dateTimeFormatter.parseDateTime(dateStr));
    }


}

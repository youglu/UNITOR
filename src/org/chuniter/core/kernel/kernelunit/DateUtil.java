package org.chuniter.core.kernel.kernelunit;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;


public class DateUtil
  implements Serializable
{
  private static final long serialVersionUID = -2267475972296676373L;

  public static Date shortDate(Date date)
  {
    SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd");
    String d = sf.format(date);
    Date dt = null;
    try {
      dt = sf.parse(d);
    }
    catch (ParseException e) {
      e.printStackTrace();
    }
    return dt;
  }

  public static String dateToString(Date date, String format)
  {
    if (date == null) return null;

    String d = null;
    SimpleDateFormat sf = null;
    try {
      if (!(StringUtil.isValid(format))) {
        format = "yyyy-MM-dd";
      }
      //先固定中文吧，后面需根据设置的语言来设置。
      sf = new SimpleDateFormat(format,Locale.CHINESE);
      d = sf.format(date);
    }
    catch (Exception e) {
      sf = new SimpleDateFormat("yyyy-MM-dd");
      d = sf.format(date);
    }
    return d;
  }

  public static String dateToString(Date date) {
    if (date == null) return null;

    return dateToString(date, "yyyy-MM-dd");
  }

  public static Date getTime(Date date)
  {
    SimpleDateFormat sf = new SimpleDateFormat("HH:mm:mm");
    String d = sf.format(date);
    Date dt = null;
    try {
      dt = sf.parse(d);
    }
    catch (ParseException e) {
      e.printStackTrace();
    }
    return dt;
  }

  public static Date stringToDate(String dateString)
  {
    Date date = null;
    if ((dateString != null) && (!("".equals(dateString.trim())))) {
      SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd");
      dateString = dateString.replaceAll("/", "-");
      try {
        date = s.parse(dateString);
      }
      catch (ParseException e) {
        SimpleDateFormat ss = new SimpleDateFormat("yyyyMMdd");
        try {
          return ss.parse(dateString);
        }
        catch (ParseException e1) {
          e1.printStackTrace();
        }
      }
    }
    return date;
  }

  public static Date stringToDate(String dateString, String format) {
    SimpleDateFormat sf = null;
    try {
      if (!(StringUtil.isValid(format))) {
        format = "yyyy-MM-dd";
      }
      sf = new SimpleDateFormat(format);
    }
    catch (Exception e) {
      sf = new SimpleDateFormat("yyyy-MM-dd");
    }
    Date date = null;
    if ((dateString != null) && (!("".equals(dateString.trim())))) {
      dateString = dateString.replaceAll("/", "-");
      try {
        date = sf.parse(dateString); } catch (ParseException localParseException) {
      }
    }
    return date;
  }

  public static int calculateMonth(Date startDate, Date endDate)
  {
    int result = 0;
    if ((startDate != null) && (endDate != null) && 
      (startDate.before(endDate))) {
      Calendar c = Calendar.getInstance();
      c.setTime(startDate);
      int startMonth = c.get(2);
      int startYear = c.get(1);

      c.clear();
      c.setTime(endDate);
      int endMonth = c.get(2);
      int endYear = c.get(1);

      result = endMonth - startMonth + 12 * (endYear - startYear);
    }
    return result;
  }

  public static Map<String, String> fetchStartMap(Calendar c, Map<String, String> map)
  {
    int month = c.get(2);
    for (int i = 0; i <= month; ++i) {
      String key = null; String value = null;
      c.set(2, i);
      c.set(5, 1);
      key = dateToString(c.getTime(), "yyyyMM");
      value = dateToString(c.getTime());
      map.put(key, value);
    }
    return map;
  }

  public static Map<String, String> fetchMaxMap(Calendar c, Map<String, String> map)
  {
    int month = c.get(2);
    for (int i = 0; i <= month; ++i) {
      String key = null; String value = null;

      c.set(2, i);
      c.set(5, 1);
      key = dateToString(c.getTime(), "yyyyMM");
      c.add(2, 1);
      c.add(5, -1);
      value = dateToString(c.getTime());
      map.put(key, value);

      c.add(2, -1);
      c.add(5, 1);
    }
    return map;
  }

  public static void fillMap(Map<String, String> start, Map<String, String> end)
  {
    Calendar c = Calendar.getInstance();
    int currentMonth = c.get(2);
    int currentYear = c.get(1);
    for (int i = 2004; i <= currentYear; ++i) {
      c.set(1, i);
      if (i < currentYear)
        c.set(2, 11);
      else {
        c.set(2, currentMonth);
      }
      fetchStartMap(c, start);
      fetchMaxMap(c, end);
    }
  }

  public static void main(String[] args)
  {
  }
}
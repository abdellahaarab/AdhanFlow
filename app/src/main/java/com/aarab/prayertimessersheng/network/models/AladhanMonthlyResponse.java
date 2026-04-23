package com.aarab.prayertimessersheng.network.models;

import java.util.List;

/** Root response from GET /v1/calendarByCity/{year}/{month} */
public class AladhanMonthlyResponse {
    public int code;
    public String status;
    public List<DayData> data;
}

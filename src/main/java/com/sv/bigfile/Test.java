package com.sv.bigfile;

public class Test {
    public static void main(String[] args) {
        String r = "Cancelled";
        String s = "String statusStr = isCancelled() ? \"Read cancelled - \" : \"Read complete - \";";
        System.out.println(s.contains(r));
        System.out.println(s.toLowerCase().contains(r.toLowerCase()));
        String[] a = s.split(r);
        System.out.println(a.length);
        for (String z : a) {
            System.out.println("z = " + z);
        }
     }
}

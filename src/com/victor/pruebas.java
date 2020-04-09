package com.victor;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class pruebas {

    public static void main(String[] args) {
	Pattern regex = Pattern.compile("([\\d.]+).*",Pattern.MULTILINE);
	Matcher m = regex.matcher("1.7 db\n");
	if (m.find()) {
	    System.out.println(new BigDecimal(m.group(1)));
	}
    }

}

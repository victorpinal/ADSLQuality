package com.victor;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;

public class PreferencesManager {
    
    private static Preferences preferences = Preferences.userNodeForPackage(ADSLQuality.class);
    
    static String getPreference(String message, String name, String def_value) {
	String value = preferences.get(name, null);
	if (value == null) {
	    value = JOptionPane.showInputDialog(null, message, "Configuracion", JOptionPane.QUESTION_MESSAGE, null, null, def_value).toString();
	    preferences.put(name, value);
	}
	return value;
    }
    
    static void clean(String name) {
	preferences.remove(name);
    }
    
    static void cleanAll() {
	try {
	    preferences.clear();
	} catch (BackingStoreException e) {
	    e.printStackTrace();
	}
    }
    
 }

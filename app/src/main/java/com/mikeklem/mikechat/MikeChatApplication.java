package com.mikeklem.mikechat;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by mikek on 2014-09-15.
 */
public class MikeChatApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        Parse.initialize(this, "NGocTsULSIDSb6R0hgqcMZA8FY3uGCDnPjvHbRxR", "EzdocacbhOaZNnMhn9Wfqn4IUycd7hKUy9Vvjojz");
    }
}

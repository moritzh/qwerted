/*
qwerted - virtual keyboard for android
Copyright (c) 2010 Moritz Haarmann. All Rights Reserved.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version
3 of the License, or (at your option) any later version.
 */
package com.qwerted;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.TextView;
import android.widget.Toast;

/**
 * the former name of the project was itype, and i was lazy. so
 * ITypePreferences.
 * 
 * @author moritzhaarmann
 * 
 */
public class ITypePreferences extends PreferenceActivity {
    Handler mHandler;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new PreferenceNotificationHandler(this);
        addPreferencesFromResource(R.xml.preferences);
        final Preference foo = this.getPreferenceScreen().getPreference(2);
        if (foo != null) {
            foo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                public boolean onPreferenceClick(final Preference preference) {
                    launch();
                    return false;
                }

            });
        }
    }

    private void launch() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setTitle("Please Wait");
        dialog.setMessage("Fetching Dictionary List...");
        dialog.show();
        final HttpRequest req = new HttpGet(
                "http://www.qwerted.com/dictionaries/index.dict");
        final HttpClient c = new DefaultHttpClient();
        try {
            final HttpResponse p = c.execute(new HttpHost("www.qwerted.com"),
                    req, (HttpContext) null);
            final HttpEntity message = p.getEntity();
            dialog.dismiss();

            if (message != null) {
                final byte[] buf = new byte[(int) message.getContentLength()];
                message.getContent().read(buf);
                final String text = new String(buf);
                final String[] availables = text.split("\n");
                final String[] dicts = new String[availables.length];
                for (int i = 0; i < dicts.length; i++) {
                    dicts[i] = availables[i].split(":")[0];
                }

                final AlertDialog.Builder builder = new AlertDialog.Builder(
                        this);
                builder.setTitle(R.string.dict_choose);
                builder.setItems(dicts, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,
                            final int item) {
                        new DictionaryDownloader(ITypePreferences.this,
                                availables[item]).start();
                        final Toast t = Toast
                                .makeText(
                                        getBaseContext(),
                                        "Your dictionary is being downloaded. This might take some time, you'll be notified when it's ready to use.",
                                        5000);
                        t.setDuration(10000);
                        t.show();
                    }
                });
                final AlertDialog alert = builder.create();

                alert.show();
            }
        } catch (final Exception e) {
            dialog.dismiss();
        }
    }

    /**
     * some utility classes.
     * 
     * @author moritzhaarmann
     * 
     */
    class DictionaryDownloader extends Thread {
        String name;
        Activity a;

        DictionaryDownloader(final Activity a, final String name) {
            this.a = a;
            this.name = name.split(":")[1];
        }

        @Override
        public void run() {

            final HttpRequest req = new HttpGet(
                    "http://www.qwerted.com/dictionaries/" + name + ".dict");
            final HttpClient c = new DefaultHttpClient();
            try {
                final HttpResponse p = c.execute(
                        new HttpHost("www.qwerted.com"), req,
                        (HttpContext) null);
                final HttpEntity message = p.getEntity();
                if (p.getStatusLine().getStatusCode() != 200) {
                    throw new Exception("FOOOO");
                }
                if (message != null) {
                    final int BUFFER_SIZE = 4096;
                    final byte[] buffer = new byte[BUFFER_SIZE];
                    final InputStream is = message.getContent();
                    final FileOutputStream f = new FileOutputStream(new File(a
                            .getBaseContext().getFilesDir().getAbsolutePath()
                            + "/" + this.name + ".dict"));
                    // read and write.

                    int read;
                    while ((read = is.read(buffer)) > -1) {
                        f.write(buffer, 0, read);
                    }
                    f.close();
                    is.close();
                    Message.obtain(ITypePreferences.this.mHandler, 0, this.name)
                            .sendToTarget();
                }
            } catch (final ClientProtocolException e) {
                // TODO Auto-generated catch block
                Message.obtain(ITypePreferences.this.mHandler, 1)
                        .sendToTarget();
                e.printStackTrace();
            } catch (final IOException e) {
                Message.obtain(ITypePreferences.this.mHandler, 1)
                        .sendToTarget(); // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (final Exception e) {
                Message.obtain(ITypePreferences.this.mHandler, 1)
                        .sendToTarget(); // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    class PreferenceNotificationHandler extends Handler {
        Activity a;

        PreferenceNotificationHandler(final Activity a) {
            this.a = a;
        }

        @Override
        public void handleMessage(final Message m) {
            if (m.what == 0) {

                final NotificationManager foo = (NotificationManager) a
                        .getBaseContext().getSystemService("notification");
                final Notification n = new Notification(
                        android.R.drawable.stat_sys_download_done,
                        "dictionary has been downloaded!",
                        System.currentTimeMillis());
                final TextView tv = new TextView(this.a);
                tv.setText("the qwerted dictionary is done downloading, go use it!");
                n.setLatestEventInfo(a, "qwerted", a.getBaseContext()
                        .getResources().getString(R.string.dict_success),
                        PendingIntent.getActivity(a.getBaseContext(), 0,
                                new Intent(Intent.ACTION_SEND), 0));
                foo.notify(0, n);
                final SharedPreferences p = PreferenceManager
                        .getDefaultSharedPreferences(getBaseContext());
                final Editor pe = p.edit();
                pe.putString("dict.dict", m.obj.toString());
                pe.commit();
            } else {
                final Toast t = Toast
                        .makeText(
                                a.getBaseContext(),
                                "There has been an error downloading the dictionary, please try again. Or give up.",
                                10000);
                t.show();
            }
        }
    }
}
